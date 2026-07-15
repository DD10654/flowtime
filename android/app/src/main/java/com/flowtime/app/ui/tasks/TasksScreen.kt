package com.flowtime.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowtime.app.data.remote.TaskBody
import com.flowtime.app.data.remote.TaskDto
import com.flowtime.app.domain.TaskStatuses
import com.flowtime.app.domain.dueIsoFromUtcMillis
import com.flowtime.app.domain.dueLabel
import com.flowtime.app.domain.dueUtcMillisFromIso
import com.flowtime.app.domain.prettyDuration
import com.flowtime.app.domain.priorityColor
import com.flowtime.app.ui.common.DateField
import com.flowtime.app.ui.common.FieldLabel
import com.flowtime.app.ui.common.FlowTextField
import com.flowtime.app.ui.common.NumberField
import com.flowtime.app.ui.common.SaveButton
import com.flowtime.app.ui.common.ScreenHeader
import com.flowtime.app.ui.common.SegmentedSelector
import com.flowtime.app.ui.common.SheetHeader
import com.flowtime.app.ui.common.StateContent
import kotlinx.coroutines.launch

private val PRIORITY_TEXT = mapOf(1 to "Urgent", 2 to "High", 3 to "Medium", 4 to "Low")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen() {
    val vm: TasksViewModel = viewModel(factory = TasksViewModel.Factory)
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var editing by remember { mutableStateOf<TaskDto?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }

    val allTasks = (state as? com.flowtime.app.ui.common.UiState.Success)?.data ?: emptyList()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; showSheet = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New task") },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            ScreenHeader(
                title = "Tasks",
                subtitle = "Give tasks a duration & deadline — the AI finds the time.",
                trailing = { if (busy) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp)) },
            )
            StateContent(state, onRetry = { vm.refresh() }) { tasks ->
                val active = tasks.filter { it.status != TaskStatuses.DONE }
                if (active.isEmpty()) {
                    com.flowtime.app.ui.common.EmptyBox(
                        "No active tasks",
                        "Add one to get it auto-scheduled.",
                    )
                } else {
                    LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(active, key = { it.id }) { t ->
                            TaskRow(
                                task = t,
                                onComplete = { vm.completeTask(t.id) },
                                onEdit = { editing = t; showSheet = true },
                                onDelete = { vm.deleteTask(t.id) },
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
            TaskFormContent(
                initial = editing,
                allTasks = allTasks,
                busy = busy,
                onSuggest = { title -> vm.suggest(title) },
                onSave = { body ->
                    if (editing == null) vm.createTask(body) { showSheet = false }
                    else vm.updateTask(editing!!.id, body) { showSheet = false }
                },
            )
        }
    }
}

@Composable
private fun TaskRow(
    task: TaskDto,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onComplete) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.secondary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = "Complete",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Box(
            Modifier
                .padding(horizontal = 8.dp)
                .width(4.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(priorityColor(task.priority)),
        )
        Column(Modifier.weight(1f)) {
            Text(task.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            val meta = buildString {
                append(PRIORITY_TEXT[task.priority] ?: "P${task.priority}")
                append("  ·  ").append(prettyDuration(task.durationMin))
                append("  ·  ").append(dueLabel(task.due))
                if (task.dependsOnIds.isNotEmpty()) append("  ·  after ${task.dependsOnIds.size}")
                if (task.status == TaskStatuses.SCHEDULED) append("  ·  scheduled")
            }
            Text(
                meta,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TaskFormContent(
    initial: TaskDto?,
    allTasks: List<TaskDto>,
    busy: Boolean,
    onSuggest: suspend (String) -> com.flowtime.app.data.remote.PrioritizeResult,
    onSave: (TaskBody) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var durationMin by remember { mutableIntStateOf(initial?.durationMin ?: 60) }
    var priority by remember { mutableIntStateOf(initial?.priority ?: 3) }
    var dueMillis by remember { mutableStateOf(dueUtcMillisFromIso(initial?.due)) }
    var minChunk by remember { mutableIntStateOf(initial?.minChunkMin ?: 30) }
    var maxChunk by remember { mutableIntStateOf(initial?.maxChunkMin ?: 120) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var deps by remember { mutableStateOf(initial?.dependsOnIds?.toSet() ?: emptySet()) }
    var suggesting by remember { mutableStateOf(false) }

    val candidates = allTasks.filter { it.id != initial?.id && it.status != TaskStatuses.DONE }

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
        SheetHeader(if (initial == null) "New task" else "Edit task") { /* dismiss handled by sheet scrim */ }

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowTextField(
                label = "Title",
                value = title,
                onValueChange = { title = it },
                placeholder = "What needs doing?",
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = {
                    if (title.isNotBlank()) {
                        suggesting = true
                        scope.launch {
                            runCatching { onSuggest(title) }.onSuccess { r ->
                                r.priority?.let { priority = it }
                                r.durationMin?.let { durationMin = it }
                            }
                            suggesting = false
                        }
                    }
                },
                enabled = title.isNotBlank() && !suggesting,
                shape = RoundedCornerShape(10.dp),
            ) {
                if (suggesting) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                } else {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text("Suggest")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField("Duration (min)", durationMin, { durationMin = it }, Modifier.weight(1f))
            Column(Modifier.weight(1f)) {
                FieldLabel("Priority")
                SegmentedSelector(
                    options = listOf(1, 2, 3, 4),
                    selected = priority,
                    onSelect = { priority = it },
                    label = { "P$it" },
                )
            }
        }

        DateField("Deadline", dueMillis, { dueMillis = it })

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField("Min chunk (min)", minChunk, { minChunk = it }, Modifier.weight(1f))
            NumberField("Max chunk (min)", maxChunk, { maxChunk = it }, Modifier.weight(1f))
        }

        FlowTextField("Notes", notes, { notes = it }, singleLine = false, minLines = 2)

        if (candidates.isNotEmpty()) {
            Column {
                FieldLabel("Depends on (scheduled after these finish)")
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .verticalScroll(rememberScrollState())
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .padding(4.dp),
                ) {
                    candidates.forEach { c ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    deps = if (deps.contains(c.id)) deps - c.id else deps + c.id
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = deps.contains(c.id), onCheckedChange = {
                                deps = if (it) deps + c.id else deps - c.id
                            })
                            Text(c.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        }
                    }
                }
            }
        }

        SaveButton(
            text = if (initial == null) "Add task" else "Save changes",
            enabled = title.isNotBlank(),
            busy = busy,
            onClick = {
                onSave(
                    TaskBody(
                        title = title.trim(),
                        notes = notes.ifBlank { null },
                        durationMin = durationMin,
                        minChunkMin = minChunk,
                        maxChunkMin = maxOf(maxChunk, minChunk),
                        due = dueMillis?.let { dueIsoFromUtcMillis(it) },
                        priority = priority,
                        status = initial?.status ?: TaskStatuses.TODO,
                        dependsOnIds = deps.toList(),
                    ),
                )
            },
        )
    }
}
