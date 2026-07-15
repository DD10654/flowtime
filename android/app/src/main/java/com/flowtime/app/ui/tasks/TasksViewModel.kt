package com.flowtime.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowtime.app.data.remote.TaskBody
import com.flowtime.app.data.remote.TaskDto
import com.flowtime.app.data.repository.FlowtimeRepository
import com.flowtime.app.ui.common.UiState
import com.flowtime.app.ui.common.errorMessage
import com.flowtime.app.ui.common.repoViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TasksViewModel(private val repo: FlowtimeRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<TaskDto>>>(UiState.Loading)
    val state = _state.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { repo.tasks() }
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(errorMessage(it)) }
        }
    }

    /** Run a mutation, replan (as the web client does), reload, then invoke [onDone] on success. */
    private fun mutate(onDone: () -> Unit = {}, block: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            val ok = runCatching {
                block()
                repo.replan()
            }.onFailure { _message.value = errorMessage(it) }.isSuccess
            runCatching { repo.tasks() }.onSuccess { _state.value = UiState.Success(it) }
            _busy.value = false
            if (ok) onDone()
        }
    }

    fun createTask(body: TaskBody, onDone: () -> Unit) = mutate(onDone) { repo.createTask(body) }
    fun updateTask(id: String, body: TaskBody, onDone: () -> Unit) = mutate(onDone) { repo.updateTask(id, body) }

    /** Completing a task removes it entirely (mirrors the web app). */
    fun completeTask(id: String) = mutate { repo.deleteTask(id) }
    fun deleteTask(id: String) = mutate { repo.deleteTask(id) }

    suspend fun suggest(title: String) = repo.prioritize(title)

    fun consumeMessage() { _message.value = null }

    companion object {
        val Factory = repoViewModelFactory { TasksViewModel(it) }
    }
}
