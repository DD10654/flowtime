package com.flowtime.app.domain

import androidx.compose.ui.graphics.Color

/** Parse a "#RRGGBB" (or "#AARRGGBB") hex string into a Compose [Color]. */
fun hexColor(hex: String): Color {
    val h = hex.removePrefix("#")
    return when (h.length) {
        6 -> Color(("FF$h").toLong(16))
        8 -> Color(h.toLong(16))
        else -> Color(0xFF6366F1)
    }
}

/** Block colors per event type — mirrors TYPE_COLORS in web/lib/types.ts. */
val TYPE_COLORS: Map<String, Color> = mapOf(
    EventTypes.MEETING to hexColor("#475569"),
    EventTypes.TASK_BLOCK to hexColor("#3b82f6"),
    EventTypes.HABIT_BLOCK to hexColor("#10b981"),
    EventTypes.FOCUS to hexColor("#6366f1"),
    EventTypes.BUFFER to hexColor("#cbd5e1"),
    EventTypes.COMMITMENT to hexColor("#0e7490"),
)

val TYPE_LABELS: Map<String, String> = mapOf(
    EventTypes.MEETING to "Meeting",
    EventTypes.TASK_BLOCK to "Task",
    EventTypes.HABIT_BLOCK to "Habit",
    EventTypes.FOCUS to "Focus",
    EventTypes.BUFFER to "Buffer",
    EventTypes.COMMITMENT to "Commitment",
)

val PRIORITY_LABELS: Map<Int, String> = mapOf(
    1 to "P1 — Urgent",
    2 to "P2 — High",
    3 to "P3 — Medium",
    4 to "P4 — Low",
)

/** Task urgency colors — mirrors PRIORITY_COLORS in web/lib/types.ts. */
val PRIORITY_COLORS: Map<Int, Color> = mapOf(
    1 to hexColor("#dc2626"),
    2 to hexColor("#ea580c"),
    3 to hexColor("#0891b2"),
    4 to hexColor("#64748b"),
)

fun priorityColor(priority: Int): Color =
    PRIORITY_COLORS[priority] ?: TYPE_COLORS.getValue(EventTypes.TASK_BLOCK)

fun typeColor(type: String): Color =
    TYPE_COLORS[type] ?: hexColor("#5b5bd6")

val FREQUENCY_LABELS: Map<String, String> = mapOf(
    HabitFrequencies.DAILY to "Every day",
    HabitFrequencies.WEEKDAYS to "Weekdays",
    HabitFrequencies.N_PER_WEEK to "Times per week",
)

// ---- Flowtime brand palette (mirrors web CSS custom properties) ----
object Brand {
    val Primary = hexColor("#6366f1")      // indigo-500
    val PrimaryStrong = hexColor("#4f46e5") // indigo-600
    val Emerald = hexColor("#10b981")
    val Background = hexColor("#f8fafc")   // slate-50
    val Surface = Color.White
    val Border = hexColor("#e5e7eb")
    val TextMuted = hexColor("#6b7280")
}
