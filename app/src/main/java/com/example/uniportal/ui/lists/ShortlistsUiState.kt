package com.example.uniportal.ui.lists

import com.example.uniportal.domain.model.ErrorType
import com.example.uniportal.domain.model.Shortlist
import com.example.uniportal.ui.shortlist.ShortlistFormEvent
import com.example.uniportal.ui.shortlist.ShortlistFormState

enum class ListsTab { FAVOURITES, SHORTLISTS }

data class ShortlistsUiState(
    val content: ShortlistsContent = ShortlistsContent.Loading,

    val form: ShortlistFormState? = null,

    val message: ShortlistsMessage? = null,
) {
    val shortlistCount: Int
        get() = (content as? ShortlistsContent.Success)?.shortlists?.size ?: 0
}

sealed interface ShortlistsContent {
    data object Loading : ShortlistsContent
    data class Error(val type: ErrorType) : ShortlistsContent

    data object Empty : ShortlistsContent

    data class Success(val shortlists: List<Shortlist>) : ShortlistsContent
}

sealed interface ShortlistsMessage {
    data class Created(val id: Long, val name: String) : ShortlistsMessage

    data object SaveFailed : ShortlistsMessage
}

sealed interface ShortlistsEvent {
    data object Retry : ShortlistsEvent
    data object OpenCreateForm : ShortlistsEvent

    data class Form(val event: ShortlistFormEvent) : ShortlistsEvent
    data object MessageShown : ShortlistsEvent
}
