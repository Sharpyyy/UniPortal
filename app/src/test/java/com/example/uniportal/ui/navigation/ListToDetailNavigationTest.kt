package com.example.uniportal.ui.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.uniportal.R
import com.example.uniportal.domain.model.University
import com.example.uniportal.testutil.AppFakes
import com.example.uniportal.testutil.FakeTimeProvider
import com.example.uniportal.testutil.searchParams
import com.example.uniportal.testutil.string
import com.example.uniportal.testutil.university
import com.example.uniportal.testutil.universityPage
import com.example.uniportal.testutil.waitUntilNodeExists
import com.example.uniportal.testutil.waitUntilTagDoesNotExist
import com.example.uniportal.testutil.waitUntilTagExists
import com.example.uniportal.ui.components.TestTags
import com.example.uniportal.ui.detail.UniversityDetailRoute
import com.example.uniportal.ui.list.UniversityListRoute
import com.example.uniportal.ui.theme.UniPortalTheme
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h891dp")
class ListToDetailNavigationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakes = AppFakes()
    private val universityRepository = fakes.universityRepository
    private val historyRepository = fakes.historyRepository
    private val favouritesRepository = fakes.favouritesRepository
    private lateinit var navController: NavHostController

    private val mtsu = university(id = "mtsu.edu", name = "Middle Tennessee State University")
    private val kfu = university(
        id = "kpfu.ru",
        name = "Kazan Federal University",
        country = "Russian Federation",
        countryCode = "RU",
        domains = listOf("kpfu.ru", "ksu.ru"),
    )
    private val tokyo = university(
        id = "u-tokyo.ac.jp",
        name = "University of Tokyo",
        country = "Japan",
        countryCode = "JP",
    )

    private val addText: String
        get() = string(R.string.cd_add_favourite)
    private val removeText: String
        get() = string(R.string.cd_remove_favourite)

    private fun setAppContent() {
        composeTestRule.setContent {
            val controller = rememberNavController()
            navController = controller
            UniPortalTheme {
                TestAppNavHost(navController = controller, fakes = fakes)
            }
        }
    }

    private fun openDetailFromList(university: University) {
        composeTestRule.waitUntilTagExists(TestTags.universityItem(university.id))
        composeTestRule.onNodeWithTag(TestTags.universityItem(university.id)).performClick()
    }

    private fun waitForDetailHeart(contentDescription: String) {
        composeTestRule.waitUntilNodeExists(
            hasTestTag(TestTags.DETAIL_FAVOURITE_BUTTON) and hasContentDescription(contentDescription),
        )
    }

    @Test
    fun `given search results, when a university is clicked, then detail opens for exactly that id from the cache`() {
        universityRepository.remoteUniversities = listOf(mtsu, kfu, tokyo)
        setAppContent()

        openDetailFromList(kfu)
        composeTestRule.waitUntilTagExists(TestTags.DETAIL_TITLE)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.DETAIL_TITLE).assertTextEquals(kfu.name)
        composeTestRule.onNodeWithText(kfu.domains[1]).assertExists()
        composeTestRule.onNodeWithTag(TestTags.UNIVERSITY_LIST).assertDoesNotExist()
        assertEquals(Routes.DETAIL, navController.currentDestination?.route)
        assertEquals(kfu.id, navController.currentBackStackEntry?.arguments?.getString(Routes.ARG_ID))
        assertEquals("the detail screen observed the clicked id, once", listOf(kfu.id), universityRepository.observeUniversityCalls)
        assertTrue("just cached by the search: no request", universityRepository.refreshUniversityCalls.isEmpty())
        assertEquals(listOf(kfu), historyRepository.recordedViews)
    }
}

@Composable
private fun TestAppNavHost(
    navController: NavHostController,
    fakes: AppFakes,
) {
    NavHost(navController = navController, startDestination = Routes.SEARCH) {
        composable(route = Routes.SEARCH) {
            UniversityListRoute(
                onUniversityClick = { id -> navController.navigate(Routes.detail(id)) },
                viewModel = viewModel { fakes.listViewModel() },
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.StringType }),
        ) {
            UniversityDetailRoute(
                onBack = { navController.popBackStack() },
                onEditApplication = {},
                viewModel = viewModel { fakes.detailViewModel(createSavedStateHandle()) },
            )
        }
    }
}
