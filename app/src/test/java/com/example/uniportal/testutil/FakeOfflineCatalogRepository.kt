package com.example.uniportal.testutil

import com.example.uniportal.data.repository.OfflineCatalogRepository
import com.example.uniportal.domain.model.OfflineCountry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

class FakeOfflineCatalogRepository(
    initial: List<OfflineCountry> = emptyList(),
    initialCachedCount: Int = 0,
) : OfflineCatalogRepository {
    private val stored = MutableStateFlow(normalized(initial))

    val cachedCount = MutableStateFlow(initialCachedCount)

    val countries: List<OfflineCountry>
        get() = stored.value

    val removeCalls = mutableListOf<String>()

    var readError: Throwable? = null
    var writeError: Throwable? = null

    fun putCountry(country: OfflineCountry) {
        stored.update { list -> normalized(listOf(country) + list.filterNot { it.countryName == country.countryName }) }
    }

    override fun observeCountries(): Flow<List<OfflineCountry>> = flow {
        readError?.let { throw it }
        emitAll(stored)
    }

    override suspend fun remove(countryName: String) {
        removeCalls += countryName
        writeError?.let { throw it }
        stored.update { list -> list.filterNot { it.countryName == countryName } }
    }

    override fun observeCachedCount(): Flow<Int> = flow {
        readError?.let { throw it }
        emitAll(cachedCount)
    }

    private fun normalized(list: List<OfflineCountry>): List<OfflineCountry> =
        list.sortedByDescending { it.downloadedAt }.distinctBy { it.countryName }
}
