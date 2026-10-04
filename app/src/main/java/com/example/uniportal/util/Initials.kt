package com.example.uniportal.util

private val WHITESPACE = Regex("\\s+")

private val STOP_WORDS = setOf(
    "of", "the", "and", "for", "at", "in",
    "de", "del", "la", "le", "les", "des", "du", "di", "da", "der", "und", "y", "et",
)

fun monogram(name: String): String {
    val words = name.trim().split(WHITESPACE).filter { word -> word.any(Char::isLetter) }
    val significant = words.filterNot { it.lowercase() in STOP_WORDS }.ifEmpty { words }
    return significant
        .take(2)
        .mapNotNull { word -> word.firstOrNull(Char::isLetter) }
        .joinToString(separator = "") { it.uppercase() }
        .ifEmpty { "?" }
}
