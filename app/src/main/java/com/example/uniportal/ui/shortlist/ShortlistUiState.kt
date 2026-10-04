package com.example.uniportal.ui.shortlist

import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.ErrorType
import com.example.uniportal.domain.model.ShortlistDetails
import com.example.uniportal.domain.model.ShortlistItem

data class ShortlistUiState(
    val content: ShortlistContent = ShortlistContent.Loading,

    val applicationStatuses: Map<String, ApplicationStatus> = emptyMap(),

    val form: ShortlistFormState? = null,

    val isDeleting: Boolean = false,

    val isDeleted: Boolean = false,

    val message: ShortlistMessage? = null,
)

sealed interface ShortlistContent {
    data object Loading : ShortlistContent
    data class Error(val type: ErrorType) : ShortlistContent

    data object NotFound : ShortlistContent

    data class Success(val details: ShortlistDetails) : ShortlistContent
}

sealed interface ShortlistMessage {
    data object Saved : ShortlistMessage

    data object SaveFailed : ShortlistMessage
}

sealed interface ShortlistEvent {
    data object Retry : ShortlistEvent
    data class RemoveItem(val item: ShortlistItem) : ShortlistEvent

    data class RestoreItem(val item: ShortlistItem) : ShortlistEvent
    data object OpenEditForm : ShortlistEvent

    data class Form(val event: ShortlistFormEvent) : ShortlistEvent

    data object Delete : ShortlistEvent
    data object MessageShown : ShortlistEvent
}
