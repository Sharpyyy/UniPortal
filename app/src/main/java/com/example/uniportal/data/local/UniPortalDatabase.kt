package com.example.uniportal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.uniportal.data.local.dao.ApplicationDao
import com.example.uniportal.data.local.dao.FavouritesDao
import com.example.uniportal.data.local.dao.HistoryDao
import com.example.uniportal.data.local.dao.OfflineCountryDao
import com.example.uniportal.data.local.dao.ShortlistDao
import com.example.uniportal.data.local.dao.UniversityDao
import com.example.uniportal.data.local.entity.ApplicationEntity
import com.example.uniportal.data.local.entity.FavouriteEntity
import com.example.uniportal.data.local.entity.HistoryEntity
import com.example.uniportal.data.local.entity.OfflineCountryEntity
import com.example.uniportal.data.local.entity.SearchQueryEntity
import com.example.uniportal.data.local.entity.SearchResultEntity
import com.example.uniportal.data.local.entity.ShortlistEntity
import com.example.uniportal.data.local.entity.ShortlistItemEntity
import com.example.uniportal.data.local.entity.UniversityEntity

@Database(
    entities = [
        UniversityEntity::class,
        SearchQueryEntity::class,
        SearchResultEntity::class,
        FavouriteEntity::class,
        HistoryEntity::class,
        ShortlistEntity::class,
        ShortlistItemEntity::class,
        ApplicationEntity::class,
        OfflineCountryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(StringListConverter::class)
abstract class UniPortalDatabase : RoomDatabase() {
    abstract fun universityDao(): UniversityDao

    abstract fun favouritesDao(): FavouritesDao

    abstract fun historyDao(): HistoryDao

    abstract fun shortlistDao(): ShortlistDao

    abstract fun applicationDao(): ApplicationDao

    abstract fun offlineCountryDao(): OfflineCountryDao

    companion object {
        const val NAME = "uniportal_final.db"
    }
}
