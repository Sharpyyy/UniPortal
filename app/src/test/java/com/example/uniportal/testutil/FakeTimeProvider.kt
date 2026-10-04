package com.example.uniportal.testutil

import com.example.uniportal.util.TimeProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class FakeTimeProvider(
    var currentTimeMillis: Long = START_TIME_MILLIS,
    val zone: ZoneId = ZoneOffset.UTC,
) : TimeProvider {
    override fun now(): Long = currentTimeMillis

    override fun today(): LocalDate = Instant.ofEpochMilli(currentTimeMillis).atZone(zone).toLocalDate()

    fun advanceBy(millis: Long) {
        currentTimeMillis += millis
    }

    fun advanceDays(days: Long) {
        advanceBy(TimeUnit.DAYS.toMillis(days))
    }

    fun setToday(date: LocalDate) {
        currentTimeMillis = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    }

    companion object {
        const val START_TIME_MILLIS = 1_741_608_000_000L

        val START_DATE: LocalDate = LocalDate.of(2025, 3, 10)

        val ONE_HOUR: Long = TimeUnit.HOURS.toMillis(1)
        val ONE_DAY: Long = TimeUnit.DAYS.toMillis(1)
    }
}
