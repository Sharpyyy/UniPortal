package com.example.uniportal.ui.list

import app.cash.turbine.test
import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.ErrorType
import com.example.uniportal.domain.model.University
import com.example.uniportal.domain.rules.ApplicationRules
import com.example.uniportal.testutil.AppFakes
import com.example.uniportal.testutil.FakeTimeProvider
import com.example.uniportal.testutil.MainDispatcherRule
import com.example.uniportal.testutil.searchParams
import com.example.uniportal.testutil.universities
import com.example.uniportal.testutil.university
import com.example.uniportal.testutil.universityPage
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceTimeBy
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
class UniversityListFlowTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakes = AppFakes()
    private val repository = fakes.universityRepository
    private val favouritesRepository = fakes.favouritesRepository
    private val timeProvider = fakes.timeProvider

    private val all = searchParams()

    private val mtsu = university(id = "mtsu.edu", name = "Middle Tennessee State University")
    private val kfu = university(
        id = "kpfu.ru",
        name = "Kazan Federal University",
        country = "Russian Federation",
        countryCode = "RU",
    )
    private val tokyo = university(
        id = "u-tokyo.ac.jp",
        name = "University of Tokyo",
        country = "Japan",
        countryCode = "JP",
    )

    private fun createViewModel() = fakes.listViewModel()

    private fun resultFor(query: String): University =
        university(id = "${query.ifEmpty { "all" }.lowercase()}.edu", name = query.ifEmpty { "All universities" })

    @Test
    fun `given first page fails, when Retry, then it emits Loading, Error, Loading, Success and one new request`() =
        runTest {
            val items = universities(2)
            repository.enqueueRefreshError(IOException("No route to host"))
            repository.enqueueRefresh(items)
            val viewModel = createViewModel()

            viewModel.uiState.map { it.content }.distinctUntilChanged().test {
                assertEquals(ListContent.Loading, awaitItem())
                assertEquals(ListContent.Error(ErrorType.NETWORK), awaitItem())

                val gate = CompletableDeferred<Unit>()
                repository.refreshGate = gate
                viewModel.onEvent(UniversityListEvent.Retry)
                assertEquals("Loading again", ListContent.Loading, awaitItem())
                runCurrent()
                expectNoEvents()
                assertEquals(2, repository.refreshSearchCalls.size)

                gate.complete(Unit)
                assertEquals(ListContent.Success(items), awaitItem())
                advanceUntilIdle()
                expectNoEvents()
            }
            assertEquals("Retry issued exactly one new request", 2, repository.refreshSearchCalls.size)
            assertEquals(repository.refreshSearchCalls[0], repository.refreshSearchCalls[1])
        }

    @Test
    fun `given slow request for query A, when query B is typed, then A is cancelled and its result is never emitted`() =
        runTest {
            val oxfordGate = CompletableDeferred<Unit>()
            repository.searchHandler = { params, _ ->
                if (params.query == "Oxford") oxfordGate.await()
                universityPage(items = listOf(resultFor(params.query)))
            }
            val viewModel = createViewModel()
            val emitted = mutableListOf<ListContent>()

            viewModel.uiState.map { it.content }.distinctUntilChanged().test {
                assertEquals(ListContent.Loading, awaitItem().also { emitted += it })
                assertEquals(ListContent.Success(listOf(resultFor(""))), awaitItem().also { emitted += it })

                viewModel.onEvent(UniversityListEvent.QueryChanged("Oxford"))
                assertEquals(
                    "after the debounce the Oxford search starts from Loading",
                    ListContent.Loading,
                    awaitItem().also { emitted += it },
                )
                runCurrent()
                assertEquals("the Oxford request is in flight", searchParams(query = "Oxford"), repository.refreshSearchCalls.last())

                viewModel.onEvent(UniversityListEvent.QueryChanged("Harvard"))
                assertEquals(ListContent.Success(listOf(resultFor("Harvard"))), awaitItem().also { emitted += it })

                oxfordGate.complete(Unit)
                advanceUntilIdle()
                expectNoEvents()
                assertEquals("Harvard", viewModel.uiState.value.query)
            }

            assertEquals(listOf("", "Oxford", "Harvard"), repository.refreshSearchCalls.map { it.query })
            assertEquals("the Oxford request never finished", listOf("", "Harvard"), repository.completedRefreshSearches.map { it.query })
            assertNull("its answer never reached the cache", repository.cachedSearch(searchParams(query = "Oxford")))
            assertTrue(
                "no emitted state ever contained the Oxford result",
                emitted.none { content -> (content as? ListContent.Success)?.items?.contains(resultFor("Oxford")) == true },
            )
        }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L

        const val TYPING_INTERVAL_MS = 100L
    }
}
