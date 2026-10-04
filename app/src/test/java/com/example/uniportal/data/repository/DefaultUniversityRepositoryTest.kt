package com.example.uniportal.data.repository

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.uniportal.data.local.UniPortalDatabase
import com.example.uniportal.data.local.entity.OfflineCountryEntity
import com.example.uniportal.data.local.entity.ShortlistEntity
import com.example.uniportal.data.local.entity.ShortlistItemEntity
import com.example.uniportal.data.mapper.toEntity
import com.example.uniportal.data.repository.UniversityRepository.Companion.PAGE_SIZE
import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.CachedSearch
import com.example.uniportal.domain.model.CountryFilter
import com.example.uniportal.domain.model.SearchParams
import com.example.uniportal.domain.model.University
import com.example.uniportal.domain.model.UniversityNotFoundException
import com.example.uniportal.domain.rules.CachePolicy
import com.example.uniportal.testutil.FakeTimeProvider
import com.example.uniportal.testutil.FakeUniversitiesApi
import com.example.uniportal.testutil.FakeUniversitiesApi.SearchCall
import com.example.uniportal.testutil.ROOM_FLOW_TIMEOUT
import com.example.uniportal.testutil.countRows
import com.example.uniportal.testutil.httpException
import com.example.uniportal.testutil.inMemoryDatabase
import com.example.uniportal.testutil.rawColumn
import com.example.uniportal.testutil.rawLong
import com.example.uniportal.testutil.toDto
import com.example.uniportal.testutil.universities
import com.example.uniportal.testutil.university
import com.example.uniportal.testutil.universityDto
import com.example.uniportal.testutil.universityDtos
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
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
import retrofit2.HttpException

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class DefaultUniversityRepositoryTest {
    private lateinit var database: UniPortalDatabase
    private lateinit var repository: DefaultUniversityRepository

    private val api = FakeUniversitiesApi()
    private val timeProvider = FakeTimeProvider()

    private val all = SearchParams(query = "", country = CountryFilter.ALL)

    private val kfu = university(
        id = "kpfu.ru",
        name = "Kazan Federal University",
        country = "Russian Federation",
        countryCode = "RU",
        stateProvince = "Tatarstan",
        domains = listOf("kpfu.ru", "ksu.ru"),
        webPages = listOf("http://www.kpfu.ru/"),
    )
    private val mtsu = university(id = "mtsu.edu", name = "Middle Tennessee State University", stateProvince = "Tennessee")
    private val michigan = university(id = "msu.edu", name = "Michigan State University", stateProvince = "Michigan")
    private val moscow = university(
        id = "msu.ru",
        name = "Moscow State University",
        country = "Russian Federation",
        countryCode = "RU",
    )
    private val tokyo = university(id = "u-tokyo.ac.jp", name = "University of Tokyo", country = "Japan", countryCode = "JP")

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        repository = DefaultUniversityRepository(api, database.universityDao(), timeProvider)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun cachedSearchOf(params: SearchParams): CachedSearch =
        checkNotNull(repository.observeSearch(params).first()) { "search $params is not cached" }

    @Test
    fun `given API finds nothing, when refreshed, then an empty search is cached, not a missing one`() = runTest {
        val params = SearchParams(query = "Nonexistent", country = CountryFilter.ALL)
        api.enqueueSearch(emptyList())

        repository.refreshSearch(params)

        assertEquals(
            CachedSearch(items = emptyList(), hasMore = false, loadedPages = 1, fetchedAt = FakeTimeProvider.START_TIME_MILLIS),
            repository.observeSearch(params).first(),
        )
        assertEquals(FakeTimeProvider.START_TIME_MILLIS, repository.searchFetchedAt(params))
    }
}
