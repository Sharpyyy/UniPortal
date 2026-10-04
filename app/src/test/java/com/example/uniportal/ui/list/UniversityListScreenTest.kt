package com.example.uniportal.ui.list

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.uniportal.R
import com.example.uniportal.domain.model.CountryFilter
import com.example.uniportal.testutil.AppFakes
import com.example.uniportal.testutil.FakeTimeProvider
import com.example.uniportal.testutil.searchParams
import com.example.uniportal.testutil.string
import com.example.uniportal.testutil.universities
import com.example.uniportal.testutil.university
import com.example.uniportal.testutil.universityPage
import com.example.uniportal.testutil.waitUntilNodeExists
import com.example.uniportal.testutil.waitUntilTagDoesNotExist
import com.example.uniportal.testutil.waitUntilTagExists
import com.example.uniportal.ui.components.TestTags
import com.example.uniportal.ui.theme.UniPortalTheme
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h891dp")
class UniversityListScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakes = AppFakes()
    private val repository = fakes.universityRepository
    private val favouritesRepository = fakes.favouritesRepository

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

    private fun setListScreen() {
        composeTestRule.setContent {
            UniPortalTheme {
                UniversityListRoute(
                    onUniversityClick = {},
                    viewModel = viewModel { fakes.listViewModel() },
                )
            }
        }
    }

    @Test
    fun `given first load fails, when Retry is pressed, then a new request is made and the list is shown`() {
        val items = universities(2)
        repository.enqueueRefreshError(IOException("No route to host"))
        repository.enqueueRefresh(items)
        setListScreen()

        composeTestRule.waitUntilTagExists(TestTags.ERROR_STATE)
        composeTestRule.onNodeWithText(string(R.string.error_network_title)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.error_network_message)).assertExists()
        composeTestRule.onNodeWithTag(TestTags.UNIVERSITY_LIST).assertDoesNotExist()
        assertEquals(1, repository.refreshSearchCalls.size)

        composeTestRule.onNodeWithTag(TestTags.RETRY_BUTTON).performClick()
        composeTestRule.waitUntilTagExists(TestTags.universityItem(items.first().id))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.ERROR_STATE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.RETRY_BUTTON).assertDoesNotExist()
        items.forEach { university ->
            composeTestRule.onNodeWithTag(TestTags.universityItem(university.id)).assertIsDisplayed()
        }
        assertEquals("Retry sends a new request", 2, repository.refreshSearchCalls.size)
        assertEquals("the same first page is requested again", repository.refreshSearchCalls[0], repository.refreshSearchCalls[1])
    }
}
