package com.example.uniportal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.uniportal.data.local.entity.ApplicationEntity
import com.example.uniportal.data.local.entity.UniversityEntity
import com.example.uniportal.data.local.relation.ApplicationStatusRow
import com.example.uniportal.data.local.relation.ApplicationWithUniversity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ApplicationDao {
    @Query(
        "SELECT applications.*, universities.* FROM applications " +
            "INNER JOIN universities ON universities.id = applications.universityId " +
            "ORDER BY applications.updatedAt DESC"
    )
    abstract fun observeAll(): Flow<List<ApplicationWithUniversity>>

    @Query(
        "SELECT applications.*, universities.* FROM applications " +
            "INNER JOIN universities ON universities.id = applications.universityId " +
            "WHERE applications.universityId = :universityId"
    )
    abstract fun observe(universityId: String): Flow<ApplicationWithUniversity?>

    @Query("SELECT universityId, status FROM applications")
    abstract fun observeStatuses(): Flow<List<ApplicationStatusRow>>

    @Query("SELECT * FROM applications WHERE universityId = :universityId")
    abstract suspend fun get(universityId: String): ApplicationEntity?

    @Query(
        "SELECT applications.*, universities.* FROM applications " +
            "INNER JOIN universities ON universities.id = applications.universityId " +
            "WHERE applications.deadlineEpochDay BETWEEN :fromEpochDay AND :toEpochDay"
    )
    abstract suspend fun getWithDeadlineBetween(fromEpochDay: Long, toEpochDay: Long): List<ApplicationWithUniversity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertUniversityIfAbsent(university: UniversityEntity)

    @Upsert
    abstract suspend fun upsert(application: ApplicationEntity)

    @Query("UPDATE applications SET status = :status, updatedAt = :updatedAt WHERE universityId = :universityId")
    abstract suspend fun updateStatus(universityId: String, status: String, updatedAt: Long): Int

    @Query("DELETE FROM applications WHERE universityId = :universityId")
    abstract suspend fun delete(universityId: String)

    @Transaction
    open suspend fun save(university: UniversityEntity, application: ApplicationEntity) {
        insertUniversityIfAbsent(university)
        val existing = get(application.universityId)
        upsert(if (existing == null) application else application.copy(createdAt = existing.createdAt))
    }
}
