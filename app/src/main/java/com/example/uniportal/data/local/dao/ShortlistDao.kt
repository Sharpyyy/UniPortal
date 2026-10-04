package com.example.uniportal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.uniportal.data.local.entity.ShortlistEntity
import com.example.uniportal.data.local.entity.ShortlistItemEntity
import com.example.uniportal.data.local.entity.UniversityEntity
import com.example.uniportal.data.local.relation.ShortlistItemWithUniversity
import com.example.uniportal.data.local.relation.ShortlistWithCount
import com.example.uniportal.data.local.relation.UniversityShortlistCount
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ShortlistDao {
    @Query(
        "SELECT shortlists.*, COUNT(shortlist_items.universityId) AS itemCount FROM shortlists " +
            "LEFT JOIN shortlist_items ON shortlist_items.shortlistId = shortlists.id " +
            "GROUP BY shortlists.id " +
            "ORDER BY shortlists.updatedAt DESC, shortlists.id DESC"
    )
    abstract fun observeAllWithCounts(): Flow<List<ShortlistWithCount>>

    @Query("SELECT * FROM shortlists WHERE id = :id")
    abstract fun observeById(id: Long): Flow<ShortlistEntity?>

    @Query("SELECT * FROM shortlists WHERE id = :id")
    abstract suspend fun getById(id: Long): ShortlistEntity?

    @Query(
        "SELECT universities.*, shortlist_items.addedAt AS addedAt FROM shortlist_items " +
            "INNER JOIN universities ON universities.id = shortlist_items.universityId " +
            "WHERE shortlist_items.shortlistId = :shortlistId " +
            "ORDER BY shortlist_items.addedAt DESC"
    )
    abstract fun observeItems(shortlistId: Long): Flow<List<ShortlistItemWithUniversity>>

    @Query("SELECT shortlistId FROM shortlist_items WHERE universityId = :universityId")
    abstract fun observeMembership(universityId: String): Flow<List<Long>>

    @Query("SELECT shortlistId FROM shortlist_items WHERE universityId = :universityId")
    abstract suspend fun getMembership(universityId: String): List<Long>

    @Query("SELECT universityId, COUNT(*) AS shortlistCount FROM shortlist_items GROUP BY universityId")
    abstract fun observeMembershipCounts(): Flow<List<UniversityShortlistCount>>

    @Query("SELECT name FROM shortlists")
    abstract suspend fun getNames(): List<String>

    @Query("SELECT name FROM shortlists WHERE id != :excludedId")
    abstract suspend fun getNamesExcept(excludedId: Long): List<String>

    @Query("SELECT id FROM shortlists WHERE id IN (:ids)")
    abstract suspend fun getExistingIds(ids: List<Long>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(shortlist: ShortlistEntity): Long

    @Query(
        "UPDATE shortlists SET name = :name, emoji = :emoji, description = :description, " +
            "updatedAt = :updatedAt WHERE id = :id"
    )
    abstract suspend fun update(id: Long, name: String, emoji: String, description: String, updatedAt: Long): Int

    @Query("DELETE FROM shortlists WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Query("UPDATE shortlists SET updatedAt = :updatedAt WHERE id IN (:ids)")
    abstract suspend fun touch(ids: List<Long>, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertUniversityIfAbsent(university: UniversityEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertItem(item: ShortlistItemEntity): Long

    @Query("DELETE FROM shortlist_items WHERE shortlistId = :shortlistId AND universityId = :universityId")
    abstract suspend fun deleteItem(shortlistId: Long, universityId: String): Int

    @Transaction
    open suspend fun addItem(university: UniversityEntity, item: ShortlistItemEntity, now: Long) {
        if (getById(item.shortlistId) == null) return
        insertUniversityIfAbsent(university)
        if (insertItem(item) != -1L) touch(listOf(item.shortlistId), now)
    }

    @Transaction
    open suspend fun removeItem(shortlistId: Long, universityId: String, now: Long) {
        if (deleteItem(shortlistId, universityId) > 0) touch(listOf(shortlistId), now)
    }

    @Transaction
    open suspend fun setMembership(university: UniversityEntity, shortlistIds: Set<Long>, now: Long) {
        insertUniversityIfAbsent(university)
        val current = getMembership(university.id).toSet()
        val wanted = if (shortlistIds.isEmpty()) emptySet() else getExistingIds(shortlistIds.toList()).toSet()
        val toAdd = wanted - current
        val toRemove = current - wanted
        toRemove.forEach { shortlistId -> deleteItem(shortlistId, university.id) }
        toAdd.forEach { shortlistId ->
            insertItem(ShortlistItemEntity(shortlistId = shortlistId, universityId = university.id, addedAt = now))
        }
        val changed = toAdd + toRemove
        if (changed.isNotEmpty()) touch(changed.toList(), now)
    }
}
