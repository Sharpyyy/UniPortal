package com.example.uniportal.data.repository

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.uniportal.data.local.UniPortalDatabase
import com.example.uniportal.data.mapper.toEntity
import com.example.uniportal.domain.model.AppSettings
import com.example.uniportal.domain.model.HistoryEntry
import com.example.uniportal.domain.model.University
import com.example.uniportal.testutil.FakeTimeProvider
import com.example.uniportal.testutil.ROOM_FLOW_TIMEOUT
import com.example.uniportal.testutil.countRows
import com.example.uniportal.testutil.historyEntry
import com.example.uniportal.testutil.inMemoryDatabase
import com.example.uniportal.testutil.universities
import com.example.uniportal.testutil.university
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class RoomHistoryRepositoryTest {
    private lateinit var database: UniPortalDatabase
    private lateinit var repository: RoomHistoryRepository
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
    private val mtsu = university(id = "mtsu.edu", name = "Middle Tennessee State University")
    private val tokyo = university(
        id = "u-tokyo.ac.jp",
        name = "University of Tokyo",
        country = "Japan",
        countryCode = "JP",
    )

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        repository = RoomHistoryRepository(database.historyDao(), timeProvider)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun view(university: University, limit: Int = LIMIT) {
        repository.recordView(university, limit)
        timeProvider.advanceBy(ONE_SECOND)
    }

    @Test
    fun `given university viewed again later, when history is read, then it is listed once and on top`() = runTest {
        view(mtsu)
        view(kfu)
        view(tokyo)
        val secondViewTime = timeProvider.now()

        view(mtsu)

        val history = repository.observeHistory().first()
        assertEquals(listOf(mtsu.id, tokyo.id, kfu.id), history.map { it.university.id })
        assertEquals(secondViewTime, history.first().viewedAt)
        assertEquals(3, database.countRows("history"))
    }

    private companion object {
        const val ONE_SECOND = 1_000L
        const val LIMIT = AppSettings.DEFAULT_HISTORY_LIMIT
        const val SMALL_LIMIT = 20
    }
}
