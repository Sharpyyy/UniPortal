package com.example.uniportal.ui.favourites

import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.University

data class FavouritesUiState(

    val filterQuery: String = "",

    val totalCount: Int = 0,
    val content: FavouritesContent = FavouritesContent.Loading,

    val applicationStatuses: Map<String, ApplicationStatus> = emptyMap(),
    val shortlistCounts: Map<String, Int> = emptyMap(),
)

sealed interface FavouritesContent {
    data object Loading : FavouritesContent

    data object Empty : FavouritesContent

    data object NoMatches : FavouritesContent

    data class Success(val items: List<University>) : FavouritesContent
}

sealed interface FavouritesEvent {
    data class FilterChanged(val query: String) : FavouritesEvent
    data class Remove(val university: University) : FavouritesEvent

    data class Restore(val university: University) : FavouritesEvent
}
