package com.example.uniportal.di

import android.content.Context
import androidx.room.Room
import com.example.uniportal.data.local.UniPortalDatabase
import com.example.uniportal.data.local.dao.ApplicationDao
import com.example.uniportal.data.local.dao.FavouritesDao
import com.example.uniportal.data.local.dao.HistoryDao
import com.example.uniportal.data.local.dao.OfflineCountryDao
import com.example.uniportal.data.local.dao.ShortlistDao
import com.example.uniportal.data.local.dao.UniversityDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): UniPortalDatabase =
        Room.databaseBuilder(context, UniPortalDatabase::class.java, UniPortalDatabase.NAME).build()

    @Provides
    fun provideUniversityDao(database: UniPortalDatabase): UniversityDao = database.universityDao()

    @Provides
    fun provideFavouritesDao(database: UniPortalDatabase): FavouritesDao = database.favouritesDao()

    @Provides
    fun provideHistoryDao(database: UniPortalDatabase): HistoryDao = database.historyDao()

    @Provides
    fun provideShortlistDao(database: UniPortalDatabase): ShortlistDao = database.shortlistDao()

    @Provides
    fun provideApplicationDao(database: UniPortalDatabase): ApplicationDao = database.applicationDao()

    @Provides
    fun provideOfflineCountryDao(database: UniPortalDatabase): OfflineCountryDao = database.offlineCountryDao()
}
