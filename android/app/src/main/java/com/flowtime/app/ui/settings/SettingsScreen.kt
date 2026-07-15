package com.flowtime.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowtime.app.data.remote.RecurringBody
import com.flowtime.app.data.remote.RecurringEventDto
import com.flowtime.app.data.remote.SettingsBody
import com.flowtime.app.data.remote.TimeOffBody
import com.flowtime.app.data.remote.TimeOffDto
import com.flowtime.app.domain.daysLabel
import com.flowtime.app.domain.hexColor
import com.flowtime.app.domain.minutesToLabel
import com.flowtime.app.domain.timeOffRangeLabel
import com.flowtime.app.domain.toUtcMidnightMillis
import com.flowtime.app.domain.utcMillisToLocalDate
import com.flowtime.app.domain.ymd
import com.flowtime.app.ui.common.ColorSwatchRow
import com.flowtime.app.ui.common.DateField
import com.flowtime.app.ui.common.FieldLabel
import com.flowtime.app.ui.common.FlowTextField
import com.flowtime.app.ui.common.NumberField
import com.flowtime.app.ui.common.SaveButton
import com.flowtime.app.ui.common.StateContent
import com.flowtime.app.ui.common.TimeField
import com.flowtime.app.ui.common.WeekdayPicker
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val RECUR_COLORS = listOf("#0e7490", "#475569", "#b45309", "#7c3aed", "#be123c", "#15803d")

@Composable
fun SettingsScreen() {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.consumeMessage() } }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        Column(Modifier.padding(pad)) {
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Settings", style = MaterialTheme.typography.titleLarge)
                    Text("Tune the hours and rules the scheduler plans around.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (busy) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            }
            StateContent(state, onRetry = { vm.refresh() }) { bundle ->
                SettingsContent(bundle, busy, snackbar, vm)
            }
        }
    }
}

@Composable
private fun SettingsContent(
    bundle: SettingsBundle,
    busy: Boolean,
    snackbar: SnackbarHostState,
    vm: SettingsViewModel,
) {
    val s = bundle.settings
    val scope = rememberCoroutineScope()

    var startMin by remember(s) { mutableIntStateOf(s.workdayStartMin) }
    var endMin by remember(s) { mutableIntStateOf(s.workdayEndMin) }
    var days by remember(s) { mutableStateOf(parseDays(s.workdays)) }
    var buffer by remember(s) { mutableIntStateOf(s.defaultBufferMin) }
    var taskBufferThreshold by remember(s) { mutableIntStateOf(s.minTaskDurationForBuffer) }
    var chunkGap by remember(s) { mutableIntStateOf(s.minGapBetweenTaskChunks) }
    var focusTarget by remember(s) { mutableIntStateOf(s.weeklyFocusTargetHours) }
    var lockHorizon by remember(s) { mutableIntStateOf(s.lockHorizonHours) }
    var planHorizon by remember(s) { mutableIntStateOf(s.planHorizonDays) }

    var showRecurring by remember { mutableStateOf(false) }
    var showTimeOff by remember { mutableStateOf(false) }

    val dirty = startMin != s.workdayStartMin || endMin != s.workdayEndMin ||
        days.sorted().joinToString(",") != normalize(s.workdays) ||
        buffer != s.defaultBufferMin || taskBufferThreshold != s.minTaskDurationForBuffer ||
        chunkGap != s.minGapBetweenTaskChunks || focusTarget != s.weeklyFocusTargetHours ||
        lockHorizon != s.lockHorizonHours || planHorizon != s.planHorizonDays

    fun save() {
        when {
            days.isEmpty() -> scope.launch { snackbar.showSnackbar("Pick at least one working day.") }
            endMin <= startMin -> scope.launch { snackbar.showSnackbar("Workday end must be after the start.") }
            else -> vm.saveSettings(
                SettingsBody(
                    workdayStartMin = startMin,
                    workdayEndMin = endMin,
                    workdays = days.sorted().joinToString(","),
                    defaultBufferMin = buffer,
                    weeklyFocusTargetHours = focusTarget,
                    lockHorizonHours = lockHorizon,
                    planHorizonDays = planHorizon,
                    minTaskDurationForBuffer = taskBufferThreshold,
                    minGapBetweenTaskChunks = chunkGap,
                ),
            ) {}
        }
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SettingCard("Working hours", "The daily window the AI schedules tasks and habits inside.") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeField("Day starts", startMin, { startMin = it }, Modifier.weight(1f))
                TimeField("Day ends", endMin, { endMin = it }, Modifier.weight(1f))
            }
        }
        SettingCard("Working days", "Days treated as workdays. Others stay free.") {
            WeekdayPicker(days, { iso -> days = if (days.contains(iso)) days - iso else days + iso })
        }
        SettingCard("Default buffer", "Padding around meetings so you're never back-to-back.") {
            SuffixNumber(buffer, { buffer = it }, "minutes")
        }
        SettingCard("Buffer after long tasks", "Task blocks at least this long get a trailing buffer.") {
            SuffixNumber(taskBufferThreshold, { taskBufferThreshold = it }, "minutes or longer")
        }
        SettingCard("Gap between task chunks", "Minimum spacing when a task is split across blocks (0 allows back-to-back).") {
            SuffixNumber(chunkGap, { chunkGap = it }, "minutes")
        }
        SettingCard("Weekly focus target", "How many hours of focus time the AI aims to protect each week.") {
            SuffixNumber(focusTarget, { focusTarget = it }, "hours / week")
        }
        SettingCard("Lock horizon", "Flexible blocks within this window are locked so they stop moving.") {
            SuffixNumber(lockHorizon, { lockHorizon = it }, "hours ahead")
        }
        SettingCard("Planning horizon", "How many days ahead the AI plans each run.") {
            SuffixNumber(planHorizon, { planHorizon = it }, "days")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (dirty) {
                OutlinedButton(onClick = {
                    startMin = s.workdayStartMin; endMin = s.workdayEndMin; days = parseDays(s.workdays)
                    buffer = s.defaultBufferMin; taskBufferThreshold = s.minTaskDurationForBuffer
                    chunkGap = s.minGapBetweenTaskChunks; focusTarget = s.weeklyFocusTargetHours
                    lockHorizon = s.lockHorizonHours; planHorizon = s.planHorizonDays
                }, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) { Text("Discard") }
            }
            Box(Modifier.weight(1f)) {
                SaveButton(if (dirty) "Save changes" else "Saved", enabled = dirty, busy = busy) { save() }
            }
        }

        // Recurring commitments
        SettingCard("Recurring commitments", "Fixed events (e.g. School) the AI never moves and plans around.") {
            bundle.recurring.forEach { r ->
                RecurringRow(r, onToggle = { vm.setRecurringActive(r.id, !r.active) }, onDelete = { vm.deleteRecurring(r.id) })
                Spacer(Modifier.height(6.dp))
            }
            OutlinedButton(onClick = { showRecurring = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Add recurring event")
            }
        }

        // Time off
        SettingCard("Time off", "While away, the AI skips focus, habits, and tasks (fixed events stay).") {
            bundle.timeOff.forEach { t ->
                TimeOffRow(t, onDelete = { vm.deleteTimeOff(t.id) })
                Spacer(Modifier.height(6.dp))
            }
            OutlinedButton(onClick = { showTimeOff = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Add time off")
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showRecurring) {
        RecurringFormSheet(busy = busy, onDismiss = { showRecurring = false }, onSave = { body ->
            vm.addRecurring(body) { showRecurring = false }
        })
    }
    if (showTimeOff) {
        TimeOffFormSheet(busy = busy, onDismiss = { showTimeOff = false }, onSave = { body ->
            vm.addTimeOff(body) { showTimeOff = false }
        })
    }
}

@Composable
private fun SettingCard(title: String, desc: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)).padding(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 10.dp, top = 2.dp))
        content()
    }
}

