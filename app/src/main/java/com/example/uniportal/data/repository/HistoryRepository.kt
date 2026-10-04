package com.example.uniportal.data.repository

import com.example.uniportal.domain.model.HistoryEntry
import com.example.uniportal.domain.model.University
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun observeHistory(): Flow<List<HistoryEntry>>

    suspend fun recordView(university: University, limit: Int)

    suspend fun restore(entry: HistoryEntry)

    suspend fun remove(universityId: String)

    suspend fun clear()

    suspend fun trimTo(limit: Int)
}
