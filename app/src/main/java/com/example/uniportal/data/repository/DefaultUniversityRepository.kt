package com.example.uniportal.data.repository

import com.example.uniportal.data.local.dao.UniversityDao
import com.example.uniportal.data.local.entity.SearchQueryEntity
import com.example.uniportal.data.mapper.escapeLikePattern
import com.example.uniportal.data.mapper.normalizedQuery
import com.example.uniportal.data.mapper.queryKey
import com.example.uniportal.data.mapper.toCachedSearch
import com.example.uniportal.data.mapper.toDomain
import com.example.uniportal.data.mapper.toEntity
import com.example.uniportal.data.remote.UniversitiesApi
import com.example.uniportal.domain.model.CachedSearch
import com.example.uniportal.domain.model.SearchParams
import com.example.uniportal.domain.model.University
import com.example.uniportal.domain.model.UniversityNotFoundException
import com.example.uniportal.domain.model.UniversityPage
import com.example.uniportal.util.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class DefaultUniversityRepository @Inject constructor(
    private val api: UniversitiesApi,
    private val dao: UniversityDao,
    private val timeProvider: TimeProvider,
) : UniversityRepository {
    override fun observeSearch(params: SearchParams): Flow<CachedSearch?> =
        dao.observeSearch(params.queryKey())
            .map { rows -> rows.toCachedSearch() }
            .distinctUntilChanged()

    override suspend fun refreshSearch(params: SearchParams) {
        val page = fetchPage(params, page = 0)
        val now = timeProvider.now()
        dao.replaceSearch(
            query = SearchQueryEntity(
                queryKey = params.queryKey(),
                query = params.normalizedQuery(),
                country = params.country.apiName.orEmpty(),
                loadedPages = 1,
                hasMore = page.hasMore,
                fetchedAt = now,
            ),
            universities = page.items.map { it.toEntity(fetchedAt = now) },
        )
    }

    override suspend fun loadNextPage(params: SearchParams) {
        val key = params.queryKey()
        val cached = dao.getSearchQuery(key)
        if (cached == null) {
            refreshSearch(params)
            return
        }
        if (!cached.hasMore) return

        val page = fetchPage(params, page = cached.loadedPages)
        val now = timeProvider.now()

        dao.appendSearchPage(
            queryKey = key,
            expectedLoadedPages = cached.loadedPages,
            hasMore = page.hasMore,
            universities = page.items.map { it.toEntity(fetchedAt = now) },
        )
    }

    override fun observeLocalSearch(params: SearchParams, limit: Int): Flow<List<University>> =
        dao.observeLocalSearch(
            query = params.query.trim().escapeLikePattern(),
            country = params.country.apiName.orEmpty(),
            limit = limit,
        )
            .map { entities -> entities.map { it.toDomain() }.distinctBy { it.id } }
            .distinctUntilChanged()

    override fun observeUniversity(id: String): Flow<University?> =
        dao.observe(id.normalizedId())
            .map { entity -> entity?.toDomain() }
            .distinctUntilChanged()

    override suspend fun refreshUniversity(id: String): University {
        val domain = id.normalizedId()
        val candidates = api.findByDomain(domain).mapNotNull { it.toDomain() }

        val university = candidates.firstOrNull { it.id == domain }
            ?: candidates.firstOrNull()?.copy(id = domain)
            ?: throw UniversityNotFoundException(domain)
        dao.upsert(university.toEntity(fetchedAt = timeProvider.now()))
        return university
    }

    override suspend fun cachedUniversityFetchedAt(id: String): Long? = dao.getFetchedAt(id.normalizedId())

    override suspend fun searchFetchedAt(params: SearchParams): Long? =
        dao.getSearchQuery(params.queryKey())?.fetchedAt

    override suspend fun stalePinnedIds(staleBefore: Long, limit: Int): List<String> =
        dao.getStalePinnedIds(staleBefore = staleBefore, limit = limit)

    override suspend fun pruneCache(olderThan: Long): Int = dao.prune(threshold = olderThan)

    override suspend fun clearUnpinnedCache(): Int = dao.clearUnpinned()

    private suspend fun fetchPage(params: SearchParams, page: Int): UniversityPage {
        val pageSize = UniversityRepository.PAGE_SIZE
        val dtos = api.search(
            name = params.query.trim().ifEmpty { null },
            country = params.country.apiName,
            limit = pageSize,
            offset = page * pageSize,
        )
        return UniversityPage(
            items = dtos.mapNotNull { it.toDomain() }.distinctBy { it.id },
            page = page,
            hasMore = dtos.size == pageSize,
        )
    }

    private fun String.normalizedId(): String = trim().lowercase()
}
