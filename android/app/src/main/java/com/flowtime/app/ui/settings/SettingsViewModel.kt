package com.flowtime.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowtime.app.data.remote.RecurringBody
import com.flowtime.app.data.remote.RecurringEventDto
import com.flowtime.app.data.remote.SettingsBody
import com.flowtime.app.data.remote.SettingsDto
import com.flowtime.app.data.remote.TimeOffBody
import com.flowtime.app.data.remote.TimeOffDto
import com.flowtime.app.data.repository.FlowtimeRepository
import com.flowtime.app.ui.common.UiState
import com.flowtime.app.ui.common.errorMessage
import com.flowtime.app.ui.common.repoViewModelFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsBundle(
    val settings: SettingsDto,
    val recurring: List<RecurringEventDto>,
    val timeOff: List<TimeOffDto>,
)

class SettingsViewModel(private val repo: FlowtimeRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<SettingsBundle>>(UiState.Loading)
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
                    val s = async { repo.settings() }
                    val r = async { repo.recurring() }
                    val t = async { repo.timeOff() }
                    SettingsBundle(s.await(), r.await(), t.await())
                }
            }.onSuccess { _state.value = UiState.Success(it) }
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
            runCatching {
                coroutineScope {
                    val s = async { repo.settings() }
                    val r = async { repo.recurring() }
                    val t = async { repo.timeOff() }
                    SettingsBundle(s.await(), r.await(), t.await())
                }
            }.onSuccess { _state.value = UiState.Success(it) }
            _busy.value = false
            if (ok) onDone()
        }
    }

    fun saveSettings(body: SettingsBody, onDone: () -> Unit) = mutate(onDone) { repo.updateSettings(body) }

    fun addRecurring(body: RecurringBody, onDone: () -> Unit) = mutate(onDone) { repo.createRecurring(body) }
    fun deleteRecurring(id: String) = mutate { repo.deleteRecurring(id) }
    fun setRecurringActive(id: String, active: Boolean) = mutate { repo.setRecurringActive(id, active) }

    fun addTimeOff(body: TimeOffBody, onDone: () -> Unit) = mutate(onDone) { repo.createTimeOff(body) }
    fun deleteTimeOff(id: String) = mutate { repo.deleteTimeOff(id) }

    fun consumeMessage() { _message.value = null }

    companion object {
        val Factory = repoViewModelFactory { SettingsViewModel(it) }
    }
}
