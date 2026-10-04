package com.example.uniportal.di

import com.example.uniportal.data.connectivity.AndroidConnectivityObserver
import com.example.uniportal.data.connectivity.ConnectivityObserver
import com.example.uniportal.data.repository.ApplicationRepository
import com.example.uniportal.data.repository.DataStoreSettingsRepository
import com.example.uniportal.data.repository.DefaultUniversityRepository
import com.example.uniportal.data.repository.FavouritesRepository
import com.example.uniportal.data.repository.HistoryRepository
import com.example.uniportal.data.repository.OfflineCatalogRepository
import com.example.uniportal.data.repository.RoomApplicationRepository
import com.example.uniportal.data.repository.RoomFavouritesRepository
import com.example.uniportal.data.repository.RoomHistoryRepository
import com.example.uniportal.data.repository.RoomOfflineCatalogRepository
import com.example.uniportal.data.repository.RoomShortlistRepository
import com.example.uniportal.data.repository.SettingsRepository
import com.example.uniportal.data.repository.ShortlistRepository
import com.example.uniportal.data.repository.UniversityRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindUniversityRepository(impl: DefaultUniversityRepository): UniversityRepository

    @Binds
    abstract fun bindFavouritesRepository(impl: RoomFavouritesRepository): FavouritesRepository

    @Binds
    abstract fun bindHistoryRepository(impl: RoomHistoryRepository): HistoryRepository

    @Binds
    abstract fun bindShortlistRepository(impl: RoomShortlistRepository): ShortlistRepository

    @Binds
    abstract fun bindApplicationRepository(impl: RoomApplicationRepository): ApplicationRepository

    @Binds
    abstract fun bindOfflineCatalogRepository(impl: RoomOfflineCatalogRepository): OfflineCatalogRepository

    @Binds
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindConnectivityObserver(impl: AndroidConnectivityObserver): ConnectivityObserver
}
