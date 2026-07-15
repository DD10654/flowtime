package com.flowtime.app.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowtime.app.data.remote.EventBody
import com.flowtime.app.data.remote.EventDto
import com.flowtime.app.data.remote.TaskBody
import com.flowtime.app.data.remote.TaskDto
import com.flowtime.app.data.repository.FlowtimeRepository
import com.flowtime.app.domain.TaskStatuses
import com.flowtime.app.ui.common.UiState
import com.flowtime.app.ui.common.errorMessage
import com.flowtime.app.ui.common.repoViewModelFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlannerData(
    val events: List<EventDto>,
    val upcoming: List<TaskDto>,
    val workStartMin: Int,
    val workEndMin: Int,
)

class PlannerViewModel(private val repo: FlowtimeRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<PlannerData>>(UiState.Loading)
    val state = _state.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching {
                coroutineScope {
                    val events = async { repo.events() }
                    val tasks = async { repo.tasks() }
                    val settings = async { repo.settings() }
                    Triple(events.await(), tasks.await(), settings.await())
                }
            }.onSuccess { (events, tasks, settings) ->
                _state.value = UiState.Success(buildData(events, tasks, settings.workdayStartMin, settings.workdayEndMin))
            }.onFailure { _state.value = UiState.Error(errorMessage(it)) }
        }
    }

    private fun buildData(events: List<EventDto>, tasks: List<TaskDto>, ws: Int, we: Int) =
        PlannerData(
            events = events,
            upcoming = tasks.filter { it.status != TaskStatuses.DONE }.sortedBy { it.priority }.take(6),
            workStartMin = ws,
            workEndMin = we,
        )

    /** Run a mutation, replan, then silently reload events + tasks (keeping work hours). */
    private fun mutate(onDone: () -> Unit = {}, block: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            val ok = runCatching {
                block()
                repo.replan()
            }.onFailure { _message.value = errorMessage(it) }.isSuccess
            runCatching {
                coroutineScope {
                    val events = async { repo.events() }
                    val tasks = async { repo.tasks() }
                    events.await() to tasks.await()
                }
            }.onSuccess { (events, tasks) ->
                val prev = _state.value as? UiState.Success
                val ws = prev?.data?.workStartMin ?: 540
                val we = prev?.data?.workEndMin ?: 1020
                _state.value = UiState.Success(buildData(events, tasks, ws, we))
            }
            _busy.value = false
            if (ok) onDone()
        }
    }

    fun replan() = mutate { /* replan happens inside mutate */ }

    fun moveEvent(event: EventDto, startIso: String, endIso: String) =
        mutate { repo.moveEvent(event, startIso, endIso) }

    fun toggleLock(event: EventDto) = mutate { repo.setEventLocked(event.id, !event.locked) }
    fun toggleBuffer(event: EventDto) = mutate { repo.setEventBuffer(event.id, !event.noBuffer) }
    fun deleteEvent(id: String) = mutate { repo.deleteEvent(id) }
    fun deleteTask(id: String) = mutate { repo.deleteTask(id) }
    fun deleteHabit(id: String) = mutate { repo.deleteHabit(id) }
    fun pauseHabit(id: String) = mutate { repo.pauseHabit(id) }
    fun deleteSeries(id: String) = mutate { repo.deleteRecurring(id) }
    fun pauseSeries(id: String) = mutate { repo.pauseSeries(id) }

    fun createMeeting(title: String, startIso: String, endIso: String, onDone: () -> Unit) =
        mutate(onDone) { repo.createEvent(EventBody(title = title, start = startIso, end = endIso)) }

    fun quickAdd(text: String, onDone: () -> Unit) = mutate(onDone) {
        val parsed = repo.parseTask(text).task
        repo.createTask(
            TaskBody(
                title = parsed.title,
                durationMin = parsed.durationMin,
                minChunkMin = 30,
                maxChunkMin = 120,
                due = parsed.due,
                priority = parsed.priority,
                status = TaskStatuses.TODO,
            ),
        )
    }

    fun consumeMessage() { _message.value = null }

    companion object {
        val Factory = repoViewModelFactory { PlannerViewModel(it) }
    }
}
