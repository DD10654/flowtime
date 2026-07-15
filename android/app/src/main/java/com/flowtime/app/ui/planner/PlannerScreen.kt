package com.flowtime.app.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowtime.app.data.remote.EventDto
import com.flowtime.app.domain.durationMinutes
import com.flowtime.app.domain.isoAt
import com.flowtime.app.domain.local
import com.flowtime.app.domain.longDate
import com.flowtime.app.domain.parseInstant
import com.flowtime.app.ui.common.StateContent
import com.flowtime.app.ui.common.UiState
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.minutes

private val WD = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun PlannerScreen() {
    val vm: PlannerViewModel = viewModel(factory = PlannerViewModel.Factory)
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    var selectedDate by remember { mutableStateOf(today) }
    var selected by remember { mutableStateOf<EventDto?>(null) }
    var quick by remember { mutableStateOf("") }

    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.consumeMessage() } }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            // Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Planner", style = MaterialTheme.typography.titleLarge)
                    Text(longDate(selectedDate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { vm.replan() }, enabled = !busy) {
                    if (busy) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                    } else {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("Plan my day")
                }
            }

            // Quick add
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = quick,
                    onValueChange = { quick = it },
                    placeholder = { Text("Add a task — e.g. “Report by Friday 3h high”") },
                    leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { if (quick.isNotBlank()) vm.quickAdd(quick) { quick = "" } },
                    enabled = quick.isNotBlank() && !busy,
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Add")
                }
            }

            // Week strip
            val days = remember(today) { (0..13).map { today.plus(DatePeriod(days = it)) } }
            LazyRow(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(vertical = 6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(days, key = { it.toString() }) { d ->
                    DayChip(d, d == selectedDate, d == today) { selectedDate = d }
                }
            }

            // Timeline
            StateContent(state, onRetry = { vm.refresh() }) { data ->
                DayTimeline(
                    day = selectedDate,
                    events = data.events,
                    scrollToMin = data.workStartMin,
                    onEventClick = { selected = it },
                    onEventMoved = { ev, newStartMin ->
                        val startIso = isoAt(selectedDate, newStartMin)
                        val dur = durationMinutes(ev.start, ev.end)
                        val endIso = (parseInstant(startIso) + dur.minutes).toString()
                        vm.moveEvent(ev, startIso, endIso)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    selected?.let { ev ->
        EventContextSheet(
            event = ev,
            actions = EventActions(
                onLockToggle = { selected = null; vm.toggleLock(it) },
                onToggleBuffer = { selected = null; vm.toggleBuffer(it) },
                onReschedule = { selected = null; vm.replan() },
                onDeleteEvent = { selected = null; vm.deleteEvent(it.id) },
                onDeleteTask = { selected = null; vm.deleteTask(it) },
                onDeleteHabit = { selected = null; vm.deleteHabit(it) },
                onPauseHabit = { selected = null; vm.pauseHabit(it) },
                onDeleteSeries = { selected = null; vm.deleteSeries(it) },
                onPauseSeries = { selected = null; vm.pauseSeries(it) },
                onDeleteOccurrence = { selected = null; vm.deleteEvent(it.id) },
            ),
            onDismiss = { selected = null },
        )
    }
}

@Composable
private fun DayChip(date: LocalDate, selected: Boolean, isToday: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Column(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(
                1.dp,
                if (isToday && !selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(WD[date.dayOfWeek.isoDayNumber - 1], style = MaterialTheme.typography.labelSmall, color = fg)
        Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = fg)
    }
}
