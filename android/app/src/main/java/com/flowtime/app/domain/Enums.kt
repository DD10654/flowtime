package com.flowtime.app.domain

/**
 * Enum-like string constants mirroring web/lib/types.ts. Kept as strings (not
 * Kotlin enums) to match the API's string fields and tolerate unknown values.
 */
object EventTypes {
    const val MEETING = "MEETING"
    const val TASK_BLOCK = "TASK_BLOCK"
    const val HABIT_BLOCK = "HABIT_BLOCK"
    const val FOCUS = "FOCUS"
    const val BUFFER = "BUFFER"
    const val COMMITMENT = "COMMITMENT"
    val ALL = listOf(MEETING, TASK_BLOCK, HABIT_BLOCK, FOCUS, BUFFER, COMMITMENT)
}

object EventStates {
    const val FREE = "FREE"
    const val BUSY = "BUSY"
}

object TaskStatuses {
    const val TODO = "todo"
    const val SCHEDULED = "scheduled"
    const val DONE = "done"
}

object HabitFrequencies {
    const val DAILY = "DAILY"
    const val WEEKDAYS = "WEEKDAYS"
    const val N_PER_WEEK = "N_PER_WEEK"
    val ALL = listOf(DAILY, WEEKDAYS, N_PER_WEEK)
}

object HabitKinds {
    const val SOLO = "SOLO"
    const val TEAM = "TEAM"
    val ALL = listOf(SOLO, TEAM)
}
