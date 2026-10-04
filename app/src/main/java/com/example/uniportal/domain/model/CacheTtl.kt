package com.example.uniportal.domain.model

import java.util.concurrent.TimeUnit

enum class CacheTtl(val millis: Long) {
    H6(TimeUnit.HOURS.toMillis(6)),
    H24(TimeUnit.HOURS.toMillis(24)),
    D3(TimeUnit.DAYS.toMillis(3)),
    D7(TimeUnit.DAYS.toMillis(7)),
}
