package com.example.uniportal.ui.history

import com.example.uniportal.domain.model.AppSettings
import com.example.uniportal.domain.model.ErrorType
import com.example.uniportal.domain.model.HistoryEntry

data class HistoryUiState(
    val content: HistoryContent = HistoryContent.Loading,

    val limit: Int = AppSettings.DEFAULT_HISTORY_LIMIT,
)

sealed interface HistoryContent {
    data object Loading : HistoryContent
    data class Error(val type: ErrorType) : HistoryContent
    data object Empty : HistoryContent

    data class Success(val entries: List<HistoryEntry>) : HistoryContent
}

sealed interface HistoryEvent {
    data object Retry : HistoryEvent
    data class Remove(val universityId: String) : HistoryEvent

    data class Restore(val entry: HistoryEntry) : HistoryEvent
    data object ClearAll : HistoryEvent
}
