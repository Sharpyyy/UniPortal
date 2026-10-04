package com.example.uniportal.ui.more

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniportal.data.repository.ApplicationRepository
import com.example.uniportal.data.repository.FavouritesRepository
import com.example.uniportal.data.repository.HistoryRepository
import com.example.uniportal.data.repository.OfflineCatalogRepository
import com.example.uniportal.data.repository.SettingsRepository
import com.example.uniportal.data.repository.ShortlistRepository
import com.example.uniportal.domain.model.AppSettings
import com.example.uniportal.domain.model.UniversityApplication
import com.example.uniportal.util.WhileUiSubscribed
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*

data class MoreUiState(
    val favouritesCount: Int = 0,
    val shortlistsCount: Int = 0,
    val applicationsCount: Int = 0,

    val activeApplicationsCount: Int = 0,

    val cachedUniversitiesCount: Int = 0,
    val offlineCountriesCount: Int = 0,
    val historyCount: Int = 0,

    val lastSyncAt: Long? = null,
)

@HiltViewModel
class MoreViewModel @Inject constructor(
    favouritesRepository: FavouritesRepository,
    shortlistRepository: ShortlistRepository,
    applicationRepository: ApplicationRepository,
    historyRepository: HistoryRepository,
    offlineCatalogRepository: OfflineCatalogRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val personal: Flow<PersonalCounts> = combine(
        favouritesRepository.observeFavouriteIds().map { it.size }.orZero("favourites"),
        shortlistRepository.observeShortlists().map { it.size }.orZero("shortlists"),
        applicationRepository.observeApplications()
            .catch { error ->
                Log.w(TAG, "Failed to observe applications", error)
                emit(emptyList())
            },
        historyRepository.observeHistory().map { it.size }.orZero("history"),
    ) { favourites, shortlists, applications, history ->
        PersonalCounts(
            favourites = favourites,
            shortlists = shortlists,
            applications = applications,
            history = history,
        )
    }

    private val catalog: Flow<CatalogCounts> = combine(
        offlineCatalogRepository.observeCachedCount().orZero("cached universities"),
        offlineCatalogRepository.observeCountries().map { it.size }.orZero("offline countries"),
        settingsRepository.settings
            .map<AppSettings, Long?> { settings -> settings.lastSyncAt }
            .catch { error ->
                Log.w(TAG, "Failed to read settings", error)
                emit(null)
            },
    ) { cached, countries, lastSyncAt -> CatalogCounts(cached, countries, lastSyncAt) }

    val uiState: StateFlow<MoreUiState> = combine(personal, catalog) { counts, catalogCounts ->
        MoreUiState(
            favouritesCount = counts.favourites,
            shortlistsCount = counts.shortlists,
            applicationsCount = counts.applications.size,
            activeApplicationsCount = counts.applications.count { it.status.isActive },
            cachedUniversitiesCount = catalogCounts.cachedUniversities,
            offlineCountriesCount = catalogCounts.offlineCountries,
            historyCount = counts.history,
            lastSyncAt = catalogCounts.lastSyncAt,
        )
    }.stateIn(viewModelScope, WhileUiSubscribed, MoreUiState())

    private fun Flow<Int>.orZero(what: String): Flow<Int> = catch { error ->
        Log.w(TAG, "Failed to observe $what", error)
        emit(0)
    }

    private companion object {
        const val TAG = "MoreVM"
    }
}

private data class PersonalCounts(
    val favourites: Int,
    val shortlists: Int,
    val applications: List<UniversityApplication>,
    val history: Int,
)

private data class CatalogCounts(
    val cachedUniversities: Int,
    val offlineCountries: Int,
    val lastSyncAt: Long?,
)
