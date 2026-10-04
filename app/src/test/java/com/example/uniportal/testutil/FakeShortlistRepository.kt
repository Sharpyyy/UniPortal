package com.example.uniportal.testutil

import com.example.uniportal.data.repository.ShortlistRepository
import com.example.uniportal.domain.model.Shortlist
import com.example.uniportal.domain.model.ShortlistDetails
import com.example.uniportal.domain.model.ShortlistItem
import com.example.uniportal.domain.model.University
import com.example.uniportal.domain.rules.ShortlistError
import com.example.uniportal.domain.rules.ShortlistRules
import com.example.uniportal.domain.rules.ShortlistSaveResult
import com.example.uniportal.domain.rules.ShortlistValidation
import com.example.uniportal.util.TimeProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeShortlistRepository(
    private val timeProvider: TimeProvider = FakeTimeProvider(),
) : ShortlistRepository {
    data class SaveCall(val name: String, val emoji: String, val description: String)
    data class UpdateCall(val id: Long, val name: String, val emoji: String, val description: String)
    data class ItemCall(val shortlistId: Long, val universityId: String)
    data class RestoreCall(val shortlistId: Long, val university: University, val addedAt: Long)
    data class MembershipCall(val university: University, val shortlistIds: Set<Long>)

    private data class StoredList(
        val id: Long,
        val name: String,
        val emoji: String,
        val description: String,
        val createdAt: Long,
        val updatedAt: Long,
    )

    private data class StoredItem(val shortlistId: Long, val university: University, val addedAt: Long)

    private data class Tables(
        val lists: List<StoredList> = emptyList(),

        val items: List<StoredItem> = emptyList(),
    )

    private val tables = MutableStateFlow(Tables())
    private var nextId = 1L

    val createCalls = mutableListOf<SaveCall>()
    val updateCalls = mutableListOf<UpdateCall>()
    val deleteCalls = mutableListOf<Long>()
    val addCalls = mutableListOf<ItemCall>()
    val removeCalls = mutableListOf<ItemCall>()
    val restoreCalls = mutableListOf<RestoreCall>()
    val setMembershipCalls = mutableListOf<MembershipCall>()

    var observeCalls = 0
        private set

    var readError: Throwable? = null
    var writeError: Throwable? = null
    var writeGate: CompletableDeferred<Unit>? = null

    val activeObservers: Int
        get() = tables.subscriptionCount.value

    val shortlists: List<Shortlist>
        get() = tables.value.shortlists()

    fun details(id: Long): ShortlistDetails? = tables.value.details(id)

    fun membership(universityId: String): Set<Long> = tables.value.membership(universityId)

    fun seed(
        name: String,
        emoji: String = ShortlistRules.DEFAULT_EMOJI,
        description: String = "",
        universities: List<University> = emptyList(),
        createdAt: Long = timeProvider.now(),
    ): Long {
        val id = nextId++
        tables.update { current ->
            current.copy(
                lists = current.lists + StoredList(id, name, emoji, description, createdAt, createdAt),
                items = universities.distinctBy { it.id }.reversed()
                    .map { StoredItem(shortlistId = id, university = it, addedAt = createdAt) } + current.items,
            )
        }
        return id
    }

    override fun observeShortlists(): Flow<List<Shortlist>> = observe { it.shortlists() }

    override fun observeShortlist(id: Long): Flow<ShortlistDetails?> = observe { it.details(id) }

    override fun observeMembership(universityId: String): Flow<Set<Long>> = observe { it.membership(universityId) }

    override fun observeMembershipCounts(): Flow<Map<String, Int>> = observe { current ->
        current.items.groupingBy { it.university.id }.eachCount()
    }

    override suspend fun create(name: String, emoji: String, description: String): ShortlistSaveResult {
        createCalls += SaveCall(name = name, emoji = emoji, description = description)
        beforeWrite()
        val names = tables.value.lists.map { it.name }
        return when (val validation = ShortlistRules.validate(name, description, names)) {
            is ShortlistValidation.Invalid -> ShortlistSaveResult.Invalid(validation.error)
            is ShortlistValidation.Valid -> {
                val now = timeProvider.now()
                val id = nextId++
                val list = StoredList(
                    id = id,
                    name = validation.name,
                    emoji = ShortlistRules.normalizeEmoji(emoji),
                    description = validation.description,
                    createdAt = now,
                    updatedAt = now,
                )
                tables.update { current -> current.copy(lists = current.lists + list) }
                ShortlistSaveResult.Saved(id)
            }
        }
    }

    override suspend fun update(id: Long, name: String, emoji: String, description: String): ShortlistSaveResult {
        updateCalls += UpdateCall(id = id, name = name, emoji = emoji, description = description)
        beforeWrite()
        val current = tables.value
        if (current.lists.none { it.id == id }) return ShortlistSaveResult.Invalid(ShortlistError.NOT_FOUND)
        val otherNames = current.lists.filter { it.id != id }.map { it.name }
        return when (val validation = ShortlistRules.validate(name, description, otherNames)) {
            is ShortlistValidation.Invalid -> ShortlistSaveResult.Invalid(validation.error)
            is ShortlistValidation.Valid -> {
                val now = timeProvider.now()
                tables.update { tablesNow ->
                    tablesNow.copy(
                        lists = tablesNow.lists.map { list ->
                            if (list.id != id) {
                                list
                            } else {
                                list.copy(
                                    name = validation.name,
                                    emoji = ShortlistRules.normalizeEmoji(emoji),
                                    description = validation.description,
                                    updatedAt = now,
                                )
                            }
                        },
                    )
                }
                ShortlistSaveResult.Saved(id)
            }
        }
    }

    override suspend fun delete(id: Long) {
        deleteCalls += id
        beforeWrite()
        tables.update { current ->
            current.copy(
                lists = current.lists.filterNot { it.id == id },
                items = current.items.filterNot { it.shortlistId == id },
            )
        }
    }

    override suspend fun add(shortlistId: Long, university: University) {
        addCalls += ItemCall(shortlistId = shortlistId, universityId = university.id)
        beforeWrite()
        insertItem(shortlistId, university, addedAt = timeProvider.now())
    }

    override suspend fun restore(shortlistId: Long, university: University, addedAt: Long) {
        restoreCalls += RestoreCall(shortlistId = shortlistId, university = university, addedAt = addedAt)
        beforeWrite()
        insertItem(shortlistId, university, addedAt = addedAt)
    }

    override suspend fun remove(shortlistId: Long, universityId: String) {
        removeCalls += ItemCall(shortlistId = shortlistId, universityId = universityId)
        beforeWrite()
        val now = timeProvider.now()
        tables.update { current ->
            val remaining = current.items.filterNot { it.shortlistId == shortlistId && it.university.id == universityId }
            if (remaining.size == current.items.size) {
                current
            } else {
                current.copy(lists = current.lists.touched(setOf(shortlistId), now), items = remaining)
            }
        }
    }

    override suspend fun setMembership(university: University, shortlistIds: Set<Long>) {
        setMembershipCalls += MembershipCall(university = university, shortlistIds = shortlistIds)
        beforeWrite()
        val now = timeProvider.now()
        tables.update { current ->
            val existing = current.lists.map { it.id }.toSet()
            val wanted = shortlistIds.filter { it in existing }.toSet()
            val present = current.membership(university.id)
            val toAdd = wanted - present
            val toRemove = present - wanted
            val kept = current.items.filterNot { it.university.id == university.id && it.shortlistId in toRemove }
            val added = toAdd.map { StoredItem(shortlistId = it, university = university, addedAt = now) }
            current.copy(lists = current.lists.touched(toAdd + toRemove, now), items = added + kept)
        }
    }

    private fun insertItem(shortlistId: Long, university: University, addedAt: Long) {
        val now = timeProvider.now()
        tables.update { current ->
            val listExists = current.lists.any { it.id == shortlistId }
            val alreadyIn = current.items.any { it.shortlistId == shortlistId && it.university.id == university.id }
            if (!listExists || alreadyIn) {
                current
            } else {
                current.copy(
                    lists = current.lists.touched(setOf(shortlistId), now),
                    items = listOf(StoredItem(shortlistId, university, addedAt)) + current.items,
                )
            }
        }
    }

    private suspend fun beforeWrite() {
        val currentGate = writeGate
        currentGate?.await()
        writeError?.let { throw it }
    }

    private fun <T> observe(transform: (Tables) -> T): Flow<T> = flow {
        observeCalls++
        readError?.let { throw it }
        emitAll(tables.map { current -> transform(current) })
    }.distinctUntilChanged()

    private fun List<StoredList>.touched(ids: Set<Long>, now: Long): List<StoredList> =
        map { list -> if (list.id in ids) list.copy(updatedAt = now) else list }

    private fun Tables.itemCount(id: Long): Int = items.count { it.shortlistId == id }

    private fun Tables.shortlists(): List<Shortlist> =
        lists
            .sortedWith(compareByDescending<StoredList> { it.updatedAt }.thenByDescending { it.id })
            .map { it.toShortlist(itemCount(it.id)) }

    private fun Tables.details(id: Long): ShortlistDetails? {
        val list = lists.firstOrNull { it.id == id } ?: return null
        val shortlistItems = items
            .filter { it.shortlistId == id }
            .sortedByDescending { it.addedAt }
            .map { ShortlistItem(university = it.university, addedAt = it.addedAt) }
        return ShortlistDetails(shortlist = list.toShortlist(shortlistItems.size), items = shortlistItems)
    }

    private fun Tables.membership(universityId: String): Set<Long> =
        items.filter { it.university.id == universityId }.map { it.shortlistId }.toSet()

    private fun StoredList.toShortlist(itemCount: Int): Shortlist = Shortlist(
        id = id,
        name = name,
        emoji = emoji,
        description = description,
        itemCount = itemCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
