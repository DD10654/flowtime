package com.flowtime.app.ui.habits

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowtime.app.data.remote.HabitBody
import com.flowtime.app.data.remote.HabitDto
import com.flowtime.app.domain.FREQUENCY_LABELS
import com.flowtime.app.domain.HabitFrequencies
import com.flowtime.app.domain.HabitKinds
import com.flowtime.app.domain.hexColor
import com.flowtime.app.domain.minutesToLabel
import com.flowtime.app.domain.prettyDuration
import com.flowtime.app.ui.common.EmptyBox
import com.flowtime.app.ui.common.FieldLabel
import com.flowtime.app.ui.common.ColorSwatchRow
import com.flowtime.app.ui.common.FlowTextField
import com.flowtime.app.ui.common.NumberField
import com.flowtime.app.ui.common.SaveButton
import com.flowtime.app.ui.common.ScreenHeader
import com.flowtime.app.ui.common.SegmentedSelector
import com.flowtime.app.ui.common.SheetHeader
import com.flowtime.app.ui.common.StateContent
import com.flowtime.app.ui.common.TimeField
import com.flowtime.app.ui.common.UiState

private val HABIT_COLORS = listOf("#10b981", "#f59e0b", "#0ea5e9", "#8b5cf6", "#ef4444", "#14b8a6")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen() {
    val vm: HabitsViewModel = viewModel(factory = HabitsViewModel.Factory)
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var editing by remember { mutableStateOf<HabitDto?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.consumeMessage() } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; showSheet = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New habit") },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            ScreenHeader(
                title = "Habits",
                subtitle = "Recurring routines the AI fits into your ideal window.",
                trailing = { if (busy) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp)) },
            )
            StateContent(state, onRetry = { vm.refresh() }) { habits ->
                if (habits.isEmpty()) {
                    EmptyBox("No habits yet", "Add routines like Lunch, Gym, or Standup.")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(habits, key = { it.id }) { h ->
                            HabitCard(
                                habit = h,
                                onToggle = { vm.toggleActive(h) },
                                onEdit = { editing = h; showSheet = true },
                                onDelete = { vm.deleteHabit(h.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showSheet = false }, sheetState = sheetState) {
            HabitFormContent(
                initial = editing,
                busy = busy,
                onSave = { body ->
                    if (editing == null) vm.createHabit(body) { showSheet = false }
                    else vm.updateHabit(editing!!.id, body) { showSheet = false }
                },
            )
        }
    }
}

@Composable
private fun HabitCard(
    habit: HabitDto,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (habit.active) 1f else 0.55f }
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(hexColor(habit.color)))
            Spacer(Modifier.width(8.dp))
            Text(habit.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                if (habit.kind == HabitKinds.TEAM) Icons.Outlined.Group else Icons.Outlined.Person,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${if (habit.kind == HabitKinds.TEAM) "Team" else "Solo"}  ·  ${prettyDuration(habit.durationMin)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(2.dp))
        val freq = (FREQUENCY_LABELS[habit.frequency] ?: habit.frequency) +
            if (habit.frequency == HabitFrequencies.N_PER_WEEK) " (${habit.perWeek}×)" else ""
        Text(freq, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "${minutesToLabel(habit.idealWindowStartMin)} – ${minutesToLabel(habit.idealWindowEndMin)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onToggle, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)) {
                Text(if (habit.active) "Pause" else "Resume")
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HabitFormContent(
    initial: HabitDto?,
    busy: Boolean,
    onSave: (HabitBody) -> Unit,
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var durationMin by remember { mutableIntStateOf(initial?.durationMin ?: 30) }
    var frequency by remember { mutableStateOf(initial?.frequency ?: HabitFrequencies.DAILY) }
    var perWeek by remember { mutableIntStateOf(initial?.perWeek ?: 3) }
    var startMin by remember { mutableIntStateOf(initial?.idealWindowStartMin ?: 540) }
    var endMin by remember { mutableIntStateOf(initial?.idealWindowEndMin ?: 1020) }
    var kind by remember { mutableStateOf(initial?.kind ?: HabitKinds.SOLO) }
    var color by remember { mutableStateOf(initial?.color ?: HABIT_COLORS.first()) }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp)
            .navigationBarsPadding()
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SheetHeader(if (initial == null) "New habit" else "Edit habit") { }

        FlowTextField("Title", title, { title = it }, placeholder = "e.g. Gym, Lunch, Standup")

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField("Duration (min)", durationMin, { durationMin = it }, Modifier.weight(1f))
            if (frequency == HabitFrequencies.N_PER_WEEK) {
                NumberField("Times / week", perWeek, { perWeek = it.coerceIn(1, 7) }, Modifier.weight(1f))
            } else {
                Spacer(Modifier.weight(1f))
            }
        }

        Column {
            FieldLabel("Frequency")
            SegmentedSelector(
                options = HabitFrequencies.ALL,
                selected = frequency,
                onSelect = { frequency = it },
                label = {
                    when (it) {
                        HabitFrequencies.DAILY -> "Daily"
                        HabitFrequencies.WEEKDAYS -> "Weekdays"
                        else -> "N/week"
                    }
                },
            )
        }

        Column {
            FieldLabel("Ideal time window")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeField("From", startMin, { startMin = it }, Modifier.weight(1f))
                TimeField("To", endMin, { endMin = it }, Modifier.weight(1f))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                FieldLabel("Type")
                SegmentedSelector(
                    options = HabitKinds.ALL,
                    selected = kind,
                    onSelect = { kind = it },
                    label = { if (it == HabitKinds.TEAM) "Team" else "Solo" },
                )
            }
        }

        Column {
            FieldLabel("Color")
            ColorSwatchRow(HABIT_COLORS, color, { color = it })
        }

        SaveButton(
            text = if (initial == null) "Add habit" else "Save changes",
            enabled = title.isNotBlank(),
            busy = busy,
            onClick = {
                onSave(
                    HabitBody(
                        title = title.trim(),
                        durationMin = durationMin,
                        frequency = frequency,
                        perWeek = perWeek,
                        idealWindowStartMin = startMin,
                        idealWindowEndMin = endMin,
                        kind = kind,
                        color = color,
                        active = initial?.active ?: true,
                    ),
                )
            },
        )
    }
}
