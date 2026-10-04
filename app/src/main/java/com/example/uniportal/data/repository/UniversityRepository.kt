package com.example.uniportal.data.repository

import com.example.uniportal.domain.model.CachedSearch
import com.example.uniportal.domain.model.SearchParams
import com.example.uniportal.domain.model.University
import kotlinx.coroutines.flow.Flow

interface UniversityRepository {
    fun observeSearch(params: SearchParams): Flow<CachedSearch?>

    suspend fun refreshSearch(params: SearchParams)

    suspend fun loadNextPage(params: SearchParams)

    fun observeLocalSearch(params: SearchParams, limit: Int = LOCAL_SEARCH_LIMIT): Flow<List<University>>

    fun observeUniversity(id: String): Flow<University?>

    suspend fun refreshUniversity(id: String): University

    suspend fun cachedUniversityFetchedAt(id: String): Long?

    suspend fun searchFetchedAt(params: SearchParams): Long?

    suspend fun stalePinnedIds(staleBefore: Long, limit: Int): List<String>

    suspend fun pruneCache(olderThan: Long): Int

    suspend fun clearUnpinnedCache(): Int

    companion object {
        const val PAGE_SIZE = 20
        const val LOCAL_SEARCH_LIMIT = 100
    }
}
