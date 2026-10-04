package com.example.uniportal.util

private const val REGIONAL_INDICATOR_A = 0x1F1E6

const val GLOBE_EMOJI = "🌍"

private const val UNKNOWN_FLAG_EMOJI = "🏳️"

fun flagEmoji(countryCode: String): String {
    val code = countryCode.trim().uppercase()
    if (code.length != 2 || code.any { it !in 'A'..'Z' }) return UNKNOWN_FLAG_EMOJI
    return buildString {
        code.forEach { letter -> appendCodePoint(REGIONAL_INDICATOR_A + (letter - 'A')) }
    }
}
