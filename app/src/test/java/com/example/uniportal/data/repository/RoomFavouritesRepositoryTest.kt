package com.example.uniportal.data.repository

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.uniportal.data.local.UniPortalDatabase
import com.example.uniportal.data.mapper.toEntity
import com.example.uniportal.testutil.FakeTimeProvider
import com.example.uniportal.testutil.ROOM_FLOW_TIMEOUT
import com.example.uniportal.testutil.countRows
import com.example.uniportal.testutil.fileDatabase
import com.example.uniportal.testutil.inMemoryDatabase
import com.example.uniportal.testutil.rawLong
import com.example.uniportal.testutil.rawText
import com.example.uniportal.testutil.university
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class RoomFavouritesRepositoryTest {
    private lateinit var database: UniPortalDatabase
    private lateinit var repository: RoomFavouritesRepository
    private val timeProvider = FakeTimeProvider()

    private val kfu = university(
        id = "kpfu.ru",
        name = "Kazan Federal University",
        country = "Russian Federation",
        countryCode = "RU",
        stateProvince = "Tatarstan",
        domains = listOf("kpfu.ru", "ksu.ru"),
        webPages = listOf("http://www.kpfu.ru/", "http://www.ksu.ru/"),
    )
    private val mtsu = university(
        id = "mtsu.edu",
        name = "Middle Tennessee State University",
        stateProvince = null,
        domains = listOf("mtsu.edu"),
        webPages = emptyList(),
    )

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        repository = RoomFavouritesRepository(database.favouritesDao(), timeProvider)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `given favourite, when the same university is added again, then there is still one row, moved to the top`() =
        runTest {
            repository.add(mtsu)
            timeProvider.advanceBy(1_000L)
            repository.add(kfu)
            timeProvider.advanceBy(1_000L)

            repository.add(mtsu)

            assertEquals(2, database.countRows("favourites"))
            assertEquals(listOf(mtsu.id, kfu.id), repository.observeFavourites().first().map { it.id })
            assertEquals(timeProvider.now(), database.rawLong("favourites", "addedAt", "universityId", mtsu.id))
        }

    @Test
    fun `given favourites saved to the database file, when it is closed and opened again, then they are still there`() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            context.deleteDatabase(RESTART_DB_NAME)
            val beforeRestart = fileDatabase(RESTART_DB_NAME)
            try {
                RoomFavouritesRepository(beforeRestart.favouritesDao(), timeProvider).add(kfu)
            } finally {
                beforeRestart.close()
            }

            val afterRestart = fileDatabase(RESTART_DB_NAME)
            try {
                val favourites = RoomFavouritesRepository(afterRestart.favouritesDao(), FakeTimeProvider())
                    .observeFavourites()
                    .first()
                assertEquals(listOf(kfu), favourites)
            } finally {
                afterRestart.close()
                context.deleteDatabase(RESTART_DB_NAME)
            }
        }

    private companion object {
        const val RESTART_DB_NAME = "favourites-restart-test.db"
    }
}
