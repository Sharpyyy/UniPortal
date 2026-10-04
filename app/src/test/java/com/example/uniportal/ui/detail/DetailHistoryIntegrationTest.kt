package com.example.uniportal.ui.detail

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.example.uniportal.data.local.UniPortalDatabase
import com.example.uniportal.data.repository.DefaultUniversityRepository
import com.example.uniportal.data.repository.RoomApplicationRepository
import com.example.uniportal.data.repository.RoomFavouritesRepository
import com.example.uniportal.data.repository.RoomHistoryRepository
import com.example.uniportal.data.repository.RoomShortlistRepository
import com.example.uniportal.domain.model.ErrorType
import com.example.uniportal.domain.model.HistoryEntry
import com.example.uniportal.testutil.FakeConnectivityObserver
import com.example.uniportal.testutil.FakeSettingsRepository
import com.example.uniportal.testutil.FakeTimeProvider
import com.example.uniportal.testutil.FakeUniversitiesApi
import com.example.uniportal.testutil.MainDispatcherRule
import com.example.uniportal.testutil.NotifyingHistoryRepository
import com.example.uniportal.testutil.ROOM_FLOW_TIMEOUT
import com.example.uniportal.testutil.countRows
import com.example.uniportal.testutil.inMemoryDatabase
import com.example.uniportal.testutil.rawLong
import com.example.uniportal.testutil.university
import com.example.uniportal.testutil.universityDto
import com.example.uniportal.ui.navigation.Routes
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class DetailHistoryIntegrationTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: UniPortalDatabase
    private lateinit var universityRepository: DefaultUniversityRepository
    private lateinit var historyRepository: NotifyingHistoryRepository
    private lateinit var favouritesRepository: RoomFavouritesRepository
    private lateinit var shortlistRepository: RoomShortlistRepository
    private lateinit var applicationRepository: RoomApplicationRepository

    private val timeProvider = FakeTimeProvider()
    private val settingsRepository = FakeSettingsRepository()
    private val connectivity = FakeConnectivityObserver()

    private val api = FakeUniversitiesApi(
        universities = listOf(
            universityDto(
                name = "Kazan Federal University",
                country = "Russian Federation",
                alphaTwoCode = "RU",
                stateProvince = "Tatarstan",
                domains = listOf("kpfu.ru", "ksu.ru"),
                webPages = listOf("http://www.kpfu.ru/"),
            ),
            universityDto(
                name = "Middle Tennessee State University",
                country = "United States",
                alphaTwoCode = "US",
                stateProvince = null,
                domains = listOf("mtsu.edu"),
                webPages = listOf("http://www.mtsu.edu/"),
            ),
        ),
    )

    private val kfu = university(
        id = "kpfu.ru",
        name = "Kazan Federal University",
        country = "Russian Federation",
        countryCode = "RU",
        stateProvince = "Tatarstan",
        domains = listOf("kpfu.ru", "ksu.ru"),
        webPages = listOf("http://www.kpfu.ru/"),
    )
    private val mtsu = university(
        id = "mtsu.edu",
        name = "Middle Tennessee State University",
        country = "United States",
        countryCode = "US",
        stateProvince = null,
        domains = listOf("mtsu.edu"),
        webPages = listOf("http://www.mtsu.edu/"),
    )

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        universityRepository = DefaultUniversityRepository(api, database.universityDao(), timeProvider)
        historyRepository = NotifyingHistoryRepository(RoomHistoryRepository(database.historyDao(), timeProvider))
        favouritesRepository = RoomFavouritesRepository(database.favouritesDao(), timeProvider)
        shortlistRepository = RoomShortlistRepository(database, database.shortlistDao(), timeProvider)
        applicationRepository = RoomApplicationRepository(database.applicationDao(), timeProvider)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun openDetail(id: String) = UniversityDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf(Routes.ARG_ID to id)),
        universityRepository = universityRepository,
        historyRepository = historyRepository,
        favouritesRepository = favouritesRepository,
        shortlistRepository = shortlistRepository,
        applicationRepository = applicationRepository,
        settingsRepository = settingsRepository,
        connectivityObserver = connectivity,
        timeProvider = timeProvider,
    )

    private suspend fun <T> ReceiveTurbine<T>.awaitItemMatching(predicate: (T) -> Boolean): T {
        while (true) {
            val item = awaitItem()
            if (predicate(item)) return item
        }
    }

    @Test
    fun `given university not on the device, when detail loads, then it is cached and the view is stored in history`() =
        runTest {
            val viewModel = openDetail(kfu.id)

            viewModel.uiState.map { it.content }.distinctUntilChanged().test(timeout = ROOM_FLOW_TIMEOUT) {
                assertEquals(DetailContent.Loading, awaitItem())
                assertEquals(DetailContent.Success(kfu), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            val recorded = historyRepository.awaitRecordedView()

            assertEquals(kfu, recorded)
            assertEquals(listOf(kfu.id), api.findByDomainCalls)
            assertEquals(
                "cached for offline use with the fetch time",
                FakeTimeProvider.START_TIME_MILLIS,
                database.rawLong("universities", "fetchedAt", "id", kfu.id),
            )
            val rows = database.historyDao().observeAll().first()
            assertEquals(1, rows.size)
            assertEquals(kfu.id, rows.single().university.id)
            assertEquals(FakeTimeProvider.START_TIME_MILLIS, rows.single().viewedAt)

            assertEquals(
                listOf(HistoryEntry(university = kfu, viewedAt = FakeTimeProvider.START_TIME_MILLIS)),
                RoomHistoryRepository(database.historyDao(), FakeTimeProvider()).observeHistory().first(),
            )
        }

    private companion object {
        const val ONE_MINUTE = 60_000L
    }
}
