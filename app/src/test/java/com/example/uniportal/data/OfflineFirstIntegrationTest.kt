package com.example.uniportal.data

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.uniportal.data.local.UniPortalDatabase
import com.example.uniportal.data.repository.DefaultUniversityRepository
import com.example.uniportal.data.repository.RoomFavouritesRepository
import com.example.uniportal.data.repository.RoomOfflineCatalogRepository
import com.example.uniportal.data.sync.CountryDownloader
import com.example.uniportal.data.sync.DefaultUniversitySyncer
import com.example.uniportal.domain.model.AppSettings
import com.example.uniportal.domain.model.CountryFilter
import com.example.uniportal.domain.model.SearchParams
import com.example.uniportal.domain.model.University
import com.example.uniportal.testutil.FakeSettingsRepository
import com.example.uniportal.testutil.FakeTimeProvider
import com.example.uniportal.testutil.FakeUniversitiesApi
import com.example.uniportal.testutil.ROOM_FLOW_TIMEOUT
import com.example.uniportal.testutil.inMemoryDatabase
import com.example.uniportal.testutil.rawColumn
import com.example.uniportal.testutil.toDto
import com.example.uniportal.testutil.university
import com.example.uniportal.testutil.universityDtos
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class OfflineFirstIntegrationTest {
    private lateinit var database: UniPortalDatabase
    private lateinit var repository: DefaultUniversityRepository
    private lateinit var downloader: CountryDownloader
    private lateinit var favourites: RoomFavouritesRepository
    private lateinit var offlineCatalog: RoomOfflineCatalogRepository

    private val api = FakeUniversitiesApi()
    private val timeProvider = FakeTimeProvider()
    private val t0 = FakeTimeProvider.START_TIME_MILLIS

    private val mtsu = university(id = "mtsu.edu", name = "Middle Tennessee State University", stateProvince = "Tennessee")
    private val michigan = university(id = "msu.edu", name = "Michigan State University", stateProvince = "Michigan")
    private val tokyo = university(id = "u-tokyo.ac.jp", name = "University of Tokyo", country = "Japan", countryCode = "JP")
    private val kyoto = university(id = "kyoto-u.ac.jp", name = "Kyoto University", country = "Japan", countryCode = "JP")
    private val kyotoSangyo = university(
        id = "kyoto-su.ac.jp",
        name = "Kyoto Sangyo University",
        country = "Japan",
        countryCode = "JP",
    )

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        repository = DefaultUniversityRepository(api, database.universityDao(), timeProvider)
        downloader = CountryDownloader(api, database.universityDao(), database.offlineCountryDao(), timeProvider)
        favourites = RoomFavouritesRepository(database.favouritesDao(), timeProvider)
        offlineCatalog = RoomOfflineCatalogRepository(database.offlineCountryDao(), database.universityDao())
        api.universities = listOf(mtsu, michigan, tokyo, kyoto, kyotoSangyo).map { it.toDto() } +
            universityDtos(120, country = "Japan", alphaTwoCode = "JP")
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun loseNetwork() {
        api.searchHandler = { throw IOException("No route to host") }
        api.findByDomainHandler = { throw IOException("No route to host") }
    }

    @Test
    fun `given data cached online, when the network is lost, then cached search and details are still served unchanged`() =
        runTest {
            val stateSearch = SearchParams(query = "state", country = CountryFilter.USA)
            repository.refreshSearch(stateSearch)
            repository.refreshUniversity(mtsu.id)
            loseNetwork()
            timeProvider.advanceDays(2)

            repository.observeSearch(stateSearch).test(timeout = ROOM_FLOW_TIMEOUT) {
                val cached = awaitItem()
                assertEquals("in API order", listOf(mtsu, michigan), cached?.items)

                val refreshError = runCatching { repository.refreshSearch(stateSearch) }.exceptionOrNull()
                val detailError = runCatching { repository.refreshUniversity(mtsu.id) }.exceptionOrNull()

                assertTrue("was $refreshError", refreshError is IOException)
                assertTrue("was $detailError", detailError is IOException)
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(mtsu, repository.observeUniversity(mtsu.id).first())
            assertEquals(t0, repository.searchFetchedAt(stateSearch))
            assertEquals(t0, repository.cachedUniversityFetchedAt(mtsu.id))
        }
}
