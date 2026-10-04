package com.example.uniportal.ui.detail

import androidx.lifecycle.SavedStateHandle
import com.example.uniportal.domain.model.AppSettings
import com.example.uniportal.domain.model.ErrorType
import com.example.uniportal.domain.model.HistoryEntry
import com.example.uniportal.domain.model.UniversityNotFoundException
import com.example.uniportal.testutil.AppFakes
import com.example.uniportal.testutil.FakeHistoryRepository.RecordViewCall
import com.example.uniportal.testutil.FakeTimeProvider
import com.example.uniportal.testutil.MainDispatcherRule
import com.example.uniportal.testutil.collectInBackground
import com.example.uniportal.testutil.httpException
import com.example.uniportal.testutil.university
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UniversityDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakes = AppFakes()
    private val repository = fakes.universityRepository
    private val historyRepository = fakes.historyRepository
    private val favouritesRepository = fakes.favouritesRepository
    private val timeProvider = fakes.timeProvider

    private val mtsu = university(id = "mtsu.edu", name = "Middle Tennessee State University")
    private val tokyo = university(
        id = "u-tokyo.ac.jp",
        name = "University of Tokyo",
        country = "Japan",
        countryCode = "JP",
    )

    private val staleFetchedAt: Long
        get() = timeProvider.now() - 2 * FakeTimeProvider.ONE_DAY

    private fun TestScope.createViewModel(id: String): UniversityDetailViewModel =
        fakes.detailViewModel(id).also { viewModel -> collectInBackground(viewModel.uiState) }

    @Test
    fun `given no id in SavedStateHandle, when ViewModel is created, then it fails fast`() {
        assertThrows(IllegalStateException::class.java) {
            fakes.detailViewModel(SavedStateHandle())
        }
    }
}
