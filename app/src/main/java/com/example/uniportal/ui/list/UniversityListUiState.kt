package com.example.uniportal.ui.list

import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.CountryFilter
import com.example.uniportal.domain.model.ErrorType
import com.example.uniportal.domain.model.University

data class UniversityListUiState(

    val query: String = "",
    val country: CountryFilter = CountryFilter.ALL,

    val favouritesOnly: Boolean = false,

    val favouriteIds: Set<String> = emptySet(),
    val applicationStatuses: Map<String, ApplicationStatus> = emptyMap(),
    val shortlistCounts: Map<String, Int> = emptyMap(),
    val content: ListContent = ListContent.Loading,

    val source: ResultsSource = ResultsSource.NONE,

    val banner: SearchBanner? = null,

    val allHiddenByFavouritesOnly: Boolean = false,

    val isRefreshing: Boolean = false,

    val isUpdating: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val loadMoreError: ErrorType? = null,

    val message: SearchMessage? = null,

    val resultsVersion: Int = 0,
) {
    val hasActiveFilters: Boolean
        get() = query.isNotBlank() || country != CountryFilter.ALL || favouritesOnly
}

sealed interface ListContent {
    data object Loading : ListContent
    data class Error(val type: ErrorType) : ListContent
    data object Empty : ListContent
    data class Success(val items: List<University>) : ListContent
}

enum class ResultsSource {
    NONE,

    CACHE,

    LOCAL_CATALOG,
}

sealed interface SearchBanner {
    data class Offline(val fetchedAt: Long) : SearchBanner

    data class RefreshFailed(val fetchedAt: Long, val error: ErrorType) : SearchBanner

    data class LocalCatalog(val isOnline: Boolean) : SearchBanner
}

enum class SearchMessage {
    REFRESH_FAILED,

    LOAD_MORE_OFFLINE,

    SETTINGS_UNAVAILABLE,
}

sealed interface UniversityListEvent {
    data class QueryChanged(val query: String) : UniversityListEvent
    data class CountrySelected(val country: CountryFilter) : UniversityListEvent
    data class FavouritesOnlyChanged(val enabled: Boolean) : UniversityListEvent

    data object ClearFilters : UniversityListEvent
    data object Retry : UniversityListEvent
    data object Refresh : UniversityListEvent
    data object LoadMore : UniversityListEvent

    data object MessageShown : UniversityListEvent
    data class ToggleFavourite(val university: University) : UniversityListEvent

    data class RestoreFavourite(val university: University) : UniversityListEvent
}
