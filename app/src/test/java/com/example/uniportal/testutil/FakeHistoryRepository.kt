package com.example.uniportal.testutil

import com.example.uniportal.data.repository.HistoryRepository
import com.example.uniportal.domain.model.HistoryEntry
import com.example.uniportal.domain.model.University
import com.example.uniportal.util.TimeProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

class FakeHistoryRepository(
    initial: List<HistoryEntry> = emptyList(),
    private val timeProvider: TimeProvider = FakeTimeProvider(),
) : HistoryRepository {
    data class RecordViewCall(val university: University, val limit: Int)

    private val stored = MutableStateFlow(normalized(initial))

    val entries: List<HistoryEntry>
        get() = stored.value

    val activeObservers: Int
        get() = stored.subscriptionCount.value

    var observeCalls = 0
        private set
    val recordViewCalls = mutableListOf<RecordViewCall>()
    val removeCalls = mutableListOf<String>()
    val restoreCalls = mutableListOf<HistoryEntry>()
    val trimCalls = mutableListOf<Int>()
    var clearCalls = 0
        private set

    val recordedViews: List<University>
        get() = recordViewCalls.map { it.university }

    var readGate: CompletableDeferred<Unit>? = null
    var readError: Throwable? = null
    var writeError: Throwable? = null

    override fun observeHistory(): Flow<List<HistoryEntry>> = flow {
        observeCalls++
        val currentGate = readGate
        currentGate?.await()
        readError?.let { throw it }
        emitAll(stored)
    }

    override suspend fun recordView(university: University, limit: Int) {
        recordViewCalls += RecordViewCall(university = university, limit = limit)
        writeError?.let { throw it }
        val entry = HistoryEntry(university = university, viewedAt = timeProvider.now())
        stored.update { list -> put(list, entry).take(limit.coerceAtLeast(1)) }
    }

    override suspend fun restore(entry: HistoryEntry) {
        restoreCalls += entry
        writeError?.let { throw it }
        stored.update { list -> put(list, entry) }
    }

    override suspend fun remove(universityId: String) {
        removeCalls += universityId
        writeError?.let { throw it }
        stored.update { list -> list.filterNot { it.university.id == universityId } }
    }

    override suspend fun clear() {
        clearCalls++
        writeError?.let { throw it }
        stored.value = emptyList()
    }

    override suspend fun trimTo(limit: Int) {
        trimCalls += limit
        writeError?.let { throw it }
        stored.update { list -> list.take(limit.coerceAtLeast(0)) }
    }

    fun replaceAll(entries: List<HistoryEntry>) {
        stored.value = normalized(entries)
    }

    private fun put(list: List<HistoryEntry>, entry: HistoryEntry): List<HistoryEntry> =
        normalized(listOf(entry) + list.filterNot { it.university.id == entry.university.id })

    private fun normalized(list: List<HistoryEntry>): List<HistoryEntry> =
        list.sortedByDescending { it.viewedAt }.distinctBy { it.university.id }
}
