package com.example.uniportal.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.uniportal.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

@Composable
fun relativeTimeText(timestamp: Long, now: Long): String {
    val zone = ZoneId.systemDefault()
    val moment = Instant.ofEpochMilli(timestamp).atZone(zone)
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val day = moment.toLocalDate()
    val age = now - timestamp
    val minutes = TimeUnit.MILLISECONDS.toMinutes(age)
    val hours = TimeUnit.MILLISECONDS.toHours(age)
    val time = moment.format(TIME_FORMATTER)
    return when {
        age < 0 -> stringResource(R.string.time_date_at, day.format(dayFormatter(day, today)), time)
        minutes < 1 -> stringResource(R.string.time_just_now)
        minutes < 60 -> stringResource(R.string.time_minutes_ago, minutes)
        hours < RELATIVE_HOURS_LIMIT -> stringResource(R.string.time_hours_ago, hours)
        day == today -> stringResource(R.string.time_today_at, time)
        day == today.minusDays(1) -> stringResource(R.string.time_yesterday_at, time)
        day.year == today.year -> stringResource(R.string.time_date_at, day.format(DAY_FORMATTER), time)
        else -> day.format(DAY_WITH_YEAR_FORMATTER)
    }
}

@Composable
fun rememberCurrentTimeMillis(refreshMillis: Long = DEFAULT_REFRESH_MILLIS): Long {
    val now by produceState(System.currentTimeMillis(), refreshMillis) {
        while (true) {
            delay(refreshMillis)
            value = System.currentTimeMillis()
        }
    }
    return now
}

fun LocalDate.formatDay(today: LocalDate): String = format(dayFormatter(this, today))

fun LocalDate.formatFullDay(): String = format(FULL_DAY_FORMATTER)

private fun dayFormatter(day: LocalDate, today: LocalDate): DateTimeFormatter =
    if (day.year == today.year) DAY_FORMATTER else DAY_WITH_YEAR_FORMATTER

private val RUSSIAN = Locale("ru")
private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", RUSSIAN)
private val DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", RUSSIAN)
private val DAY_WITH_YEAR_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", RUSSIAN)
private val FULL_DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EE, d MMMM yyyy", RUSSIAN)

private const val RELATIVE_HOURS_LIMIT = 6
private const val DEFAULT_REFRESH_MILLIS = 30_000L
