package com.flowtime.app.data.remote

import kotlinx.serialization.Serializable

/**
 * Wire types mirroring the web API (web/lib/ui.ts DTOs + web/lib/types.ts inputs).
 * Response fields the app doesn't use (userId, createdAt, …) are dropped via
 * Json { ignoreUnknownKeys = true }. Dates are ISO-8601 strings.
 */

// ---------- Responses ----------

@Serializable
data class TaskDto(
    val id: String,
    val title: String,
    val notes: String? = null,
    val durationMin: Int = 60,
    val minChunkMin: Int = 30,
    val maxChunkMin: Int = 120,
    val due: String? = null,
    val priority: Int = 3,
    val status: String = "todo",
    val scheduledMin: Int = 0,
    val dependsOnIds: List<String> = emptyList(),
)

@Serializable
data class EventDto(
    val id: String,
    val title: String,
    val start: String,
    val end: String,
    val type: String,
    val state: String = "FREE",
    val flexible: Boolean = true,
    val locked: Boolean = false,
    val color: String = "#5b5bd6",
    val noBuffer: Boolean = false,
    val seriesOverride: Boolean = false,
    val sourceTaskId: String? = null,
    val sourceHabitId: String? = null,
    val sourceSeriesId: String? = null,
)

@Serializable
data class HabitDto(
    val id: String,
    val title: String,
    val durationMin: Int = 30,
    val frequency: String = "DAILY",
    val perWeek: Int = 3,
    val idealWindowStartMin: Int = 540,
    val idealWindowEndMin: Int = 1020,
    val kind: String = "SOLO",
    val color: String = "#10b981",
    val active: Boolean = true,
)

@Serializable
data class RecurringEventDto(
    val id: String,
    val title: String,
    val startMin: Int = 480,
    val endMin: Int = 900,
    val days: String = "1,2,3,4,5",
    val color: String = "#0e7490",
    val active: Boolean = true,
)

@Serializable
data class TimeOffDto(
    val id: String,
    val label: String? = null,
    val start: String,
    val end: String,
)

@Serializable
data class SettingsDto(
    val workdayStartMin: Int = 540,
    val workdayEndMin: Int = 1020,
    val workdays: String = "1,2,3,4,5",
    val defaultBufferMin: Int = 15,
    val weeklyFocusTargetHours: Int = 12,
    val lockHorizonHours: Int = 24,
    val planHorizonDays: Int = 14,
    val minTaskDurationForBuffer: Int = 90,
    val minGapBetweenTaskChunks: Int = 15,
)

@Serializable
data class PlanResultDto(
    val created: Int = 0,
    val unscheduledTaskIds: List<String> = emptyList(),
    val partialTaskIds: List<String> = emptyList(),
    val rescheduledOverdueTaskIds: List<String> = emptyList(),
)

@Serializable
data class OkResponse(val ok: Boolean = true)

// ---------- Request bodies ----------

@Serializable
data class TaskBody(
    val title: String,
    val notes: String? = null,
    val durationMin: Int,
    val minChunkMin: Int,
    val maxChunkMin: Int,
    val due: String? = null, // ISO instant or null
    val priority: Int,
    val status: String,
    val dependsOnIds: List<String> = emptyList(),
)

@Serializable
data class HabitBody(
    val title: String,
    val durationMin: Int,
    val frequency: String,
    val perWeek: Int,
    val idealWindowStartMin: Int,
    val idealWindowEndMin: Int,
    val kind: String,
    val color: String,
    val active: Boolean,
)

/** Create a fixed meeting on the calendar (POST /api/events). */
@Serializable
data class EventBody(
    val title: String,
    val start: String,
    val end: String,
    val type: String = "MEETING",
    val flexible: Boolean = false,
    val state: String = "BUSY",
    val locked: Boolean = false,
)

/** Narrow PATCH bodies for /api/events/{id} (eventPatch is partial). A moved
 *  block is pinned (locked + BUSY), matching the web planner. */
@Serializable
data class EventMoveBody(
    val start: String,
    val end: String,
    val locked: Boolean = true,
    val state: String = "BUSY",
)

/** Moving an occurrence that is already a series override. */
@Serializable
data class EventSeriesMoveBody(
    val start: String,
    val end: String,
    val locked: Boolean = true,
    val state: String = "BUSY",
    val seriesOverride: Boolean = true,
)

/** First move of a series occurrence — stamps the original slot so it stays skipped. */
@Serializable
data class EventSeriesFirstMoveBody(
    val start: String,
    val end: String,
    val locked: Boolean = true,
    val state: String = "BUSY",
    val seriesOverride: Boolean = true,
    val seriesOriginalDate: String, // YYYY-MM-DD
)

@Serializable
data class EventLockBody(val locked: Boolean, val state: String)

@Serializable
data class EventBufferBody(val noBuffer: Boolean)

@Serializable
data class ActiveBody(val active: Boolean)

@Serializable
data class ParseBody(val text: String)

@Serializable
data class ParsedTask(
    val title: String,
    val durationMin: Int = 60,
    val due: String? = null,
    val priority: Int = 3,
)

@Serializable
data class ParseResult(
    val enabled: Boolean = false,
    val task: ParsedTask,
)

@Serializable
data class RecurringBody(
    val title: String,
    val startMin: Int,
    val endMin: Int,
    val days: String,
    val color: String,
    val active: Boolean = true,
)

@Serializable
data class TimeOffBody(
    val label: String? = null,
    val start: String, // YYYY-MM-DD
    val end: String, // YYYY-MM-DD
)

@Serializable
data class PrioritizeBody(val title: String)

@Serializable
data class PrioritizeResult(
    val priority: Int? = null,
    val durationMin: Int? = null,
)

/** Settings PATCH — all fields sent (loaded from current settings, then edited). */
@Serializable
data class SettingsBody(
    val workdayStartMin: Int,
    val workdayEndMin: Int,
    val workdays: String,
    val defaultBufferMin: Int,
    val weeklyFocusTargetHours: Int,
    val lockHorizonHours: Int,
    val planHorizonDays: Int,
    val minTaskDurationForBuffer: Int,
    val minGapBetweenTaskChunks: Int,
)
