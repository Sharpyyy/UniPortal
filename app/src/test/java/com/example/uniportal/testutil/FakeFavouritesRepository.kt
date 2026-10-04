package com.example.uniportal.testutil

import com.example.uniportal.data.repository.FavouritesRepository
import com.example.uniportal.domain.model.University
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeFavouritesRepository(
    initial: List<University> = emptyList(),
) : FavouritesRepository {
    private val stored = MutableStateFlow(initial.distinctBy { it.id })

    val favourites: List<University>
        get() = stored.value

    val activeObservers: Int
        get() = stored.subscriptionCount.value

    var observeCalls = 0
        private set
    val addCalls = mutableListOf<University>()
    val removeCalls = mutableListOf<String>()

    var readError: Throwable? = null
    var writeError: Throwable? = null
    val failingWriteIds = mutableSetOf<String>()

    override fun observeFavourites(): Flow<List<University>> = observe { list -> list }

    override fun observeFavouriteIds(): Flow<Set<String>> =
        observe { list -> list.map { it.id }.toSet() }.distinctUntilChanged()

    override fun observeIsFavourite(id: String): Flow<Boolean> =
        observe { list -> list.any { it.id == id } }.distinctUntilChanged()

    override suspend fun add(university: University) {
        addCalls += university
        failIfRequested(university.id)
        stored.update { list -> listOf(university) + list.filterNot { it.id == university.id } }
    }

    override suspend fun remove(id: String) {
        removeCalls += id
        failIfRequested(id)
        stored.update { list -> list.filterNot { it.id == id } }
    }

    fun replaceAll(favourites: List<University>) {
        stored.value = favourites.distinctBy { it.id }
    }

    private fun <T> observe(transform: (List<University>) -> T): Flow<T> = flow {
        observeCalls++
        readError?.let { throw it }
        emitAll(stored.map { list -> transform(list) })
    }

    private fun failIfRequested(id: String) {
        writeError?.let { throw it }
        if (id in failingWriteIds) throw IOException("Write of $id failed")
    }
}
