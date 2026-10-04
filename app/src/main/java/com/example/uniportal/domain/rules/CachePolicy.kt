package com.example.uniportal.domain.rules

import com.example.uniportal.domain.model.CacheTtl
import java.util.concurrent.TimeUnit

object CachePolicy {
    val RETENTION_MILLIS: Long = TimeUnit.DAYS.toMillis(14)

    fun isStale(fetchedAt: Long?, now: Long, ttl: CacheTtl): Boolean {
        if (fetchedAt == null) return true
        val age = now - fetchedAt
        return age < 0 || age >= ttl.millis
    }
}
