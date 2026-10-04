package com.example.uniportal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.uniportal.data.local.entity.HistoryEntity
import com.example.uniportal.data.local.entity.UniversityEntity
import com.example.uniportal.data.local.relation.HistoryWithUniversity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class HistoryDao {
    @Query(
        "SELECT universities.*, history.viewedAt AS viewedAt FROM history " +
            "INNER JOIN universities ON universities.id = history.universityId " +
            "ORDER BY history.viewedAt DESC"
    )
    abstract fun observeAll(): Flow<List<HistoryWithUniversity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertUniversityIfAbsent(university: UniversityEntity)

    @Upsert
    abstract suspend fun upsert(entry: HistoryEntity)

    @Query("DELETE FROM history WHERE universityId = :universityId")
    abstract suspend fun deleteById(universityId: String)

    @Query("DELETE FROM history")
    abstract suspend fun clear()

    @Query(
        "DELETE FROM history WHERE universityId NOT IN " +
            "(SELECT universityId FROM history ORDER BY viewedAt DESC LIMIT :limit)"
    )
    abstract suspend fun trimTo(limit: Int)

    @Transaction
    open suspend fun record(university: UniversityEntity, entry: HistoryEntity, limit: Int) {
        insertUniversityIfAbsent(university)
        upsert(entry)
        trimTo(limit)
    }

    @Transaction
    open suspend fun restore(university: UniversityEntity, entry: HistoryEntity) {
        insertUniversityIfAbsent(university)
        upsert(entry)
    }
}
