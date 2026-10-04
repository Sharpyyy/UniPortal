package com.example.uniportal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.uniportal.data.local.entity.FavouriteEntity
import com.example.uniportal.data.local.entity.UniversityEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FavouritesDao {
    @Query(
        "SELECT universities.* FROM favourites " +
            "INNER JOIN universities ON universities.id = favourites.universityId " +
            "ORDER BY favourites.addedAt DESC"
    )
    abstract fun observeAll(): Flow<List<UniversityEntity>>

    @Query("SELECT universityId FROM favourites")
    abstract fun observeIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favourites WHERE universityId = :id)")
    abstract fun observeIsFavourite(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertUniversityIfAbsent(university: UniversityEntity)

    @Upsert
    abstract suspend fun upsert(favourite: FavouriteEntity)

    @Query("DELETE FROM favourites WHERE universityId = :id")
    abstract suspend fun deleteById(id: String)

    @Transaction
    open suspend fun add(university: UniversityEntity, favourite: FavouriteEntity) {
        insertUniversityIfAbsent(university)
        upsert(favourite)
    }
}
