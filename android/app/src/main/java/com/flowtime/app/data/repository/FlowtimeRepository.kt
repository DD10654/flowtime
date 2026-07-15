package com.flowtime.app.data.repository

import com.flowtime.app.data.remote.ActiveBody
import com.flowtime.app.data.remote.ApiService
import com.flowtime.app.data.remote.EventBody
import com.flowtime.app.data.remote.EventBufferBody
import com.flowtime.app.data.remote.EventDto
import com.flowtime.app.data.remote.EventLockBody
import com.flowtime.app.data.remote.EventMoveBody
import com.flowtime.app.data.remote.EventSeriesFirstMoveBody
import com.flowtime.app.data.remote.EventSeriesMoveBody
import com.flowtime.app.data.remote.HabitBody
import com.flowtime.app.data.remote.HabitDto
import com.flowtime.app.data.remote.ParseBody
import com.flowtime.app.data.remote.ParseResult
import com.flowtime.app.data.remote.PlanResultDto
import com.flowtime.app.data.remote.PrioritizeBody
import com.flowtime.app.data.remote.PrioritizeResult
import com.flowtime.app.data.remote.RecurringBody
import com.flowtime.app.data.remote.RecurringEventDto
import com.flowtime.app.data.remote.SettingsBody
import com.flowtime.app.data.remote.SettingsDto
import com.flowtime.app.data.remote.TaskBody
import com.flowtime.app.data.remote.TaskDto
import com.flowtime.app.data.remote.TimeOffBody
import com.flowtime.app.data.remote.TimeOffDto
import com.flowtime.app.domain.local
import com.flowtime.app.domain.parseInstant
import com.flowtime.app.domain.ymd

/**
 * Single gateway to the web API. Suspend functions throw on network/HTTP errors;
 * ViewModels catch and surface them as error UI state. Mutations do not replan
 * automatically — callers invoke [replan] where the web client does (mirrors the
 * "client triggers the plan" model).
 */
class FlowtimeRepository(private val api: ApiService) {

    // Tasks
    suspend fun tasks(): List<TaskDto> = api.getTasks()
    suspend fun createTask(body: TaskBody): TaskDto = api.createTask(body)
    suspend fun updateTask(id: String, body: TaskBody): TaskDto = api.updateTask(id, body)
    suspend fun deleteTask(id: String) { api.deleteTask(id) }

    // Habits
    suspend fun habits(): List<HabitDto> = api.getHabits()
    suspend fun createHabit(body: HabitBody): HabitDto = api.createHabit(body)
    suspend fun updateHabit(id: String, body: HabitBody): HabitDto = api.updateHabit(id, body)
    suspend fun pauseHabit(id: String) { api.setHabitActive(id, ActiveBody(false)) }
    suspend fun deleteHabit(id: String) { api.deleteHabit(id) }

    // Events
    suspend fun events(): List<EventDto> = api.getEvents()
    suspend fun createEvent(body: EventBody): EventDto = api.createEvent(body)

    /** Move a block to a new time, pinning it. Series occurrences become overrides. */
    suspend fun moveEvent(event: EventDto, startIso: String, endIso: String): EventDto = when {
        event.sourceSeriesId != null && !event.seriesOverride ->
            api.moveSeriesFirst(
                event.id,
                EventSeriesFirstMoveBody(
                    start = startIso,
                    end = endIso,
                    seriesOriginalDate = ymd(parseInstant(event.start).local().date),
                ),
            )
        event.sourceSeriesId != null ->
            api.moveSeriesEvent(event.id, EventSeriesMoveBody(startIso, endIso))
        else -> api.moveEvent(event.id, EventMoveBody(startIso, endIso))
    }

    suspend fun setEventLocked(id: String, locked: Boolean): EventDto =
        api.lockEvent(id, EventLockBody(locked, if (locked) "BUSY" else "FREE"))
    suspend fun setEventBuffer(id: String, noBuffer: Boolean): EventDto =
        api.setEventBuffer(id, EventBufferBody(noBuffer))
    suspend fun deleteEvent(id: String) { api.deleteEvent(id) }

    // Recurring commitments
    suspend fun recurring(): List<RecurringEventDto> = api.getRecurring()
    suspend fun createRecurring(body: RecurringBody): RecurringEventDto = api.createRecurring(body)
    suspend fun updateRecurring(id: String, body: RecurringBody): RecurringEventDto =
        api.updateRecurring(id, body)
    suspend fun setRecurringActive(id: String, active: Boolean) { api.setRecurringActive(id, ActiveBody(active)) }
    suspend fun pauseSeries(id: String) { api.setRecurringActive(id, ActiveBody(false)) }
    suspend fun deleteRecurring(id: String) { api.deleteRecurring(id) }

    // Time off
    suspend fun timeOff(): List<TimeOffDto> = api.getTimeOff()
    suspend fun createTimeOff(body: TimeOffBody): TimeOffDto = api.createTimeOff(body)
    suspend fun deleteTimeOff(id: String) { api.deleteTimeOff(id) }

    // Settings
    suspend fun settings(): SettingsDto = api.getSettings()
    suspend fun updateSettings(body: SettingsBody): SettingsDto = api.updateSettings(body)

    // Replan
    suspend fun replan(): PlanResultDto = api.plan()

    // LLM suggestion for a task's priority/duration
    suspend fun prioritize(title: String): PrioritizeResult = api.prioritize(PrioritizeBody(title))

    // NL quick-add: parse free text into task fields (server has a regex fallback)
    suspend fun parseTask(text: String): ParseResult = api.parse(ParseBody(text))
}
