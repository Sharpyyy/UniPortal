package com.example.uniportal.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.uniportal.R
import com.example.uniportal.ui.applications.ApplicationsRoute
import com.example.uniportal.ui.components.TestTags
import com.example.uniportal.ui.detail.UniversityDetailRoute
import com.example.uniportal.ui.editor.ApplicationEditorRoute
import com.example.uniportal.ui.history.HistoryRoute
import com.example.uniportal.ui.list.UniversityListRoute
import com.example.uniportal.ui.lists.ListsRoute
import com.example.uniportal.ui.lists.ListsTab
import com.example.uniportal.ui.more.MoreRoute
import com.example.uniportal.ui.offline.OfflineCatalogRoute
import com.example.uniportal.ui.settings.SettingsRoute
import com.example.uniportal.ui.shortlist.ShortlistRoute
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UniPortalAppScaffold(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    applicationsBadgeCount: Int = 0,
    pendingTab: TopLevelDestination? = null,
    onPendingTabHandled: () -> Unit = {},
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = backStackEntry?.destination?.let { destination ->
        TopLevelDestination.entries.firstOrNull { tab -> destination.hierarchy.any { it.route == tab.route } }
    }

    var lastTab by remember { mutableStateOf(TopLevelDestination.SEARCH) }
    SideEffect { if (currentTab != null) lastTab = currentTab }

    val showBottomBar = backStackEntry == null || currentTab != null

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,

        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                PortalNavigationBar(
                    selectedTab = currentTab ?: lastTab,
                    applicationsBadgeCount = applicationsBadgeCount,
                    onTabSelected = { tab -> navController.navigateToTopLevel(tab) },
                )
            }
        },
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)

                .consumeWindowInsets(innerPadding),
        )
        LaunchedEffect(pendingTab) {
            val tab = pendingTab ?: return@LaunchedEffect

            navController.currentBackStackEntryFlow.first()
            navController.navigateToTopLevel(tab)
            onPendingTabHandled()
        }
    }
}