@Composable
private fun SuffixNumber(value: Int, onChange: (Int) -> Unit, suffix: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NumberField("", value, onChange, Modifier.width(120.dp))
        Text(suffix, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RecurringRow(r: RecurringEventDto, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(hexColor(r.color)))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                if (!r.active) {
                    Spacer(Modifier.width(6.dp))
                    Text("Paused", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("${daysLabel(r.days)} · ${minutesToLabel(r.startMin)}–${minutesToLabel(r.endMin)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onToggle) {
            Icon(if (r.active) Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TimeOffRow(t: TimeOffDto, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(timeOffRangeLabel(t.start, t.end), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (!t.label.isNullOrBlank()) {
                Text(t.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringFormSheet(busy: Boolean, onDismiss: () -> Unit, onSave: (RecurringBody) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    var startMin by remember { mutableIntStateOf(480) }
    var endMin by remember { mutableIntStateOf(900) }
    var days by remember { mutableStateOf(setOf(1, 2, 3, 4, 5)) }
    var color by remember { mutableStateOf(RECUR_COLORS.first()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("New recurring event", style = MaterialTheme.typography.titleMedium)
            FlowTextField("Title", title, { title = it }, placeholder = "e.g. School")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeField("From", startMin, { startMin = it }, Modifier.weight(1f))
                TimeField("To", endMin, { endMin = it }, Modifier.weight(1f))
            }
            Column { FieldLabel("Repeats on"); WeekdayPicker(days, { iso -> days = if (days.contains(iso)) days - iso else days + iso }) }
            Column { FieldLabel("Color"); ColorSwatchRow(RECUR_COLORS, color, { color = it }) }
            SaveButton("Add recurring event", enabled = title.isNotBlank() && days.isNotEmpty() && endMin > startMin, busy = busy) {
                onSave(RecurringBody(title.trim(), startMin, endMin, days.sorted().joinToString(","), color))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeOffFormSheet(busy: Boolean, onDismiss: () -> Unit, onSave: (TimeOffBody) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    var startMillis by remember { mutableStateOf<Long?>(today.toUtcMidnightMillis()) }
    var endMillis by remember { mutableStateOf<Long?>(today.toUtcMidnightMillis()) }
    var label by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Add time off", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField("From", startMillis, { startMillis = it; if (it != null && (endMillis == null || endMillis!! < it)) endMillis = it }, Modifier.weight(1f), clearable = false)
                DateField("To", endMillis, { endMillis = it }, Modifier.weight(1f), clearable = false)
            }
            FlowTextField("Label (optional)", label, { label = it }, placeholder = "e.g. Vacation")
            SaveButton("Add time off", enabled = startMillis != null && endMillis != null, busy = busy) {
                val from = ymd(utcMillisToLocalDate(startMillis!!))
                val to = ymd(utcMillisToLocalDate(endMillis!!))
                onSave(TimeOffBody(label = label.ifBlank { null }, start = from, end = to))
            }
        }
    }
}

private fun parseDays(csv: String): Set<Int> =
    csv.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..7 }.toSet()

private fun normalize(csv: String): String = parseDays(csv).sorted().joinToString(",")
