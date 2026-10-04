package com.example.uniportal.util

fun String.withHttpScheme(): String =
    if (startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)) {
        this
    } else {
        "http://$this"
    }

fun String.displayUrl(): String =
    removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
        .removeSuffix("/")
