package com.example.uniportal.data.repository

import com.example.uniportal.data.local.dao.HistoryDao
import com.example.uniportal.data.local.entity.HistoryEntity
import com.example.uniportal.data.mapper.toDomain
import com.example.uniportal.data.mapper.toEntity
import com.example.uniportal.domain.model.HistoryEntry
import com.example.uniportal.domain.model.University
import com.example.uniportal.util.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class RoomHistoryRepository @Inject constructor(
    private val dao: HistoryDao,
    private val timeProvider: TimeProvider,
) : HistoryRepository {
    override fun observeHistory(): Flow<List<HistoryEntry>> =
        dao.observeAll()
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()

    override suspend fun recordView(university: University, limit: Int) {
        val now = timeProvider.now()
        dao.record(
            university = university.toEntity(fetchedAt = now),
            entry = HistoryEntity(universityId = university.id, viewedAt = now),
            limit = limit.coerceAtLeast(1),
        )
    }

    override suspend fun restore(entry: HistoryEntry) {
        dao.restore(
            university = entry.university.toEntity(fetchedAt = timeProvider.now()),
            entry = HistoryEntity(universityId = entry.university.id, viewedAt = entry.viewedAt),
        )
    }

    override suspend fun remove(universityId: String) {
        dao.deleteById(universityId)
    }

    override suspend fun clear() {
        dao.clear()
    }

    override suspend fun trimTo(limit: Int) {
        dao.trimTo(limit.coerceAtLeast(0))
    }
}
