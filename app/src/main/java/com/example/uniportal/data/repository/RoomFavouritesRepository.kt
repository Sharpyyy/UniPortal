package com.example.uniportal.data.repository

import com.example.uniportal.data.local.dao.FavouritesDao
import com.example.uniportal.data.local.entity.FavouriteEntity
import com.example.uniportal.data.mapper.toDomain
import com.example.uniportal.data.mapper.toEntity
import com.example.uniportal.domain.model.University
import com.example.uniportal.util.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class RoomFavouritesRepository @Inject constructor(
    private val dao: FavouritesDao,
    private val timeProvider: TimeProvider,
) : FavouritesRepository {
    override fun observeFavourites(): Flow<List<University>> =
        dao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .distinctUntilChanged()

    override fun observeFavouriteIds(): Flow<Set<String>> =
        dao.observeIds().map { ids -> ids.toSet() }.distinctUntilChanged()

    override fun observeIsFavourite(id: String): Flow<Boolean> =
        dao.observeIsFavourite(id).distinctUntilChanged()

    override suspend fun add(university: University) {
        val now = timeProvider.now()
        dao.add(
            university = university.toEntity(fetchedAt = now),
            favourite = FavouriteEntity(universityId = university.id, addedAt = now),
        )
    }

    override suspend fun remove(id: String) {
        dao.deleteById(id)
    }
}
