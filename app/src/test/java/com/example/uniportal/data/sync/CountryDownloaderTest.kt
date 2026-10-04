package com.example.uniportal.data.sync

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.uniportal.data.local.UniPortalDatabase
import com.example.uniportal.data.repository.RoomOfflineCatalogRepository
import com.example.uniportal.data.sync.CountryDownloader.Companion.PAGE_SIZE
import com.example.uniportal.domain.model.OfflineCountry
import com.example.uniportal.testutil.FakeTimeProvider
import com.example.uniportal.testutil.FakeUniversitiesApi
import com.example.uniportal.testutil.FakeUniversitiesApi.SearchCall
import com.example.uniportal.testutil.countRows
import com.example.uniportal.testutil.inMemoryDatabase
import com.example.uniportal.testutil.rawLong
import com.example.uniportal.testutil.universityDtos
import java.io.IOException
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
class CountryDownloaderTest {
    private lateinit var database: UniPortalDatabase
    private lateinit var downloader: CountryDownloader
    private lateinit var offlineCatalog: RoomOfflineCatalogRepository

    private val api = FakeUniversitiesApi()
    private val timeProvider = FakeTimeProvider()

    private fun japanDtos(count: Int, startIndex: Int = 0) =
        universityDtos(count, startIndex = startIndex, country = "Japan", alphaTwoCode = "JP")

    private fun japanPage(offset: Int) = SearchCall(name = null, country = "Japan", limit = PAGE_SIZE, offset = offset)

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        downloader = CountryDownloader(api, database.universityDao(), database.offlineCountryDao(), timeProvider)
        offlineCatalog = RoomOfflineCatalogRepository(database.offlineCountryDao(), database.universityDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `given 250 universities of a country, when downloaded, then three pages of 100 are requested and all 250 saved`() =
        runTest {
            api.universities = japanDtos(250) + universityDtos(5, startIndex = 1000)

            val downloaded = downloader.download(countryName = "Japan", countryCode = "JP")

            assertEquals(250, downloaded)
            assertEquals(listOf(japanPage(0), japanPage(100), japanPage(200)), api.searchCalls)
            assertEquals(250, database.universityDao().countByCountry("Japan"))
            assertEquals("universities of other countries are not saved", 250, database.countRows("universities"))
            assertEquals(timeProvider.now(), database.rawLong("universities", "fetchedAt", "id", "uni249.edu"))
        }
}
