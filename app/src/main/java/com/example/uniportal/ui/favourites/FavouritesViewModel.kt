package com.example.uniportal.ui.favourites

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniportal.data.repository.ApplicationRepository
import com.example.uniportal.data.repository.FavouritesRepository
import com.example.uniportal.data.repository.ShortlistRepository
import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.University
import com.example.uniportal.util.WhileUiSubscribed
import com.example.uniportal.util.runCatchingCancellable
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(FlowPreview::class)
@HiltViewModel
class FavouritesViewModel @Inject constructor(
    private val repository: FavouritesRepository,
    applicationRepository: ApplicationRepository,
    shortlistRepository: ShortlistRepository,
) : ViewModel() {
    private val filterQuery = MutableStateFlow("")

    private val writes = Mutex()

    private val favourites: Flow<List<University>> = repository.observeFavourites()
        .catch { error ->

            Log.w(TAG, "Failed to observe favourites", error)
            emit(emptyList())
        }

    private val effectiveFilter: Flow<String> = filterQuery
        .debounce { text -> if (text.isBlank()) 0L else FILTER_DEBOUNCE_MS }
        .map { text -> text.trim() }
        .distinctUntilChanged()

    private val marks: Flow<FavouriteMarks> = combine(
        applicationRepository.observeStatuses().catch { error ->
            Log.w(TAG, "Failed to observe applications", error)
            emit(emptyMap())
        },
        shortlistRepository.observeMembershipCounts().catch { error ->
            Log.w(TAG, "Failed to observe shortlists", error)
            emit(emptyMap())
        },
    ) { statuses, shortlistCounts -> FavouriteMarks(statuses, shortlistCounts) }
        .onStart { emit(FavouriteMarks()) }

    val uiState: StateFlow<FavouritesUiState> = combine(
        favourites,
        filterQuery,
        effectiveFilter,
        marks,
    ) { stored, rawFilter, appliedFilter, userMarks ->
        val matching = stored.filter { it.matches(appliedFilter) }
        FavouritesUiState(
            filterQuery = rawFilter,
            totalCount = stored.size,
            content = when {
                stored.isEmpty() -> FavouritesContent.Empty
                matching.isEmpty() -> FavouritesContent.NoMatches
                else -> FavouritesContent.Success(matching)
            },
            applicationStatuses = userMarks.applicationStatuses,
            shortlistCounts = userMarks.shortlistCounts,
        )
    }.stateIn(viewModelScope, WhileUiSubscribed, FavouritesUiState())

    fun onEvent(event: FavouritesEvent) {
        when (event) {
            is FavouritesEvent.FilterChanged -> filterQuery.value = event.query
            is FavouritesEvent.Remove -> write { repository.remove(event.university.id) }
            is FavouritesEvent.Restore -> write {
                if (!repository.observeIsFavourite(event.university.id).first()) {
                    repository.add(event.university)
                }
            }
        }
    }

    private fun write(action: suspend () -> Unit) {
        viewModelScope.launch {
            writes.withLock {
                runCatchingCancellable { action() }
                    .onFailure { error -> Log.w(TAG, "Failed to update favourites", error) }
            }
        }
    }

    companion object {
        const val FILTER_DEBOUNCE_MS = 300L
        private const val TAG = "FavouritesVM"
    }
}

private data class FavouriteMarks(
    val applicationStatuses: Map<String, ApplicationStatus> = emptyMap(),
    val shortlistCounts: Map<String, Int> = emptyMap(),
)

private fun University.matches(filter: String): Boolean =
    filter.isBlank() ||
        name.contains(filter, ignoreCase = true) ||
        country.contains(filter, ignoreCase = true) ||
        countryCode.equals(filter, ignoreCase = true) ||
        stateProvince?.contains(filter, ignoreCase = true) == true ||
        domains.any { it.contains(filter, ignoreCase = true) }
