package com.example.uniportal.ui.components

import androidx.annotation.StringRes
import com.example.uniportal.R
import com.example.uniportal.domain.model.ErrorType

@StringRes
fun ErrorType.titleRes(): Int = when (this) {
    ErrorType.NETWORK -> R.string.error_network_title
    ErrorType.SERVER -> R.string.error_server_title
    ErrorType.NOT_FOUND -> R.string.error_not_found_title
    ErrorType.UNKNOWN -> R.string.error_unknown_title
}

@StringRes
fun ErrorType.messageRes(): Int = when (this) {
    ErrorType.NETWORK -> R.string.error_network_message
    ErrorType.SERVER -> R.string.error_server_message
    ErrorType.NOT_FOUND -> R.string.error_not_found_message
    ErrorType.UNKNOWN -> R.string.error_unknown_message
}

fun ErrorType.emoji(): String = when (this) {
    ErrorType.NETWORK -> "📡"
    ErrorType.SERVER -> "🛠️"
    ErrorType.NOT_FOUND -> "🔭"
    ErrorType.UNKNOWN -> "🌀"
}
