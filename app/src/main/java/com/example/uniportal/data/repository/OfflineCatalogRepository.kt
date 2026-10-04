package com.example.uniportal.data.repository

import com.example.uniportal.domain.model.OfflineCountry
import kotlinx.coroutines.flow.Flow

interface OfflineCatalogRepository {
    fun observeCountries(): Flow<List<OfflineCountry>>

    suspend fun remove(countryName: String)

    fun observeCachedCount(): Flow<Int>
}
