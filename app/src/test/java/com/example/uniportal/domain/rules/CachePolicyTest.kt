package com.example.uniportal.domain.rules

import com.example.uniportal.domain.model.CacheTtl
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CachePolicyTest {
    private val now = 1_741_608_000_000L

    @Test
    fun `given each TTL, when data is just younger than it, exactly as old or older, then only the younger data is fresh`() {
        CacheTtl.entries.forEach { ttl ->
            assertFalse("$ttl: 1 ms younger than the TTL", CachePolicy.isStale(now - ttl.millis + 1, now, ttl))
            assertTrue("$ttl: exactly the TTL old", CachePolicy.isStale(now - ttl.millis, now, ttl))
            assertTrue("$ttl: older than the TTL", CachePolicy.isStale(now - ttl.millis - 1, now, ttl))
        }
    }
}
