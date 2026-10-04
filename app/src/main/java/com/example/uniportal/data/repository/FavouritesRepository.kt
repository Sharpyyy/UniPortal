package com.example.uniportal.data.repository

import com.example.uniportal.domain.model.University
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface FavouritesRepository {
    fun observeFavourites(): Flow<List<University>>

    fun observeFavouriteIds(): Flow<Set<String>>

    fun observeIsFavourite(id: String): Flow<Boolean>

    suspend fun add(university: University)

    suspend fun remove(id: String)
}

suspend fun FavouritesRepository.toggle(university: University) {
    if (observeIsFavourite(university.id).first()) remove(university.id) else add(university)
}
