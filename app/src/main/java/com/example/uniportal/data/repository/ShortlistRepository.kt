package com.example.uniportal.data.repository

import com.example.uniportal.domain.model.Shortlist
import com.example.uniportal.domain.model.ShortlistDetails
import com.example.uniportal.domain.model.University
import com.example.uniportal.domain.rules.ShortlistSaveResult
import kotlinx.coroutines.flow.Flow

interface ShortlistRepository {
    fun observeShortlists(): Flow<List<Shortlist>>

    fun observeShortlist(id: Long): Flow<ShortlistDetails?>

    fun observeMembership(universityId: String): Flow<Set<Long>>

    fun observeMembershipCounts(): Flow<Map<String, Int>>

    suspend fun create(name: String, emoji: String, description: String): ShortlistSaveResult

    suspend fun update(id: Long, name: String, emoji: String, description: String): ShortlistSaveResult

    suspend fun delete(id: Long)

    suspend fun add(shortlistId: Long, university: University)

    suspend fun restore(shortlistId: Long, university: University, addedAt: Long)

    suspend fun remove(shortlistId: Long, universityId: String)

    suspend fun setMembership(university: University, shortlistIds: Set<Long>)
}
