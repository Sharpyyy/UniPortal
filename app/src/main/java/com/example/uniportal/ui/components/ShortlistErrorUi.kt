package com.example.uniportal.ui.components

import androidx.annotation.StringRes
import com.example.uniportal.R
import com.example.uniportal.domain.rules.ApplicationError
import com.example.uniportal.domain.rules.ShortlistError

@StringRes
fun ShortlistError.messageRes(): Int = when (this) {
    ShortlistError.NAME_EMPTY -> R.string.shortlist_error_name_empty
    ShortlistError.NAME_TOO_LONG -> R.string.shortlist_error_name_too_long
    ShortlistError.NAME_DUPLICATE -> R.string.shortlist_error_name_duplicate
    ShortlistError.DESCRIPTION_TOO_LONG -> R.string.shortlist_error_description_too_long
    ShortlistError.NOT_FOUND -> R.string.shortlist_error_not_found
}

@StringRes
fun ApplicationError.messageRes(): Int = when (this) {
    ApplicationError.NOTE_TOO_LONG -> R.string.application_error_note_too_long
    ApplicationError.PRIORITY_OUT_OF_RANGE -> R.string.application_error_priority
}
