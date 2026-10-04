package com.example.uniportal.ui.list

import com.example.uniportal.data.repository.UniversityRepository
import com.example.uniportal.domain.model.CacheTtl
import com.example.uniportal.domain.model.CountryFilter
import com.example.uniportal.domain.model.ErrorType
import com.example.uniportal.testutil.AppFakes
import com.example.uniportal.testutil.FakeTimeProvider
import com.example.uniportal.testutil.FakeUniversityRepository.LocalSearchCall
import com.example.uniportal.testutil.MainDispatcherRule
import com.example.uniportal.testutil.collectInBackground
import com.example.uniportal.testutil.httpException
import com.example.uniportal.testutil.offlineCountry
import com.example.uniportal.testutil.searchParams
import com.example.uniportal.testutil.universities
import com.example.uniportal.testutil.university
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
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
class UniversityListViewModelTest {
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
    private val msu = university(
        id = "msu.ru",
        name = "Lomonosov Moscow State University",
        country = "Russian Federation",
        countryCode = "RU",
    )
    private val tokyo = university(id = "u-tokyo.ac.jp", name = "University of Tokyo", country = "Japan", countryCode = "JP")

    private fun TestScope.createViewModel(): UniversityListViewModel =
        fakes.listViewModel().also { viewModel -> collectInBackground(viewModel.uiState) }

    @Test
    fun `given nothing cached, when ViewModel is created, then state is Loading and the first page is refreshed exactly once`() =
        runTest {
            val items = universities(3)
            val gate = CompletableDeferred<Unit>()
            repository.refreshGate = gate
            repository.enqueueRefresh(items)

            val viewModel = createViewModel()

            assertEquals(UniversityListUiState(), viewModel.uiState.value)
            runCurrent()
            assertEquals(ListContent.Loading, viewModel.uiState.value.content)
            assertEquals(listOf(all), repository.observeSearchCalls)
            assertEquals(listOf(all), repository.refreshSearchCalls)

            collectInBackground(viewModel.uiState)
            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(ListContent.Success(items), viewModel.uiState.value.content)
            assertEquals(1, repository.refreshSearchCalls.size)
            assertEquals(1, repository.observeSearchCalls.size)
        }

    @Test
    fun `given the network returns universities, when the first page loads, then content is Success read from the cache`() =
        runTest {
            val items = universities(3)
            repository.enqueueRefresh(items, hasMore = true)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(ListContent.Success(items), state.content)
            assertEquals(ResultsSource.CACHE, state.source)
            assertNull(state.banner)
            assertTrue(state.canLoadMore)
            assertFalse(state.isRefreshing)
            assertFalse(state.isUpdating)
            assertFalse(state.isLoadingMore)
            assertNull(state.loadMoreError)
            assertEquals("the page is stored on the device", items, repository.cachedSearch(all)?.items)
        }

    @Test
    fun `given network failure and nothing on the device, when the first page loads, then content is Error NETWORK`() =
        runTest {
            repository.enqueueRefreshError(IOException("No route to host"))

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(ListContent.Error(ErrorType.NETWORK), state.content)
            assertEquals(ResultsSource.NONE, state.source)
            assertNull(state.banner)
            assertFalse(state.canLoadMore)
            assertEquals(
                "the device catalogue is searched before giving up",
                LocalSearchCall(params = all, limit = UniversityRepository.LOCAL_SEARCH_LIMIT + 1),
                repository.observeLocalSearchCalls.first(),
            )
        }

    @Test
    fun `given empty first page, when it loads, then content is Empty and not an empty Success`() = runTest {
        repository.enqueueRefresh(emptyList(), hasMore = false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ListContent.Empty, state.content)
        assertFalse(state.content is ListContent.Success)
        assertFalse(state.canLoadMore)
        assertEquals("an empty answer is cached too", ResultsSource.CACHE, state.source)
    }

    @Test
    fun `given loaded list, when Refresh fails, then list is kept with the refresh-failed banner and a one-shot message`() =
        runTest {
            val oldItems = universities(2)
            repository.enqueueRefresh(oldItems)
            repository.enqueueRefreshError(IOException("No route to host"))
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(UniversityListEvent.Refresh)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(ListContent.Success(oldItems), state.content)
            assertFalse(state.isRefreshing)
            assertEquals(
                SearchBanner.RefreshFailed(fetchedAt = FakeTimeProvider.START_TIME_MILLIS, error = ErrorType.NETWORK),
                state.banner,
            )
            assertEquals(SearchMessage.REFRESH_FAILED, state.message)

            viewModel.onEvent(UniversityListEvent.MessageShown)
            runCurrent()
            assertNull(viewModel.uiState.value.message)
            assertEquals("the banner stays until a refresh succeeds", state.banner, viewModel.uiState.value.banner)
            assertEquals(ListContent.Success(oldItems), viewModel.uiState.value.content)
        }

    private companion object {
        const val TYPING_INTERVAL_MS = 100L
    }
}
