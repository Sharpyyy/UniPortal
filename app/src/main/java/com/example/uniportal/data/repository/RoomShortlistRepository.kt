package com.example.uniportal.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.example.uniportal.data.local.UniPortalDatabase
import com.example.uniportal.data.local.dao.ShortlistDao
import com.example.uniportal.data.local.entity.ShortlistEntity
import com.example.uniportal.data.local.entity.ShortlistItemEntity
import com.example.uniportal.data.mapper.toDomain
import com.example.uniportal.data.mapper.toEntity
import com.example.uniportal.domain.model.Shortlist
import com.example.uniportal.domain.model.ShortlistDetails
import com.example.uniportal.domain.model.University
import com.example.uniportal.domain.rules.ShortlistError
import com.example.uniportal.domain.rules.ShortlistRules
import com.example.uniportal.domain.rules.ShortlistSaveResult
import com.example.uniportal.domain.rules.ShortlistValidation
import com.example.uniportal.util.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class RoomShortlistRepository @Inject constructor(
    private val database: UniPortalDatabase,
    private val dao: ShortlistDao,
    private val timeProvider: TimeProvider,
) : ShortlistRepository {
    override fun observeShortlists(): Flow<List<Shortlist>> =
        dao.observeAllWithCounts()
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()

    override fun observeShortlist(id: Long): Flow<ShortlistDetails?> =
        combine(dao.observeById(id), dao.observeItems(id)) { shortlist, rows ->
            shortlist?.let { entity ->
                val items = rows.map { it.toDomain() }.distinctBy { it.university.id }
                ShortlistDetails(shortlist = entity.toDomain(itemCount = items.size), items = items)
            }
        }.distinctUntilChanged()

    override fun observeMembership(universityId: String): Flow<Set<Long>> =
        dao.observeMembership(universityId).map { ids -> ids.toSet() }.distinctUntilChanged()

    override fun observeMembershipCounts(): Flow<Map<String, Int>> =
        dao.observeMembershipCounts()
            .map { rows -> rows.associate { it.universityId to it.shortlistCount } }
            .distinctUntilChanged()

    override suspend fun create(name: String, emoji: String, description: String): ShortlistSaveResult =
        saveValidated {
            when (val validation = ShortlistRules.validate(name, description, dao.getNames())) {
                is ShortlistValidation.Invalid -> ShortlistSaveResult.Invalid(validation.error)
                is ShortlistValidation.Valid -> {
                    val now = timeProvider.now()
                    val id = dao.insert(
                        ShortlistEntity(
                            name = validation.name,
                            emoji = ShortlistRules.normalizeEmoji(emoji),
                            description = validation.description,
                            createdAt = now,
                            updatedAt = now,
                        ),
                    )
                    ShortlistSaveResult.Saved(id)
                }
            }
        }

    override suspend fun update(id: Long, name: String, emoji: String, description: String): ShortlistSaveResult =
        saveValidated {
            if (dao.getById(id) == null) {
                ShortlistSaveResult.Invalid(ShortlistError.NOT_FOUND)
            } else {
                when (val validation = ShortlistRules.validate(name, description, dao.getNamesExcept(id))) {
                    is ShortlistValidation.Invalid -> ShortlistSaveResult.Invalid(validation.error)
                    is ShortlistValidation.Valid -> {
                        dao.update(
                            id = id,
                            name = validation.name,
                            emoji = ShortlistRules.normalizeEmoji(emoji),
                            description = validation.description,
                            updatedAt = timeProvider.now(),
                        )
                        ShortlistSaveResult.Saved(id)
                    }
                }
            }
        }

    override suspend fun delete(id: Long) {
        dao.delete(id)
    }

    override suspend fun add(shortlistId: Long, university: University) {
        val now = timeProvider.now()
        dao.addItem(
            university = university.toEntity(fetchedAt = now),
            item = ShortlistItemEntity(shortlistId = shortlistId, universityId = university.id, addedAt = now),
            now = now,
        )
    }

    override suspend fun restore(shortlistId: Long, university: University, addedAt: Long) {
        val now = timeProvider.now()
        dao.addItem(
            university = university.toEntity(fetchedAt = now),
            item = ShortlistItemEntity(shortlistId = shortlistId, universityId = university.id, addedAt = addedAt),
            now = now,
        )
    }

    override suspend fun remove(shortlistId: Long, universityId: String) {
        dao.removeItem(shortlistId = shortlistId, universityId = universityId, now = timeProvider.now())
    }

    override suspend fun setMembership(university: University, shortlistIds: Set<Long>) {
        val now = timeProvider.now()
        dao.setMembership(university = university.toEntity(fetchedAt = now), shortlistIds = shortlistIds, now = now)
    }

    private suspend fun saveValidated(block: suspend () -> ShortlistSaveResult): ShortlistSaveResult =
        try {
            database.withTransaction { block() }
        } catch (e: SQLiteConstraintException) {
            ShortlistSaveResult.Invalid(ShortlistError.NAME_DUPLICATE)
        }
}
