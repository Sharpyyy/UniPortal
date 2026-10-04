package com.example.uniportal.testutil

import com.example.uniportal.data.remote.UniversitiesApi
import com.example.uniportal.data.remote.dto.UniversityDto
import kotlinx.coroutines.CompletableDeferred

class FakeUniversitiesApi(
    var universities: List<UniversityDto> = emptyList(),
) : UniversitiesApi {
    data class SearchCall(val name: String?, val country: String?, val limit: Int, val offset: Int)

    val searchCalls = mutableListOf<SearchCall>()
    val findByDomainCalls = mutableListOf<String>()

    var searchHandler: suspend (SearchCall) -> List<UniversityDto> = { call -> serveSearch(call) }
    var findByDomainHandler: suspend (String) -> List<UniversityDto> = { domain -> serveFindByDomain(domain) }

    var gate: CompletableDeferred<Unit>? = null

    private val queuedSearchResults = ArrayDeque<Result<List<UniversityDto>>>()
    private val queuedFindByDomainResults = ArrayDeque<Result<List<UniversityDto>>>()

    fun enqueueSearch(items: List<UniversityDto>) {
        queuedSearchResults.addLast(Result.success(items))
    }

    fun enqueueSearchError(error: Throwable) {
        queuedSearchResults.addLast(Result.failure(error))
    }

    fun enqueueFindByDomain(items: List<UniversityDto>) {
        queuedFindByDomainResults.addLast(Result.success(items))
    }

    fun enqueueFindByDomainError(error: Throwable) {
        queuedFindByDomainResults.addLast(Result.failure(error))
    }

    override suspend fun search(
        name: String?,
        country: String?,
        limit: Int,
        offset: Int,
    ): List<UniversityDto> {
        val call = SearchCall(name = name, country = country, limit = limit, offset = offset)
        searchCalls += call
        val queued = queuedSearchResults.removeFirstOrNull()
        val currentGate = gate
        currentGate?.await()
        return if (queued != null) queued.getOrThrow() else searchHandler(call)
    }

    override suspend fun findByDomain(domain: String): List<UniversityDto> {
        findByDomainCalls += domain
        val queued = queuedFindByDomainResults.removeFirstOrNull()
        val currentGate = gate
        currentGate?.await()
        return if (queued != null) queued.getOrThrow() else findByDomainHandler(domain)
    }

    fun serveSearch(call: SearchCall): List<UniversityDto> {
        val name = call.name
        val country = call.country
        return universities
            .filter { dto -> name == null || dto.name.orEmpty().contains(name, ignoreCase = true) }
            .filter { dto -> country == null || dto.country.equals(country, ignoreCase = true) }
            .drop(call.offset)
            .take(call.limit)
    }

    fun serveFindByDomain(domain: String): List<UniversityDto> =
        universities.filter { dto ->
            dto.domains.orEmpty().any { it.trim().equals(domain, ignoreCase = true) }
        }
}
