package com.flowtime.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowtime.app.data.remote.EventDto
import com.flowtime.app.data.repository.FlowtimeRepository
import com.flowtime.app.domain.EventTypes
import com.flowtime.app.domain.TYPE_LABELS
import com.flowtime.app.domain.durationMinutes
import com.flowtime.app.domain.local
import com.flowtime.app.domain.parseInstant
import com.flowtime.app.ui.common.UiState
import com.flowtime.app.ui.common.errorMessage
import com.flowtime.app.ui.common.repoViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt

data class Slice(val name: String, val hours: Double, val colorHex: String)
data class DayBars(val label: String, val segments: List<Pair<String, Double>>) // (colorHex, hours)

data class AnalyticsData(
    val totalHours: Double,
    val taskHours: Double,
    val meetingHours: Double,
    val commitmentHours: Double,
    val donut: List<Slice>,
    val weekly: List<DayBars>,
) {
    val hasData: Boolean get() = donut.any { it.hours > 0 }
}

// Type colors as hex (mirrors TYPE_COLORS in web/lib/types.ts).
private val HEX = mapOf(
    EventTypes.MEETING to "#475569",
    EventTypes.COMMITMENT to "#0e7490",
    EventTypes.TASK_BLOCK to "#3b82f6",
    EventTypes.HABIT_BLOCK to "#10b981",
    EventTypes.FOCUS to "#6366f1",
)
private val WD = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val COUNTED = listOf(
    EventTypes.MEETING, EventTypes.TASK_BLOCK, EventTypes.HABIT_BLOCK, EventTypes.FOCUS, EventTypes.COMMITMENT,
)
private val STACK = listOf(EventTypes.MEETING, EventTypes.COMMITMENT, EventTypes.TASK_BLOCK, EventTypes.HABIT_BLOCK)

class AnalyticsViewModel(private val repo: FlowtimeRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<AnalyticsData>>(UiState.Loading)
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { compute(repo.events()) }
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(errorMessage(it)) }
        }
    }

    private fun compute(events: List<EventDto>): AnalyticsData {
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(tz).date
        val weekStartDate = today.minus(DatePeriod(days = today.dayOfWeek.isoDayNumber - 1))
        val weekStart = LocalDateTime(weekStartDate.year, weekStartDate.monthNumber, weekStartDate.dayOfMonth, 0, 0).toInstant(tz)
        val weekEndDate = weekStartDate.plus(DatePeriod(days = 7))
        val weekEnd = LocalDateTime(weekEndDate.year, weekEndDate.monthNumber, weekEndDate.dayOfMonth, 0, 0).toInstant(tz)

        val totalsMin = HashMap<String, Int>()
        val weeklyMin = Array(7) { HashMap<String, Int>() }

        for (e in events) {
            val startInstant = parseInstant(e.start)
            if (startInstant < weekStart || startInstant >= weekEnd) continue
            val min = durationMinutes(e.start, e.end)
            totalsMin[e.type] = (totalsMin[e.type] ?: 0) + min
            if (e.type in COUNTED) {
                val dayIdx = (startInstant.local().date.toEpochDays() - weekStartDate.toEpochDays())
                if (dayIdx in 0..6) weeklyMin[dayIdx][e.type] = (weeklyMin[dayIdx][e.type] ?: 0) + min
            }
        }

        fun h(min: Int): Double = (min / 60.0 * 10).roundToInt() / 10.0
        fun t(type: String): Int = totalsMin[type] ?: 0

        val donut = listOf(EventTypes.MEETING, EventTypes.COMMITMENT, EventTypes.TASK_BLOCK, EventTypes.HABIT_BLOCK)
            .map { Slice(TYPE_LABELS[it] ?: it, h(t(it)), HEX[it] ?: "#6366f1") }
            .filter { it.hours > 0 }

        val weekly = (0..6).map { d ->
            DayBars(WD[d], STACK.map { type -> (HEX[type] ?: "#6366f1") to h(weeklyMin[d][type] ?: 0) })
        }

        return AnalyticsData(
            totalHours = h(t(EventTypes.MEETING) + t(EventTypes.TASK_BLOCK) + t(EventTypes.HABIT_BLOCK) + t(EventTypes.COMMITMENT)),
            taskHours = h(t(EventTypes.TASK_BLOCK)),
            meetingHours = h(t(EventTypes.MEETING)),
            commitmentHours = h(t(EventTypes.COMMITMENT)),
            donut = donut,
            weekly = weekly,
        )
    }

    companion object {
        val Factory = repoViewModelFactory { AnalyticsViewModel(it) }
    }
}
