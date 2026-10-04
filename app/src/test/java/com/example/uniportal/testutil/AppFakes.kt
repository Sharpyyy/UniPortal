package com.example.uniportal.testutil

import androidx.lifecycle.SavedStateHandle
import com.example.uniportal.domain.model.AppSettings
import com.example.uniportal.ui.detail.UniversityDetailViewModel
import com.example.uniportal.ui.list.UniversityListViewModel
import com.example.uniportal.ui.navigation.Routes

class AppFakes(
    val timeProvider: FakeTimeProvider = FakeTimeProvider(),
    initialSettings: AppSettings = AppSettings(),
    online: Boolean = true,
) {
    val universityRepository = FakeUniversityRepository(timeProvider)
    val favouritesRepository = FakeFavouritesRepository()
    val historyRepository = FakeHistoryRepository(timeProvider = timeProvider)
    val applicationRepository = FakeApplicationRepository(timeProvider = timeProvider)
    val shortlistRepository = FakeShortlistRepository(timeProvider)
    val settingsRepository = FakeSettingsRepository(initialSettings)
    val offlineCatalogRepository = FakeOfflineCatalogRepository()
    val connectivity = FakeConnectivityObserver(initiallyOnline = online)

    fun listViewModel(): UniversityListViewModel = UniversityListViewModel(
        repository = universityRepository,
        favouritesRepository = favouritesRepository,
        applicationRepository = applicationRepository,
        shortlistRepository = shortlistRepository,
        settingsRepository = settingsRepository,
        offlineCatalogRepository = offlineCatalogRepository,
        connectivityObserver = connectivity,
        timeProvider = timeProvider,
    )

    fun detailViewModel(id: String): UniversityDetailViewModel =
        detailViewModel(SavedStateHandle(mapOf(Routes.ARG_ID to id)))

    fun detailViewModel(savedStateHandle: SavedStateHandle): UniversityDetailViewModel = UniversityDetailViewModel(
        savedStateHandle = savedStateHandle,
        universityRepository = universityRepository,
        historyRepository = historyRepository,
        favouritesRepository = favouritesRepository,
        shortlistRepository = shortlistRepository,
        applicationRepository = applicationRepository,
        settingsRepository = settingsRepository,
        connectivityObserver = connectivity,
        timeProvider = timeProvider,
    )
}
