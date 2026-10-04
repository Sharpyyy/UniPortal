package com.example.uniportal.data.sync

import com.example.uniportal.data.local.dao.OfflineCountryDao
import com.example.uniportal.data.local.dao.UniversityDao
import com.example.uniportal.data.local.entity.OfflineCountryEntity
import com.example.uniportal.data.mapper.toDomain
import com.example.uniportal.data.mapper.toEntity
import com.example.uniportal.data.remote.UniversitiesApi
import com.example.uniportal.util.TimeProvider
import javax.inject.Inject

class CountryDownloader @Inject constructor(
    private val api: UniversitiesApi,
    private val universityDao: UniversityDao,
    private val offlineCountryDao: OfflineCountryDao,
    private val timeProvider: TimeProvider,
) {
    suspend fun download(
        countryName: String,
        countryCode: String,
        onProgress: suspend (downloaded: Int) -> Unit = {},
    ): Int {
        val seenIds = HashSet<String>()
        var detectedCode = countryCode
        var page = 0
        while (page < MAX_PAGES) {
            val dtos = api.search(name = null, country = countryName, limit = PAGE_SIZE, offset = page * PAGE_SIZE)
            val fresh = dtos.mapNotNull { it.toDomain() }.filter { seenIds.add(it.id) }
            if (fresh.isNotEmpty()) {
                val now = timeProvider.now()
                universityDao.upsertAll(fresh.map { it.toEntity(fetchedAt = now) })
                if (detectedCode.isEmpty()) detectedCode = fresh.first().countryCode
            }
            onProgress(seenIds.size)

            if (dtos.size < PAGE_SIZE || fresh.isEmpty()) break
            page += 1
        }
        offlineCountryDao.upsert(
            OfflineCountryEntity(
                countryName = countryName,
                countryCode = detectedCode,
                universityCount = seenIds.size,
                downloadedAt = timeProvider.now(),
            ),
        )
        return seenIds.size
    }

    companion object {
        const val PAGE_SIZE = 100

        const val MAX_PAGES = 200
    }
}
