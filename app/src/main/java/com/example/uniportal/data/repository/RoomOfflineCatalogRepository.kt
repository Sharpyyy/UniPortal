package com.example.uniportal.data.repository

import com.example.uniportal.data.local.dao.OfflineCountryDao
import com.example.uniportal.data.local.dao.UniversityDao
import com.example.uniportal.data.mapper.toDomain
import com.example.uniportal.domain.model.OfflineCountry
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class RoomOfflineCatalogRepository @Inject constructor(
    private val offlineCountryDao: OfflineCountryDao,
    private val universityDao: UniversityDao,
) : OfflineCatalogRepository {
    override fun observeCountries(): Flow<List<OfflineCountry>> =
        offlineCountryDao.observeAll()
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()

    override suspend fun remove(countryName: String) {
        offlineCountryDao.delete(countryName)
    }

    override fun observeCachedCount(): Flow<Int> = universityDao.observeCount().distinctUntilChanged()
}
