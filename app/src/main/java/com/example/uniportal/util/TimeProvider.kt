package com.example.uniportal.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

fun interface TimeProvider {
    fun now(): Long

    fun today(): LocalDate = Instant.ofEpochMilli(now()).atZone(ZoneId.systemDefault()).toLocalDate()
}

class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): Long = System.currentTimeMillis()
}
