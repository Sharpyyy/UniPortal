package com.example.uniportal.data.sync

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.uniportal.data.local.UniPortalDatabase
import com.example.uniportal.data.local.entity.OfflineCountryEntity
import com.example.uniportal.data.local.entity.ShortlistEntity
import com.example.uniportal.data.local.entity.ShortlistItemEntity
import com.example.uniportal.data.mapper.toEntity
import com.example.uniportal.data.repository.DefaultUniversityRepository
import com.example.uniportal.data.repository.RoomApplicationRepository
import com.example.uniportal.data.repository.RoomFavouritesRepository
import com.example.uniportal.data.repository.RoomHistoryRepository
import com.example.uniportal.data.repository.UniversityRepository.Companion.PAGE_SIZE
import com.example.uniportal.domain.model.AppSettings
import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.CacheTtl
import com.example.uniportal.domain.model.CountryFilter
import com.example.uniportal.domain.model.SearchParams
import com.example.uniportal.domain.model.University
import com.example.uniportal.testutil.FakeSettingsRepository
import com.example.uniportal.testutil.FakeTimeProvider
import com.example.uniportal.testutil.FakeUniversitiesApi
import com.example.uniportal.testutil.FakeUniversitiesApi.SearchCall
import com.example.uniportal.testutil.httpException
import com.example.uniportal.testutil.inMemoryDatabase
import com.example.uniportal.testutil.rawColumn
import com.example.uniportal.testutil.toDto
import com.example.uniportal.testutil.universities
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
class UniversitySyncerTest {
    private lateinit var database: UniPortalDatabase
    private lateinit var repository: DefaultUniversityRepository
    private lateinit var favourites: RoomFavouritesRepository
    private lateinit var history: RoomHistoryRepository
    private lateinit var applications: RoomApplicationRepository
    private lateinit var syncer: DefaultUniversitySyncer

    private val api = FakeUniversitiesApi()
    private val timeProvider = FakeTimeProvider()
    private val settings = FakeSettingsRepository(
        AppSettings(homeCountry = CountryFilter.CANADA, cacheTtl = CacheTtl.H24),
    )

    private val t0 = FakeTimeProvider.START_TIME_MILLIS
    private val oneHour = FakeTimeProvider.ONE_HOUR

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        repository = DefaultUniversityRepository(api, database.universityDao(), timeProvider)
        favourites = RoomFavouritesRepository(database.favouritesDao(), timeProvider)
        history = RoomHistoryRepository(database.historyDao(), timeProvider)
        applications = RoomApplicationRepository(database.applicationDao(), timeProvider)
        syncer = DefaultUniversitySyncer(repository, settings, timeProvider)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun cached(id: String): University? = repository.observeUniversity(id).first()

    private fun cachedIds(): Set<String> = database.rawColumn("universities", "id", orderBy = "id").toSet()

    private fun serveRenamed(vararg list: University) {
        api.universities = list.map { it.copy(name = it.name + " (new)").toDto() }
    }

    @Test
    fun `given stale and fresh pinned universities and an unpinned one, when synced, then only the stale pinned ones are refreshed oldest first`() =
        runTest {
            val viewed = university(id = "viewed.edu", name = "Viewed University")
            val favourite = university(id = "favourite.edu", name = "Favourite University")
            val listed = university(id = "listed.edu", name = "Listed University")
            val applied = university(id = "applied.edu", name = "Applied University")
            val fresh = university(id = "fresh.edu", name = "Fresh University")
            val plain = university(id = "plain.edu", name = "Plain University")
            history.recordView(viewed, limit = 50)
            database.universityDao().upsert(plain.toEntity(fetchedAt = timeProvider.now()))
            timeProvider.advanceBy(oneHour)
            favourites.add(favourite)
            timeProvider.advanceBy(oneHour)
            val shortlistId = database.shortlistDao().insert(
                ShortlistEntity(name = "Мечта", emoji = "⭐", description = "", createdAt = t0, updatedAt = t0),
            )
            database.shortlistDao().addItem(
                university = listed.toEntity(fetchedAt = timeProvider.now()),
                item = ShortlistItemEntity(shortlistId = shortlistId, universityId = listed.id, addedAt = timeProvider.now()),
                now = timeProvider.now(),
            )
            timeProvider.advanceBy(oneHour)
            applications.save(applied, ApplicationStatus.PREPARING, deadline = null, priority = 2, note = "")
            timeProvider.advanceBy(20 * oneHour)
            favourites.add(fresh)
            timeProvider.advanceBy(5 * oneHour)
            serveRenamed(viewed, favourite, listed, applied, fresh, plain)

            val report = syncer.sync()

            assertEquals(
                "stale = at least 24 h old; the fresh favourite (5 h) and the unpinned row are not requested",
                listOf(viewed.id, favourite.id, listed.id, applied.id),
                api.findByDomainCalls,
            )
            listOf(viewed, favourite, listed, applied).forEach { pinned ->
                assertEquals(pinned.name + " (new)", cached(pinned.id)?.name)
                assertEquals(timeProvider.now(), repository.cachedUniversityFetchedAt(pinned.id))
            }
            assertEquals(fresh, cached(fresh.id))
            assertEquals(t0 + 23 * oneHour, repository.cachedUniversityFetchedAt(fresh.id))
            assertEquals(plain, cached(plain.id))
            assertEquals(t0, repository.cachedUniversityFetchedAt(plain.id))
            assertEquals(SyncReport(refreshed = 4, failed = 0, pruned = 0), report)
            assertEquals(listOf<Any?>(timeProvider.now()), settings.writesOf("lastSyncAt"))
        }
}
