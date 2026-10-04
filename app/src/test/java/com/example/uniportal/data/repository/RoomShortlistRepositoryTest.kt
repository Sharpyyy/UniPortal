package com.example.uniportal.data.repository

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.uniportal.data.local.UniPortalDatabase
import com.example.uniportal.data.mapper.toEntity
import com.example.uniportal.domain.model.Shortlist
import com.example.uniportal.domain.model.ShortlistItem
import com.example.uniportal.domain.rules.ShortlistError
import com.example.uniportal.domain.rules.ShortlistRules
import com.example.uniportal.domain.rules.ShortlistSaveResult
import com.example.uniportal.testutil.FakeTimeProvider
import com.example.uniportal.testutil.ROOM_FLOW_TIMEOUT
import com.example.uniportal.testutil.countRows
import com.example.uniportal.testutil.inMemoryDatabase
import com.example.uniportal.testutil.university
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class RoomShortlistRepositoryTest {
    private lateinit var database: UniPortalDatabase
    private lateinit var repository: RoomShortlistRepository
    private val timeProvider = FakeTimeProvider()
    private val t0 = FakeTimeProvider.START_TIME_MILLIS

    private val mtsu = university(id = "mtsu.edu", name = "Middle Tennessee State University")
    private val kfu = university(
        id = "kpfu.ru",
        name = "Kazan Federal University",
        country = "Russian Federation",
        countryCode = "RU",
        domains = listOf("kpfu.ru", "ksu.ru"),
    )

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        repository = RoomShortlistRepository(database, database.shortlistDao(), timeProvider)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun createList(name: String, emoji: String = ShortlistRules.DEFAULT_EMOJI, description: String = ""): Long {
        val result = repository.create(name = name, emoji = emoji, description = description)
        return (result as? ShortlistSaveResult.Saved)?.id ?: throw AssertionError("'$name' was not created: $result")
    }

    @Test
    fun `given a university in a list, when it is added again, then there is still one item with the first time of adding`() =
        runTest {
            val dream = createList("Мечта")
            timeProvider.advanceBy(1_000L)
            repository.add(dream, mtsu)
            timeProvider.advanceBy(1_000L)
            repository.add(dream, mtsu)

            val details = repository.observeShortlist(dream).first()
            assertEquals(listOf(ShortlistItem(university = mtsu, addedAt = t0 + 1_000L)), details?.items)
            assertEquals(1, details?.shortlist?.itemCount)
            assertEquals("the repeated add changed nothing", t0 + 1_000L, details?.shortlist?.updatedAt)
            assertEquals(1, database.countRows("shortlist_items"))
            assertEquals("the university went to the catalogue once", 1, database.countRows("universities"))
        }
}
