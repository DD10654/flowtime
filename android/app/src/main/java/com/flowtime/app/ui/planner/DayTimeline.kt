package com.flowtime.app.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.flowtime.app.data.remote.EventDto
import com.flowtime.app.domain.EventStates
import com.flowtime.app.domain.clockLabel
import com.flowtime.app.domain.durationMinutes
import com.flowtime.app.domain.hexColor
import com.flowtime.app.domain.local
import com.flowtime.app.domain.minutesToLabel
import com.flowtime.app.domain.parseInstant
import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

private const val HOUR_HEIGHT_DP = 56
private const val GUTTER_DP = 48

private data class Positioned(
    val event: EventDto,
    val topMin: Int,
    val durMin: Int,
    val lane: Int,
    val lanes: Int,
)

private fun layoutDay(events: List<EventDto>, day: LocalDate): List<Positioned> {
    data class Ev(val e: EventDto, val start: Int, val end: Int)
    val evs = events.mapNotNull { e ->
        val s = parseInstant(e.start).local()
        if (s.date != day) return@mapNotNull null
        val startMin = s.hour * 60 + s.minute
        val dur = durationMinutes(e.start, e.end).coerceAtLeast(15)
        Ev(e, startMin, (startMin + dur).coerceAtMost(24 * 60))
    }.sortedBy { it.start }

    val out = mutableListOf<Positioned>()
    var i = 0
    while (i < evs.size) {
        var clusterEnd = evs[i].end
        val cluster = mutableListOf(evs[i])
        var j = i + 1
        while (j < evs.size && evs[j].start < clusterEnd) {
            cluster.add(evs[j]); clusterEnd = maxOf(clusterEnd, evs[j].end); j++
        }
        val laneEnds = mutableListOf<Int>()
        val laneOf = IntArray(cluster.size)
        cluster.forEachIndexed { idx, ev ->
            var lane = laneEnds.indexOfFirst { it <= ev.start }
            if (lane == -1) { lane = laneEnds.size; laneEnds.add(ev.end) } else laneEnds[lane] = ev.end
            laneOf[idx] = lane
        }
        val lanes = laneEnds.size
        cluster.forEachIndexed { idx, ev ->
            out.add(Positioned(ev.e, ev.start, ev.end - ev.start, laneOf[idx], lanes))
        }
        i = j
    }
    return out
}

@Composable
fun DayTimeline(
    day: LocalDate,
    events: List<EventDto>,
    scrollToMin: Int,
    onEventClick: (EventDto) -> Unit,
    onEventMoved: (EventDto, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val hourPx = with(density) { HOUR_HEIGHT_DP.dp.toPx() }
    val positioned = remember(events, day) { layoutDay(events, day) }
    val scroll = rememberScrollState()

    LaunchedEffect(day) {
        scroll.scrollTo(((scrollToMin - 60).coerceAtLeast(0) / 60f * hourPx).roundToInt())
    }

    Row(modifier.verticalScroll(scroll)) {
        // Hour gutter
        Box(Modifier.width(GUTTER_DP.dp).height((24 * HOUR_HEIGHT_DP).dp)) {
            for (h in 0..23) {
                Text(
                    minutesToLabel(h * 60),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .offset { IntOffset(0, (h * hourPx).roundToInt()) }
                        .padding(end = 4.dp, top = 2.dp),
                )
            }
        }

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height((24 * HOUR_HEIGHT_DP).dp),
        ) {
            val areaWidthPx = with(density) { maxWidth.toPx() }
            val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

            // Hour grid lines
            for (h in 0..24) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .offset { IntOffset(0, (h * hourPx).roundToInt()) }
                        .background(gridColor),
                )
            }

            positioned.forEach { p ->
                val laneWidthPx = areaWidthPx / p.lanes
                val draggable = p.event.flexible || p.event.sourceSeriesId != null
                EventBlock(
                    positioned = p,
                    hourPx = hourPx,
                    xPx = (p.lane * laneWidthPx),
                    widthPx = laneWidthPx,
                    draggable = draggable,
                    onClick = { onEventClick(p.event) },
                    onMovedToMin = { newMin -> onEventMoved(p.event, newMin) },
                )
            }
        }
    }
}

@Composable
private fun EventBlock(
    positioned: Positioned,
    hourPx: Float,
    xPx: Float,
    widthPx: Float,
    draggable: Boolean,
    onClick: () -> Unit,
    onMovedToMin: (Int) -> Unit,
) {
    val density = LocalDensity.current
    var dragY by remember(positioned.event.id, positioned.topMin) { mutableFloatStateOf(0f) }
    val base = hexColor(positioned.event.color)
    val free = positioned.event.state == EventStates.FREE
    val heightPx = (positioned.durMin / 60f * hourPx)
    val widthDp = with(density) { (widthPx - 4).coerceAtLeast(0f).toDp() }
    val heightDp = with(density) { (heightPx - 2).coerceAtLeast(14f).toDp() }

    Box(
        Modifier
            .offset {
                IntOffset(
                    xPx.roundToInt() + 2,
                    (positioned.topMin / 60f * hourPx + dragY).roundToInt(),
                )
            }
            .width(widthDp)
            .height(heightDp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (free) base.copy(alpha = 0.16f) else base)
            .then(
                if (free) Modifier.border(1.dp, base, RoundedCornerShape(6.dp)) else Modifier,
            )
            .pointerInput(positioned.event.id, draggable) {
                if (draggable) {
                    detectDragGesturesAfterLongPress(
                        onDrag = { change, delta -> change.consume(); dragY += delta.y },
                        onDragEnd = {
                            val deltaMin = (dragY / hourPx * 60f).roundToInt()
                            val target = positioned.topMin + deltaMin
                            val snapped = ((target / 15f).roundToInt() * 15)
                                .coerceIn(0, 24 * 60 - positioned.durMin)
                            dragY = 0f
                            if (snapped != positioned.topMin) onMovedToMin(snapped)
                        },
                        onDragCancel = { dragY = 0f },
                    )
                }
            }
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        androidx.compose.foundation.layout.Column {
            Text(
                positioned.event.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (free) base else Color.White,
                maxLines = 1,
            )
            if (positioned.durMin >= 45) {
                Text(
                    "${clockLabel(parseInstant(positioned.event.start).local())}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (free) base else Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                )
            }
        }
    }
}
