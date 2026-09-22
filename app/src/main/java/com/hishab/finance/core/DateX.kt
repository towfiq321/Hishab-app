package com.hishab.finance.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Date helpers. Dates are persisted as epoch-day [Long] and times as minutes-from-midnight [Int],
 * which keeps rows sortable in SQL with no timezone ambiguity.
 */
object DateX {
    /** The Bangladesh week starts on Saturday; weekly reports use the same boundary. */
    val WEEK_START: DayOfWeek = DayOfWeek.SATURDAY

    fun today(): LocalDate = LocalDate.now()
    fun nowMinutes(): Int = LocalTime.now().let { it.hour * 60 + it.minute }
    fun fileTimestamp(): String = LocalDateTime.now().format(FILE_STAMP)

    /** `875` -> `02:35 PM`. */
    fun minutesToLabel(minutes: Int): String {
        val h24 = (minutes / 60).coerceIn(0, 23)
        val m = (minutes % 60).coerceIn(0, 59)
        val suffix = if (h24 < 12) "AM" else "PM"
        val h12 = when {
            h24 == 0 -> 12
            h24 > 12 -> h24 - 12
            else -> h24
        }
        return String.format(Locale.US, "%02d:%02d %s", h12, m, suffix)
    }

    /** Inclusive day count between two dates. */
    fun daysBetweenInclusive(from: LocalDate, to: LocalDate): Int =
        (to.toEpochDay() - from.toEpochDay()).toInt() + 1
}

private val DAY_FULL = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
private val DAY_MEDIUM = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private val DAY_SHORT = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val WEEKDAY_SHORT = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
private val MONTH_LONG = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val MONTH_SHORT = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
private val FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm", Locale.ENGLISH)

fun LocalDate.startOfWeek(): LocalDate = with(TemporalAdjusters.previousOrSame(DateX.WEEK_START))
fun LocalDate.endOfWeek(): LocalDate = startOfWeek().plusDays(6)
fun LocalDate.startOfMonth(): LocalDate = withDayOfMonth(1)
fun LocalDate.endOfMonth(): LocalDate = with(TemporalAdjusters.lastDayOfMonth())
fun LocalDate.toYearMonth(): YearMonth = YearMonth.of(year, monthValue)

fun LocalDate.fullLabel(): String = format(DAY_FULL)
fun LocalDate.mediumLabel(): String = format(DAY_MEDIUM)
fun LocalDate.shortLabel(): String = format(DAY_SHORT)
fun LocalDate.weekdayLabel(): String = format(WEEKDAY_SHORT)

fun YearMonth.longLabel(): String = atDay(1).format(MONTH_LONG)
fun YearMonth.shortLabel(): String = atDay(1).format(MONTH_SHORT)
fun YearMonth.key(): String = String.format(Locale.US, "%04d-%02d", year, monthValue)
fun yearMonthOfKey(key: String): YearMonth = YearMonth.parse(key)

/** Friendly heading used above a day's transactions. */
fun LocalDate.relativeLabel(reference: LocalDate = DateX.today()): String = when {
    this == reference -> "Today"
    this == reference.minusDays(1) -> "Yesterday"
    this == reference.plusDays(1) -> "Tomorrow"
    else -> fullLabel()
}

/** `Sat 12 Sep - Fri 18 Sep 2026` */
fun rangeLabel(from: LocalDate, to: LocalDate): String =
    "${from.format(DAY_SHORT)} \u2013 ${to.format(DAY_MEDIUM)}"

/** Filename-safe timestamp, e.g. `20260921-1435`. */
fun fileTimestampSafe(): String = DateX.fileTimestamp()

/** Shorthand for [DateX.minutesToLabel] so composables can import a single symbol. */
fun minutesLabel(minutes: Int): String = DateX.minutesToLabel(minutes)
