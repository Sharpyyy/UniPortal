package com.example.uniportal.ui.offline

import com.example.uniportal.domain.model.CountryFilter
import com.example.uniportal.domain.model.OfflineCountry
import com.example.uniportal.work.DownloadState

data class OfflineCatalogUiState(

    val isLoading: Boolean = true,

    val downloaded: List<DownloadedCountry> = emptyList(),

    val available: List<AvailableCountry> = emptyList(),

    val cachedCount: Int = 0,
    val isOnline: Boolean = true,
)

data class DownloadedCountry(
    val country: OfflineCountry,
    val filter: CountryFilter?,
    val download: DownloadState?,
)

data class AvailableCountry(
    val filter: CountryFilter,
    val download: DownloadState?,
)

sealed interface OfflineCatalogEvent {
    data class Download(val country: CountryFilter) : OfflineCatalogEvent
    data class CancelDownload(val country: CountryFilter) : OfflineCatalogEvent

    data class Remove(val countryName: String) : OfflineCatalogEvent
}
