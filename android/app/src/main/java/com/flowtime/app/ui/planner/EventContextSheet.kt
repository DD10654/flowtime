package com.flowtime.app.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flowtime.app.data.remote.EventDto
import com.flowtime.app.domain.EventStates
import com.flowtime.app.domain.EventTypes
import com.flowtime.app.domain.TYPE_LABELS
import com.flowtime.app.domain.clockLabel
import com.flowtime.app.domain.durationMinutes
import com.flowtime.app.domain.hexColor
import com.flowtime.app.domain.local
import com.flowtime.app.domain.parseInstant
import com.flowtime.app.domain.prettyDuration
import com.flowtime.app.domain.shortDate

data class EventActions(
    val onLockToggle: (EventDto) -> Unit,
    val onToggleBuffer: (EventDto) -> Unit,
    val onReschedule: () -> Unit,
    val onDeleteEvent: (EventDto) -> Unit,
    val onDeleteTask: (String) -> Unit,
    val onDeleteHabit: (String) -> Unit,
    val onPauseHabit: (String) -> Unit,
    val onDeleteSeries: (String) -> Unit,
    val onPauseSeries: (String) -> Unit,
    val onDeleteOccurrence: (EventDto) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventContextSheet(
    event: EventDto,
    actions: EventActions,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(hexColor(event.color)))
                Spacer(Modifier.width(8.dp))
                Text(event.title, style = MaterialTheme.typography.titleMedium)
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Badge(TYPE_LABELS[event.type] ?: event.type)
                Badge(if (event.state == EventStates.FREE) "Free · flexible" else "Busy · defended")
                if (event.locked) Badge("Locked")
            }

            val start = parseInstant(event.start).local()
            InfoRow("Start", "${shortDate(start.date)}, ${clockLabel(start)}")
            InfoRow("Duration", prettyDuration(durationMinutes(event.start, event.end)))
            InfoRow("Type", if (event.flexible) "AI-scheduled (flexible)" else "Fixed event")

            Text(
                explainBlock(event),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
            )

            if (event.type == EventTypes.MEETING && !event.flexible) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Checkbox(checked = !event.noBuffer, onCheckedChange = { actions.onToggleBuffer(event) })
                    Column {
                        Text("Add buffer around this meeting", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Keeps other blocks from butting up against it.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (event.flexible && event.type != EventTypes.BUFFER) {
                OutlinedButton(onClick = { actions.onLockToggle(event) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                    Icon(if (event.locked) Icons.Outlined.LockOpen else Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (event.locked) "Unlock block" else "Lock block")
                }
            }

            Button(onClick = actions.onReschedule, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reschedule day")
            }

            // Source-aware removal
            when {
                event.type == EventTypes.TASK_BLOCK && event.sourceTaskId != null ->
                    DangerButton("Delete task") { actions.onDeleteTask(event.sourceTaskId) }

                event.type == EventTypes.HABIT_BLOCK && event.sourceHabitId != null -> {
                    OutlinedButton(onClick = { actions.onPauseHabit(event.sourceHabitId) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Outlined.PauseCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp)); Text("Pause habit")
                    }
                    DangerButton("Delete habit") { actions.onDeleteHabit(event.sourceHabitId) }
                }

                event.type == EventTypes.COMMITMENT && event.sourceSeriesId != null -> {
                    DangerButton("Delete this occurrence") { actions.onDeleteOccurrence(event) }
                    OutlinedButton(onClick = { actions.onPauseSeries(event.sourceSeriesId) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Outlined.PauseCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp)); Text("Pause series")
                    }
                    DangerButton("Delete series") { actions.onDeleteSeries(event.sourceSeriesId) }
                }

                else -> DangerButton(if (!event.flexible) "Delete event" else "Remove block") {
                    actions.onDeleteEvent(event)
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DangerButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        ),
    ) {
        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

private fun explainBlock(e: EventDto): String = when (e.type) {
    EventTypes.TASK_BLOCK -> "A scheduled work block for this task. The AI may move it on the next plan — lock it to pin it. Deleting just the block won't help (it's re-added); delete the task to remove it for good."
    EventTypes.HABIT_BLOCK -> "A scheduled occurrence of this habit. It will be re-added on the next plan. Pause the habit to stop scheduling it, or delete it entirely."
    EventTypes.FOCUS -> "Auto-generated focus time. It's re-added on the next plan unless you lock it, lower your weekly focus goal, or add time off."
    EventTypes.BUFFER -> "An automatic buffer around a meeting. It's recreated whenever the day is re-planned."
    EventTypes.COMMITMENT -> "One occurrence of a recurring commitment. Drag to move just this occurrence, or use the buttons below to remove today's or manage the whole series."
    else -> "A fixed event. The AI won't move it; deleting removes it permanently."
}
