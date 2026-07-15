package com.flowtime.app.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Date/number formatting mirroring web/lib/ui.ts. All display is in the device's
 * local timezone; the API sends/receives ISO-8601 instants.
 */

private val WD_SHORT = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val MON_SHORT = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)
private val WD_LONG = listOf(
    "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday",
)
private val MON_LONG = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

fun parseInstant(iso: String): Instant = Instant.parse(iso)

fun Instant.local(): LocalDateTime = toLocalDateTime(TimeZone.currentSystemDefault())

/** minutes-of-day (0..1440) → "9:00 AM" */
fun minutesToLabel(min: Int): String {
    val h24 = min / 60
    val m = min % 60
    val ampm = if (h24 >= 12) "PM" else "AM"
    val h12 = if (h24 % 12 == 0) 12 else h24 % 12
    return "$h12:${m.toString().padStart(2, '0')} $ampm"
}

fun prettyDuration(min: Int): String {
    val h = min / 60
    val m = min % 60
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

/** "9:00 AM" for a wall-clock time. */
fun clockLabel(dt: LocalDateTime): String = minutesToLabel(dt.hour * 60 + dt.minute)

/** "Wed, Jul 14" from an ISO instant (or "No deadline"). */
fun dueLabel(iso: String?): String {
    if (iso.isNullOrBlank()) return "No deadline"
    val d = parseInstant(iso).local()
    return "${WD_SHORT[d.dayOfWeek.isoDayNumber - 1]}, ${MON_SHORT[d.monthNumber - 1]} ${d.dayOfMonth}"
}

/** "Wednesday, July 14" for a header. */
fun longDate(d: LocalDate): String =
    "${WD_LONG[d.dayOfWeek.isoDayNumber - 1]}, ${MON_LONG[d.monthNumber - 1]} ${d.dayOfMonth}"

/** "Jul 14" short date. */
fun shortDate(d: LocalDate): String = "${MON_SHORT[d.monthNumber - 1]} ${d.dayOfMonth}"

/** Whole minutes between two ISO instants. */
fun durationMinutes(startIso: String, endIso: String): Int {
    val s = parseInstant(startIso)
    val e = parseInstant(endIso)
    return (e - s).inWholeMinutes.toInt().coerceAtLeast(0)
}

/** "Weekdays" / "Every day" / "Mon, Wed, Fri" from a CSV of ISO weekdays. */
fun daysLabel(csv: String): String {
    val days = csv.split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it in 1..7 }
        .sorted()
    return when (days.joinToString(",")) {
        "1,2,3,4,5" -> "Weekdays"
        "1,2,3,4,5,6,7" -> "Every day"
        "6,7" -> "Weekends"
        else -> days.joinToString(", ") { WD_SHORT[it - 1] }
    }
}

/** "Jul 5 – Jul 12" from an inclusive start + exclusive end (as stored). */
// ---- Date <-> millis helpers for Material date pickers (which use UTC millis) ----

/** UTC-midnight epoch millis for a calendar date (what DatePicker expects). */
fun LocalDate.toUtcMidnightMillis(): Long =
    LocalDateTime(year, monthNumber, dayOfMonth, 0, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()

/** The calendar date a DatePicker's selectedDateMillis (UTC midnight) represents. */
fun utcMillisToLocalDate(millis: Long): LocalDate =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date

/** ISO instant for a task deadline: the picked day at 5:00 PM local (mirrors the web form). */
fun dueIsoFromUtcMillis(millis: Long): String {
    val d = utcMillisToLocalDate(millis)
    return LocalDateTime(d.year, d.monthNumber, d.dayOfMonth, 17, 0)
        .toInstant(TimeZone.currentSystemDefault()).toString()
}

/** DatePicker initial value from an existing ISO deadline. */
fun dueUtcMillisFromIso(iso: String?): Long? {
    if (iso.isNullOrBlank()) return null
    return parseInstant(iso).local().date.toUtcMidnightMillis()
}

/** ISO instant for `time` (minutes-of-day) on the given local date. */
fun isoAt(date: LocalDate, minutesOfDay: Int): String =
    LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, minutesOfDay / 60, minutesOfDay % 60)
        .toInstant(TimeZone.currentSystemDefault()).toString()

/** YYYY-MM-DD for a local date (time-off inputs). */
fun ymd(date: LocalDate): String =
    "${date.year}-${(date.monthNumber).toString().padStart(2, '0')}-${date.dayOfMonth.toString().padStart(2, '0')}"

fun timeOffRangeLabel(startIso: String, endIso: String): String {
    val s = parseInstant(startIso).local().date
    val last = parseInstant(endIso).local().date.minus(DatePeriod(days = 1))
    val f = { d: LocalDate -> "${MON_SHORT[d.monthNumber - 1]} ${d.dayOfMonth}" }
    return if (f(s) == f(last)) f(s) else "${f(s)} – ${f(last)}"
}
