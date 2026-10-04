package com.example.uniportal.ui.list

import app.cash.turbine.test
import com.example.uniportal.domain.model.CountryFilter
import com.example.uniportal.domain.model.ErrorType
import com.example.uniportal.domain.model.University
import com.example.uniportal.testutil.AppFakes
import com.example.uniportal.testutil.FakeTimeProvider
import com.example.uniportal.testutil.MainDispatcherRule
import com.example.uniportal.testutil.searchParams
import com.example.uniportal.testutil.universities
import com.example.uniportal.testutil.university
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UniversityListOfflineFlowTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakes = AppFakes()
    private val repository = fakes.universityRepository
    private val timeProvider = fakes.timeProvider

    private val all = searchParams()

    private val mtsu = university(id = "mtsu.edu", name = "Middle Tennessee State University")
    private val kfu = university(
        id = "kpfu.ru",
        name = "Kazan Federal University",
        country = "Russian Federation",
        countryCode = "RU",
    )
    private val msu = university(
        id = "msu.ru",
        name = "Lomonosov Moscow State University",
        country = "Russian Federation",
        countryCode = "RU",
    )
    private val tokyo = university(id = "u-tokyo.ac.jp", name = "University of Tokyo", country = "Japan", countryCode = "JP")

    private fun createViewModel() = fakes.listViewModel()

    private data class Shown(val content: ListContent, val source: ResultsSource, val banner: SearchBanner?)

    private val loading = Shown(ListContent.Loading, ResultsSource.NONE, null)

    @Test
    fun `given offline and no cached search, when queries and countries change, then each is searched on the device only`() =
        runTest {
            fakes.connectivity.goOffline()
            listOf(tokyo, mtsu, msu, kfu).forEach { repository.putUniversity(it) }
            val viewModel = createViewModel()

            viewModel.uiState
                .filter { it.content != ListContent.Loading }
                .map { Shown(it.content, it.source, it.banner) }
                .distinctUntilChanged()
                .test {
                    fun local(vararg items: University) = Shown(
                        ListContent.Success(items.toList()),
                        ResultsSource.LOCAL_CATALOG,
                        SearchBanner.LocalCatalog(isOnline = false),
                    )

                    assertEquals(local(kfu, msu, mtsu, tokyo), awaitItem())

                    viewModel.onEvent(UniversityListEvent.QueryChanged("state"))
                    assertEquals(local(msu, mtsu), awaitItem())

                    viewModel.onEvent(UniversityListEvent.CountrySelected(CountryFilter.RUSSIA))
                    assertEquals(local(msu), awaitItem())

                    viewModel.onEvent(UniversityListEvent.QueryChanged("tokyo"))
                    assertEquals(
                        "no local match and no network",
                        Shown(ListContent.Error(ErrorType.NETWORK), ResultsSource.NONE, null),
                        awaitItem(),
                    )

                    viewModel.onEvent(UniversityListEvent.QueryChanged(""))
                    assertEquals(local(kfu, msu), awaitItem())

                    viewModel.onEvent(UniversityListEvent.CountrySelected(CountryFilter.ALL))
                    assertEquals(local(kfu, msu, mtsu, tokyo), awaitItem())

                    advanceUntilIdle()
                    expectNoEvents()
                }

            assertTrue("nothing was requested offline", repository.refreshSearchCalls.isEmpty())
            assertTrue(repository.pageRequests.isEmpty())
            assertEquals(
                "every search went to the device catalogue (in order of first use)",
                listOf(
                    all,
                    searchParams(query = "state"),
                    searchParams(query = "state", country = CountryFilter.RUSSIA),
                    searchParams(query = "tokyo", country = CountryFilter.RUSSIA),
                    searchParams(country = CountryFilter.RUSSIA),
                ),
                repository.observeLocalSearchCalls.map { it.params }.distinct(),
            )
            assertEquals(all, repository.observeLocalSearchCalls.last().params)
        }

    @Test
    fun `given a slow settings read with a home country, when the screen opens, then the first search waits for it and uses it`() =
        runTest {
            val russia = searchParams(country = CountryFilter.RUSSIA)
            val settingsGate = CompletableDeferred<Unit>()
            fakes.settingsRepository.update { it.copy(homeCountry = CountryFilter.RUSSIA) }
            fakes.settingsRepository.readGate = settingsGate
            val networkGate = CompletableDeferred<Unit>()
            repository.refreshGate = networkGate
            repository.enqueueRefresh(listOf(kfu, msu))
            val viewModel = createViewModel()

            viewModel.uiState.map { it.country to it.content }.distinctUntilChanged().test {
                assertEquals(CountryFilter.ALL to ListContent.Loading, awaitItem())
                advanceUntilIdle()
                expectNoEvents()
                assertTrue("nothing is searched before the home country is known", repository.observeSearchCalls.isEmpty())

                settingsGate.complete(Unit)
                assertEquals(CountryFilter.RUSSIA to ListContent.Loading, awaitItem())
                advanceUntilIdle()
                expectNoEvents()
                assertEquals(listOf(russia), repository.refreshSearchCalls)

                networkGate.complete(Unit)
                assertEquals(CountryFilter.RUSSIA to ListContent.Success(listOf(kfu, msu)), awaitItem())
                advanceUntilIdle()
                expectNoEvents()
            }
            assertEquals("all countries were never searched", listOf(russia), repository.observeSearchCalls)
        }
}
