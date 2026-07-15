package com.flowtime.app.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowtime.app.data.remote.HabitBody
import com.flowtime.app.data.remote.HabitDto
import com.flowtime.app.data.repository.FlowtimeRepository
import com.flowtime.app.ui.common.UiState
import com.flowtime.app.ui.common.errorMessage
import com.flowtime.app.ui.common.repoViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HabitsViewModel(private val repo: FlowtimeRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<HabitDto>>>(UiState.Loading)
    val state = _state.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { repo.habits() }
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(errorMessage(it)) }
        }
    }

    private fun mutate(onDone: () -> Unit = {}, block: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            val ok = runCatching {
                block()
                repo.replan()
            }.onFailure { _message.value = errorMessage(it) }.isSuccess
            runCatching { repo.habits() }.onSuccess { _state.value = UiState.Success(it) }
            _busy.value = false
            if (ok) onDone()
        }
    }

    fun createHabit(body: HabitBody, onDone: () -> Unit) = mutate(onDone) { repo.createHabit(body) }
    fun updateHabit(id: String, body: HabitBody, onDone: () -> Unit) = mutate(onDone) { repo.updateHabit(id, body) }
    fun toggleActive(h: HabitDto) = mutate { repo.updateHabit(h.id, h.toBody(active = !h.active)) }
    fun deleteHabit(id: String) = mutate { repo.deleteHabit(id) }

    fun consumeMessage() { _message.value = null }

    companion object {
        val Factory = repoViewModelFactory { HabitsViewModel(it) }
    }
}

fun HabitDto.toBody(active: Boolean = this.active) = HabitBody(
    title = title,
    durationMin = durationMin,
    frequency = frequency,
    perWeek = perWeek,
    idealWindowStartMin = idealWindowStartMin,
    idealWindowEndMin = idealWindowEndMin,
    kind = kind,
    color = color,
    active = active,
)
