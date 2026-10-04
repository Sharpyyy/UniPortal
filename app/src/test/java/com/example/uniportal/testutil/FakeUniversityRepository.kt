package com.example.uniportal.testutil

import com.example.uniportal.data.mapper.queryKey
import com.example.uniportal.data.repository.UniversityRepository
import com.example.uniportal.domain.model.CachedSearch
import com.example.uniportal.domain.model.SearchParams
import com.example.uniportal.domain.model.University
import com.example.uniportal.domain.model.UniversityNotFoundException
import com.example.uniportal.domain.model.UniversityPage
import com.example.uniportal.util.TimeProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeUniversityRepository(
    private val timeProvider: TimeProvider = FakeTimeProvider(),
) : UniversityRepository {
    data class LocalSearchCall(val params: SearchParams, val limit: Int)

    data class PageRequest(val params: SearchParams, val page: Int)

    data class StalePinnedCall(val staleBefore: Long, val limit: Int)

    data class CachedUniversity(val university: University, val fetchedAt: Long)

    private data class StoredSearch(
        val ids: List<String>,
        val hasMore: Boolean,
        val loadedPages: Int,
        val fetchedAt: Long,
    )

    private data class Tables(
        val searches: Map<String, StoredSearch> = emptyMap(),
        val universities: Map<String, CachedUniversity> = emptyMap(),
    )

    private val tables = MutableStateFlow(Tables())

    val observeSearchCalls = mutableListOf<SearchParams>()
    val refreshSearchCalls = mutableListOf<SearchParams>()

    val completedRefreshSearches = mutableListOf<SearchParams>()
    val loadNextPageCalls = mutableListOf<SearchParams>()
    val observeLocalSearchCalls = mutableListOf<LocalSearchCall>()
    val observeUniversityCalls = mutableListOf<String>()
    val refreshUniversityCalls = mutableListOf<String>()
    val searchFetchedAtCalls = mutableListOf<SearchParams>()
    val cachedUniversityFetchedAtCalls = mutableListOf<String>()
    val stalePinnedIdsCalls = mutableListOf<StalePinnedCall>()
    val pruneCacheCalls = mutableListOf<Long>()
    var clearUnpinnedCacheCalls = 0
        private set

    val pageRequests = mutableListOf<PageRequest>()

    var remoteUniversities: List<University> = emptyList()

    var searchHandler: suspend (params: SearchParams, page: Int) -> UniversityPage =
        { params, page -> serveRemoteSearch(params, page) }

    var universityHandler: suspend (id: String) -> University = { id -> serveRemoteUniversity(id) }

    var refreshGate: CompletableDeferred<Unit>? = null
    var loadMoreGate: CompletableDeferred<Unit>? = null
    var universityGate: CompletableDeferred<Unit>? = null

    private val queuedRefreshes = ArrayDeque<Result<UniversityPage>>()
    private val queuedLoadMores = ArrayDeque<Result<UniversityPage>>()
    private val queuedUniversities = ArrayDeque<Result<University>>()

    var pinnedIds: Set<String> = emptySet()

    var downloadedCountries: Set<String> = emptySet()

    var readError: Throwable? = null
    var clearCacheError: Throwable? = null

    val activeObservers: Int
        get() = tables.subscriptionCount.value

    fun enqueueRefresh(items: List<University>, hasMore: Boolean = false) {
        queuedRefreshes.addLast(Result.success(UniversityPage(items = items, page = 0, hasMore = hasMore)))
    }

    fun enqueueRefreshError(error: Throwable) {
        queuedRefreshes.addLast(Result.failure(error))
    }

    fun enqueueLoadMore(items: List<University>, hasMore: Boolean = false) {
        queuedLoadMores.addLast(Result.success(UniversityPage(items = items, page = 0, hasMore = hasMore)))
    }

    fun enqueueLoadMoreError(error: Throwable) {
        queuedLoadMores.addLast(Result.failure(error))
    }

    fun enqueueUniversity(university: University) {
        queuedUniversities.addLast(Result.success(university))
    }

    fun enqueueUniversityError(error: Throwable) {
        queuedUniversities.addLast(Result.failure(error))
    }

    fun putSearch(
        params: SearchParams,
        items: List<University>,
        hasMore: Boolean = false,
        loadedPages: Int = 1,
        fetchedAt: Long = timeProvider.now(),
    ) {
        val unique = items.distinctBy { it.id }
        tables.update { current ->
            current.copy(
                searches = current.searches + (
                    params.queryKey() to StoredSearch(
                        ids = unique.map { it.id },
                        hasMore = hasMore,
                        loadedPages = loadedPages,
                        fetchedAt = fetchedAt,
                    )
                    ),
                universities = current.universities + unique.associate { it.id to CachedUniversity(it, fetchedAt) },
            )
        }
    }

    fun removeSearch(params: SearchParams) {
        tables.update { current -> current.copy(searches = current.searches - params.queryKey()) }
    }

    fun putUniversity(university: University, fetchedAt: Long = timeProvider.now()) {
        tables.update { current ->
            current.copy(universities = current.universities + (university.id to CachedUniversity(university, fetchedAt)))
        }
    }

    fun removeUniversity(id: String) {
        tables.update { current -> current.copy(universities = current.universities - id) }
    }

    fun cachedSearch(params: SearchParams): CachedSearch? = tables.value.search(params.queryKey())

    fun cachedUniversity(id: String): CachedUniversity? = tables.value.universities[id]

    val cachedUniversityIds: Set<String>
        get() = tables.value.universities.keys

    val cachedSearchCount: Int
        get() = tables.value.searches.size

    override fun observeSearch(params: SearchParams): Flow<CachedSearch?> {
        observeSearchCalls += params
        val key = params.queryKey()
        return observe { current -> current.search(key) }
    }

    override suspend fun refreshSearch(params: SearchParams) {
        refreshSearchCalls += params
        val page = fetchPage(params, page = 0, queue = queuedRefreshes, gate = refreshGate)
        storeFirstPage(params, page)
        completedRefreshSearches += params
    }

    override suspend fun loadNextPage(params: SearchParams) {
        loadNextPageCalls += params
        val key = params.queryKey()
        val cached = tables.value.searches[key]
        if (cached == null) {
            storeFirstPage(params, fetchPage(params, page = 0, queue = queuedLoadMores, gate = loadMoreGate))
            return
        }
        if (!cached.hasMore) return

        val page = fetchPage(params, page = cached.loadedPages, queue = queuedLoadMores, gate = loadMoreGate)
        val now = timeProvider.now()
        tables.update { current ->
            val search = current.searches[key]
            if (search == null || search.loadedPages != cached.loadedPages) {
                current
            } else {
                val known = search.ids.toHashSet()
                val newIds = page.items.map { it.id }.filter { known.add(it) }
                current.copy(
                    searches = current.searches + (
                        key to search.copy(
                            ids = search.ids + newIds,
                            hasMore = page.hasMore,
                            loadedPages = search.loadedPages + 1,
                        )
                        ),
                    universities = current.universities + page.items.associate { it.id to CachedUniversity(it, now) },
                )
            }
        }
    }

    override fun observeLocalSearch(params: SearchParams, limit: Int): Flow<List<University>> {
        observeLocalSearchCalls += LocalSearchCall(params = params, limit = limit)
        val query = params.query.trim()
        val country = params.country.apiName
        return observe { current ->
            current.universities.values
                .map { it.university }
                .filter { query.isEmpty() || it.name.contains(query, ignoreCase = true) }
                .filter { country == null || it.country.equals(country, ignoreCase = true) }
                .sortedBy { it.name.lowercase() }
                .take(limit)
        }
    }

    override fun observeUniversity(id: String): Flow<University?> {
        observeUniversityCalls += id
        val key = id.normalizedId()
        return observe { current -> current.universities[key]?.university }
    }

    override suspend fun refreshUniversity(id: String): University {
        refreshUniversityCalls += id
        val domain = id.normalizedId()
        val queued = queuedUniversities.removeFirstOrNull()
        val currentGate = universityGate
        currentGate?.await()
        val fetched = if (queued != null) queued.getOrThrow() else universityHandler(domain)

        val university = if (fetched.id == domain) fetched else fetched.copy(id = domain)
        putUniversity(university, fetchedAt = timeProvider.now())
        return university
    }

    override suspend fun cachedUniversityFetchedAt(id: String): Long? {
        cachedUniversityFetchedAtCalls += id
        return tables.value.universities[id.normalizedId()]?.fetchedAt
    }

    override suspend fun searchFetchedAt(params: SearchParams): Long? {
        searchFetchedAtCalls += params
        return tables.value.searches[params.queryKey()]?.fetchedAt
    }

    override suspend fun stalePinnedIds(staleBefore: Long, limit: Int): List<String> {
        stalePinnedIdsCalls += StalePinnedCall(staleBefore = staleBefore, limit = limit)
        return tables.value.universities.entries
            .filter { (id, cached) -> id in pinnedIds && cached.fetchedAt < staleBefore }
            .sortedBy { it.value.fetchedAt }
            .take(limit)
            .map { it.key }
    }

    override suspend fun pruneCache(olderThan: Long): Int {
        pruneCacheCalls += olderThan
        clearCacheError?.let { throw it }
        var deleted = 0
        tables.update { current ->
            val keptSearches = current.searches.filterValues { it.fetchedAt >= olderThan }
            val listed = keptSearches.values.flatMap { it.ids }.toSet()
            val keptUniversities = current.universities.filter { (id, cached) ->
                cached.fetchedAt >= olderThan || isKept(id, cached, listed)
            }
            deleted = (current.searches.size - keptSearches.size) +
                (current.universities.size - keptUniversities.size)
            Tables(searches = keptSearches, universities = keptUniversities)
        }
        return deleted
    }

    override suspend fun clearUnpinnedCache(): Int {
        clearUnpinnedCacheCalls++
        clearCacheError?.let { throw it }
        var deleted = 0
        tables.update { current ->
            val keptUniversities = current.universities.filter { (id, cached) -> isKept(id, cached, emptySet()) }
            deleted = current.searches.size + (current.universities.size - keptUniversities.size)
            Tables(searches = emptyMap(), universities = keptUniversities)
        }
        return deleted
    }

    private suspend fun fetchPage(
        params: SearchParams,
        page: Int,
        queue: ArrayDeque<Result<UniversityPage>>,
        gate: CompletableDeferred<Unit>?,
    ): UniversityPage {
        pageRequests += PageRequest(params = params, page = page)
        val queued = queue.removeFirstOrNull()
        gate?.await()
        return if (queued != null) queued.getOrThrow() else searchHandler(params, page)
    }

    private fun storeFirstPage(params: SearchParams, page: UniversityPage) {
        val now = timeProvider.now()
        val items = page.items.distinctBy { it.id }
        tables.update { current ->
            current.copy(
                searches = current.searches + (
                    params.queryKey() to StoredSearch(
                        ids = items.map { it.id },
                        hasMore = page.hasMore,
                        loadedPages = 1,
                        fetchedAt = now,
                    )
                    ),
                universities = current.universities + items.associate { it.id to CachedUniversity(it, now) },
            )
        }
    }

    private fun isKept(id: String, cached: CachedUniversity, listed: Set<String>): Boolean =
        id in pinnedIds ||
            id in listed ||
            downloadedCountries.any { it.equals(cached.university.country, ignoreCase = true) }

    private fun <T> observe(transform: (Tables) -> T): Flow<T> = flow {
        readError?.let { throw it }
        emitAll(tables.map { current -> transform(current) })
    }.distinctUntilChanged()

    private fun Tables.search(key: String): CachedSearch? {
        val stored = searches[key] ?: return null
        return CachedSearch(
            items = stored.ids.mapNotNull { id -> universities[id]?.university },
            hasMore = stored.hasMore,
            loadedPages = stored.loadedPages,
            fetchedAt = stored.fetchedAt,
        )
    }

    private fun serveRemoteSearch(params: SearchParams, page: Int): UniversityPage {
        val query = params.query.trim()
        val country = params.country.apiName
        val pageSize = UniversityRepository.PAGE_SIZE
        val matches = remoteUniversities
            .filter { query.isEmpty() || it.name.contains(query, ignoreCase = true) }
            .filter { country == null || it.country.equals(country, ignoreCase = true) }
            .distinctBy { it.id }
        return UniversityPage(
            items = matches.drop(page * pageSize).take(pageSize),
            page = page,
            hasMore = matches.size > (page + 1) * pageSize,
        )
    }

    private fun serveRemoteUniversity(id: String): University =
        remoteUniversities.firstOrNull { it.id == id } ?: throw UniversityNotFoundException(id)

    private fun String.normalizedId(): String = trim().lowercase()
}
