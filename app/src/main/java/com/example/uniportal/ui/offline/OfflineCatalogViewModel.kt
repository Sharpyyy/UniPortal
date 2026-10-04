package com.example.uniportal.ui.offline

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniportal.data.connectivity.ConnectivityObserver
import com.example.uniportal.data.repository.OfflineCatalogRepository
import com.example.uniportal.domain.model.CountryFilter
import com.example.uniportal.domain.model.OfflineCountry
import com.example.uniportal.util.WhileUiSubscribed
import com.example.uniportal.util.runCatchingCancellable
import com.example.uniportal.work.DownloadState
import com.example.uniportal.work.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class OfflineCatalogViewModel @Inject constructor(
    private val repository: OfflineCatalogRepository,
    private val workScheduler: WorkScheduler,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {
    private val countries: Flow<List<OfflineCountry>> = repository.observeCountries()
        .catch { error ->
            Log.w(TAG, "Failed to observe offline countries", error)
            emit(emptyList())
        }

    private val downloads: Flow<Map<String, DownloadState>> = workScheduler.observeDownloads()
        .catch { error ->
            Log.w(TAG, "Failed to observe downloads", error)
            emit(emptyMap())
        }

        .onStart { emit(emptyMap()) }

    private val cachedCount: Flow<Int> = repository.observeCachedCount()
        .catch { error ->
            Log.w(TAG, "Failed to count cached universities", error)
            emit(0)
        }

    private val isOnline: Flow<Boolean> = connectivityObserver.isOnline
        .catch { error ->
            Log.w(TAG, "Failed to observe connectivity", error)
            emit(true)
        }

    val uiState: StateFlow<OfflineCatalogUiState> = combine(
        countries,
        downloads,
        cachedCount,
        isOnline,
    ) { downloadedCountries, downloadStates, count, online ->
        val downloadedNames = downloadedCountries.map { it.countryName }.toSet()
        OfflineCatalogUiState(
            isLoading = false,
            downloaded = downloadedCountries.map { country ->
                DownloadedCountry(
                    country = country,
                    filter = filterFor(country.countryName),
                    download = downloadStates[country.countryName]?.takeIf { it.isShown },
                )
            },
            available = CountryFilter.entries.mapNotNull { filter ->
                val name = filter.apiName
                if (name == null || name in downloadedNames) {
                    null
                } else {
                    AvailableCountry(filter = filter, download = downloadStates[name]?.takeIf { it.isShown })
                }
            },
            cachedCount = count,
            isOnline = online,
        )
    }.stateIn(viewModelScope, WhileUiSubscribed, OfflineCatalogUiState())

    fun onEvent(event: OfflineCatalogEvent) {
        when (event) {
            is OfflineCatalogEvent.Download -> schedule("download ${event.country}") {
                workScheduler.downloadCountry(event.country)
            }
            is OfflineCatalogEvent.CancelDownload -> schedule("cancel the download of ${event.country}") {
                workScheduler.cancelDownload(event.country)
            }
            is OfflineCatalogEvent.Remove -> viewModelScope.launch {
                runCatchingCancellable { repository.remove(event.countryName) }
                    .onFailure { error -> Log.w(TAG, "Failed to remove ${event.countryName}", error) }
            }
        }
    }

    private fun schedule(description: String, action: () -> Unit) {
        try {
            action()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Failed to $description", e)
        }
    }

    private companion object {
        const val TAG = "OfflineCatalogVM"
    }
}

private val DownloadState.isShown: Boolean
    get() = isActive || this == DownloadState.Failed

private fun filterFor(countryName: String): CountryFilter? =
    CountryFilter.entries.firstOrNull { it.apiName == countryName }
