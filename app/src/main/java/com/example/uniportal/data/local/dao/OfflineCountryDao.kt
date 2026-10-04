package com.example.uniportal.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.uniportal.data.local.entity.OfflineCountryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineCountryDao {
    @Query("SELECT * FROM offline_countries ORDER BY downloadedAt DESC")
    fun observeAll(): Flow<List<OfflineCountryEntity>>

    @Upsert
    suspend fun upsert(country: OfflineCountryEntity)

    @Query("DELETE FROM offline_countries WHERE countryName = :countryName")
    suspend fun delete(countryName: String)
}