@Composable
private fun PortalNavigationBar(
    selectedTab: TopLevelDestination,
    applicationsBadgeCount: Int,
    onTabSelected: (TopLevelDestination) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column {
        HorizontalDivider(color = colors.outlineVariant)
        NavigationBar(
            modifier = Modifier.testTag(TestTags.BOTTOM_NAV),
            containerColor = colors.surfaceContainerLow,
            tonalElevation = 0.dp,
        ) {
            TopLevelDestination.entries.forEach { tab ->
                val selected = tab == selectedTab
                NavigationBarItem(
                    selected = selected,
                    onClick = { if (!selected) onTabSelected(tab) },
                    icon = {
                        val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
                        if (tab == TopLevelDestination.APPLICATIONS && applicationsBadgeCount > 0) {
                            val badgeDescription = pluralStringResource(
                                R.plurals.cd_applications_badge,
                                applicationsBadgeCount,
                                applicationsBadgeCount,
                            )
                            BadgedBox(
                                badge = {
                                    Badge(

                                        modifier = Modifier.clearAndSetSemantics {
                                            contentDescription = badgeDescription
                                        },
                                        containerColor = colors.error,
                                        contentColor = colors.onError,
                                    ) {
                                        Text(
                                            text = if (applicationsBadgeCount > MAX_BADGE_COUNT) {
                                                stringResource(R.string.badge_overflow)
                                            } else {
                                                applicationsBadgeCount.toString()
                                            },
                                        )
                                    }
                                },
                            ) {
                                Icon(imageVector = icon, contentDescription = null)
                            }
                        } else {
                            Icon(imageVector = icon, contentDescription = null)
                        }
                    },
                    label = {
                        Text(
                            text = stringResource(tab.labelRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    modifier = Modifier.testTag(TestTags.navItem(tab.route)),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.onPrimaryContainer,
                        selectedTextColor = colors.primary,
                        indicatorColor = colors.primaryContainer,
                        unselectedIconColor = colors.onSurfaceVariant,
                        unselectedTextColor = colors.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SEARCH,
        modifier = modifier,

        enterTransition = {
            if (isTabSwitch()) {
                fadeIn(tween(TAB_FADE_MS))
            } else {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(TRANSITION_MS)) +
                    fadeIn(tween(TRANSITION_MS))
            }
        },
        exitTransition = { fadeOut(tween(if (isTabSwitch()) TAB_FADE_MS else TRANSITION_MS)) },
        popEnterTransition = { fadeIn(tween(if (isTabSwitch()) TAB_FADE_MS else TRANSITION_MS)) },
        popExitTransition = {
            if (isTabSwitch()) {
                fadeOut(tween(TAB_FADE_MS))
            } else {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(TRANSITION_MS)) +
                    fadeOut(tween(TRANSITION_MS))
            }
        },
    ) {
        composable(route = Routes.SEARCH) { entry ->
            UniversityListRoute(
                onUniversityClick = { id -> entry.ifResumed { navController.navigate(Routes.detail(id)) } },
            )
        }

        composable(route = Routes.LISTS) { entry ->

            val selectedTabName by remember(entry) {
                entry.savedStateHandle.getStateFlow(LISTS_TAB_KEY, ListsTab.FAVOURITES.name)
            }.collectAsStateWithLifecycle()
            ListsRoute(
                selectedTab = ListsTab.entries.firstOrNull { it.name == selectedTabName } ?: ListsTab.FAVOURITES,
                onTabSelected = { tab -> entry.savedStateHandle[LISTS_TAB_KEY] = tab.name },
                onUniversityClick = { id -> entry.ifResumed { navController.navigate(Routes.detail(id)) } },
                onShortlistClick = { shortlistId ->
                    entry.ifResumed { navController.navigate(Routes.shortlist(shortlistId)) }
                },
                onOpenSearch = { entry.ifResumed { navController.navigateToTopLevel(TopLevelDestination.SEARCH) } },
            )
        }

        composable(route = Routes.APPLICATIONS) { entry ->
            ApplicationsRoute(
                onUniversityClick = { id -> entry.ifResumed { navController.navigate(Routes.detail(id)) } },
                onEditApplication = { universityId ->
                    entry.ifResumed { navController.navigate(Routes.application(universityId)) }
                },
                onOpenSearch = { entry.ifResumed { navController.navigateToTopLevel(TopLevelDestination.SEARCH) } },
            )
        }

        composable(route = Routes.MORE) { entry ->
            MoreRoute(
                onOpenHistory = { entry.ifResumed { navController.navigate(Routes.HISTORY) } },
                onOpenSettings = { entry.ifResumed { navController.navigate(Routes.SETTINGS) } },
                onOpenOffline = { entry.ifResumed { navController.navigate(Routes.OFFLINE) } },
                onOpenFavourites = { entry.ifResumed { navController.navigateToListsTab(ListsTab.FAVOURITES) } },
                onOpenShortlists = { entry.ifResumed { navController.navigateToListsTab(ListsTab.SHORTLISTS) } },
                onOpenApplications = {
                    entry.ifResumed { navController.navigateToTopLevel(TopLevelDestination.APPLICATIONS) }
                },
            )
        }

        composable(route = Routes.HISTORY) { entry ->
            HistoryRoute(
                onUniversityClick = { id -> entry.ifResumed { navController.navigate(Routes.detail(id)) } },
                onBack = { entry.ifResumed { navController.navigateUp() } },
                onOpenSearch = { entry.ifResumed { navController.navigateToTopLevel(TopLevelDestination.SEARCH) } },
            )
        }

        composable(route = Routes.SETTINGS) { entry ->
            SettingsRoute(
                onBack = { entry.ifResumed { navController.navigateUp() } },
                onOpenOffline = { entry.ifResumed { navController.navigate(Routes.OFFLINE) } },
            )
        }

        composable(route = Routes.OFFLINE) { entry ->
            OfflineCatalogRoute(
                onBack = { entry.ifResumed { navController.navigateUp() } },
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.StringType }),
        ) { entry ->
            UniversityDetailRoute(
                onBack = { entry.ifResumed { navController.navigateUp() } },
                onEditApplication = { universityId ->
                    entry.ifResumed { navController.navigate(Routes.application(universityId)) }
                },
            )
        }

        composable(
            route = Routes.SHORTLIST,
            arguments = listOf(navArgument(Routes.ARG_SHORTLIST_ID) { type = NavType.LongType }),
        ) { entry ->
            ShortlistRoute(
                onUniversityClick = { id -> entry.ifResumed { navController.navigate(Routes.detail(id)) } },

                onBack = { entry.ifResumed { navController.navigateUp() } },
                onOpenSearch = { entry.ifResumed { navController.navigateToTopLevel(TopLevelDestination.SEARCH) } },
            )
        }

        composable(
            route = Routes.APPLICATION,
            arguments = listOf(navArgument(Routes.ARG_UNIVERSITY_ID) { type = NavType.StringType }),
        ) { entry ->
            ApplicationEditorRoute(
                onClose = { entry.ifResumed { navController.navigateUp() } },
            )
        }
    }
}

fun NavHostController.navigateToTopLevel(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.navigateToListsTab(tab: ListsTab) {
    navigateToTopLevel(TopLevelDestination.LISTS)
    getBackStackEntry(Routes.LISTS).savedStateHandle[LISTS_TAB_KEY] = tab.name
}

private inline fun NavBackStackEntry.ifResumed(action: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) action()
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
    TopLevelDestination.fromRoute(initialState.destination.route) != null &&
        TopLevelDestination.fromRoute(targetState.destination.route) != null

private const val TRANSITION_MS = 300
private const val TAB_FADE_MS = 200
private const val MAX_BADGE_COUNT = 99
private const val LISTS_TAB_KEY = "lists_tab"
