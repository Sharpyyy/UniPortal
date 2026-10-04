package com.example.uniportal.ui.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniportal.R
import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.CountryFilter
import com.example.uniportal.domain.model.ErrorType
import com.example.uniportal.domain.model.University
import com.example.uniportal.ui.components.BannerTone
import com.example.uniportal.ui.components.CountryFilterRow
import com.example.uniportal.ui.components.EmptyState
import com.example.uniportal.ui.components.ErrorState
import com.example.uniportal.ui.components.FavouritesOnlyChip
import com.example.uniportal.ui.components.LoadingState
import com.example.uniportal.ui.components.OfflineBanner
import com.example.uniportal.ui.components.PortalLogo
import com.example.uniportal.ui.components.PortalSnackbarHost
import com.example.uniportal.ui.components.SearchField
import com.example.uniportal.ui.components.TestTags
import com.example.uniportal.ui.components.UniversityCard
import com.example.uniportal.ui.components.rememberCurrentTimeMillis
import com.example.uniportal.ui.components.relativeTimeText
import com.example.uniportal.ui.components.titleRes
import com.example.uniportal.ui.preview.PreviewData
import com.example.uniportal.ui.theme.UniPortalTheme
import kotlinx.coroutines.launch

@Composable
fun UniversityListRoute(
    onUniversityClick: (String) -> Unit,
    viewModel: UniversityListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UniversityListScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onUniversityClick = onUniversityClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversityListScreen(
    state: UniversityListUiState,
    onEvent: (UniversityListEvent) -> Unit,
    onUniversityClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val refreshFailedText = stringResource(R.string.refresh_error)
    val loadMoreOfflineText = stringResource(R.string.load_more_offline)
    val settingsUnavailableText = stringResource(R.string.settings_unavailable)
    val retryText = stringResource(R.string.action_retry)
    val removedText = stringResource(R.string.favourites_removed)
    val undoText = stringResource(R.string.action_undo)
    val currentOnEvent by rememberUpdatedState(onEvent)
    val currentState by rememberUpdatedState(state)
    val onToggleFavourite: (University) -> Unit = { university ->

        val leavesList = currentState.favouritesOnly && university.id in currentState.favouriteIds
        currentOnEvent(UniversityListEvent.ToggleFavourite(university))
        if (leavesList) {
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = removedText,
                    actionLabel = undoText,
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    currentOnEvent(UniversityListEvent.RestoreFavourite(university))
                }
            }
        }
    }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        val result = when (message) {
            SearchMessage.REFRESH_FAILED -> snackbarHostState.showSnackbar(
                message = refreshFailedText,
                actionLabel = retryText,
                duration = SnackbarDuration.Long,
            )
            SearchMessage.LOAD_MORE_OFFLINE -> snackbarHostState.showSnackbar(
                message = loadMoreOfflineText,
                duration = SnackbarDuration.Short,
            )
            SearchMessage.SETTINGS_UNAVAILABLE -> snackbarHostState.showSnackbar(
                message = settingsUnavailableText,
                duration = SnackbarDuration.Short,
            )
        }
        currentOnEvent(UniversityListEvent.MessageShown)
        if (message == SearchMessage.REFRESH_FAILED && result == SnackbarResult.ActionPerformed) {
            currentOnEvent(UniversityListEvent.Refresh)
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,

        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { PortalSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            UniversityListTopBar(
                scrollBehavior = scrollBehavior,
                onRefresh = { onEvent(UniversityListEvent.Refresh) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())

                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            SearchField(
                query = state.query,
                onQueryChange = { onEvent(UniversityListEvent.QueryChanged(it)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            CountryFilterRow(
                selected = state.country,
                onSelect = { onEvent(UniversityListEvent.CountrySelected(it)) },
                contentPadding = PaddingValues(horizontal = 16.dp),
                leadingChip = {
                    FavouritesOnlyChip(
                        selected = state.favouritesOnly,
                        onSelectedChange = { onEvent(UniversityListEvent.FavouritesOnlyChanged(it)) },
                    )
                },
            )
            SearchBannerSlot(
                banner = state.banner,
                onRetry = { onEvent(UniversityListEvent.Retry) },
            )
            Spacer(Modifier.height(8.dp))

            val pullToRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(UniversityListEvent.Refresh) },
                modifier = Modifier.fillMaxSize(),
                state = pullToRefreshState,
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullToRefreshState,
                        isRefreshing = state.isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            ) {
                ListBody(
                    state = state,
                    bottomInset = innerPadding.calculateBottomPadding(),
                    onEvent = onEvent,
                    onUniversityClick = onUniversityClick,
                    onToggleFavourite = onToggleFavourite,
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(LIST_TOP_PADDING)
                        .background(
                            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, Color.Transparent)),
                        ),
                )

                if (state.isUpdating) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag(TestTags.UPDATING_INDICATOR),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UniversityListTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    onRefresh: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PortalLogo()
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.list_subtitle),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onRefresh, modifier = Modifier.testTag(TestTags.REFRESH_BUTTON)) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.cd_refresh),
                )
            }
        },

        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),

        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
        ),
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun SearchBannerSlot(
    banner: SearchBanner?,
    onRetry: () -> Unit,
) {
    AnimatedContent(
        targetState = banner,
        modifier = Modifier.fillMaxWidth(),
        transitionSpec = { fadeIn(tween(CONTENT_FADE_MS)) togetherWith fadeOut(tween(CONTENT_FADE_MS)) },
        label = "searchBanner",

        contentKey = { current -> current?.let { it::class } },
    ) { current ->
        if (current != null) {
            SearchBannerView(
                banner = current,
                onRetry = onRetry,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )
        }
    }
}

@Composable
private fun SearchBannerView(
    banner: SearchBanner,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val retryText = stringResource(R.string.action_retry)
    when (banner) {
        is SearchBanner.Offline -> {
            val now = rememberCurrentTimeMillis()
            OfflineBanner(
                text = stringResource(R.string.search_banner_offline, relativeTimeText(banner.fetchedAt, now)),
                tone = BannerTone.WARNING,
                modifier = modifier,
            )
        }
        is SearchBanner.RefreshFailed -> {
            val now = rememberCurrentTimeMillis()
            val textRes = if (banner.error == ErrorType.NETWORK) {
                R.string.search_banner_no_connection
            } else {
                R.string.search_banner_refresh_failed
            }
            OfflineBanner(
                text = stringResource(textRes, relativeTimeText(banner.fetchedAt, now)),
                tone = BannerTone.WARNING,
                modifier = modifier,
                actionLabel = retryText,
                onAction = onRetry,
            )
        }
        is SearchBanner.LocalCatalog -> OfflineBanner(
            text = stringResource(
                if (banner.isOnline) R.string.search_banner_local_catalog else R.string.search_banner_local_catalog_offline,
            ),
            tone = BannerTone.INFO,
            modifier = modifier,

            actionLabel = if (banner.isOnline) retryText else null,
            onAction = if (banner.isOnline) onRetry else null,
        )
    }
}

@Composable
private fun ListBody(
    state: UniversityListUiState,
    bottomInset: Dp,
    onEvent: (UniversityListEvent) -> Unit,
    onUniversityClick: (String) -> Unit,
    onToggleFavourite: (University) -> Unit,
) {
    AnimatedContent(
        targetState = state.content,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = { fadeIn(tween(CONTENT_FADE_MS)) togetherWith fadeOut(tween(CONTENT_FADE_MS)) },
        label = "listContent",

        contentKey = { it::class },
    ) { content ->
        val stateModifier = Modifier.padding(bottom = bottomInset)
        when (content) {
            ListContent.Loading -> LoadingState(modifier = stateModifier, showAfterMillis = LOADING_SPINNER_DELAY_MS)
            is ListContent.Error -> ErrorState(
                type = content.type,
                onRetry = { onEvent(UniversityListEvent.Retry) },
                modifier = stateModifier,
            )
            ListContent.Empty -> if (state.allHiddenByFavouritesOnly) {
                EmptyState(
                    emoji = "💫",
                    title = stringResource(R.string.empty_favourites_only_title),
                    message = stringResource(
                        if (state.canLoadMore) {
                            R.string.empty_favourites_only_message_more
                        } else {
                            R.string.empty_favourites_only_message
                        },
                    ),
                    modifier = stateModifier,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (state.canLoadMore) {
                            LoadMoreFooter(
                                isLoadingMore = state.isLoadingMore,
                                canLoadMore = true,
                                loadMoreError = state.loadMoreError,
                                onLoadMore = { onEvent(UniversityListEvent.LoadMore) },
                            )
                        }
                        ShowAllButton(onClick = { onEvent(UniversityListEvent.FavouritesOnlyChanged(false)) })
                    }
                }
            } else {
                EmptyState(
                    emoji = "🔍",
                    title = stringResource(R.string.empty_search_title),
                    message = stringResource(R.string.empty_search_message),
                    modifier = stateModifier,
                    action = if (state.hasActiveFilters) {
                        { ResetFiltersButton(onClick = { onEvent(UniversityListEvent.ClearFilters) }) }
                    } else {
                        null
                    },
                )
            }
            is ListContent.Success -> UniversityList(
                items = content.items,
                favouriteIds = state.favouriteIds,
                applicationStatuses = state.applicationStatuses,
                shortlistCounts = state.shortlistCounts,
                favouritesOnly = state.favouritesOnly,
                isLoadingMore = state.isLoadingMore,
                canLoadMore = state.canLoadMore,
                loadMoreError = state.loadMoreError,
                isLocalCatalog = state.source == ResultsSource.LOCAL_CATALOG,
                resultsVersion = state.resultsVersion,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = LIST_TOP_PADDING,
                    end = 16.dp,
                    bottom = 16.dp + bottomInset,
                ),
                onUniversityClick = onUniversityClick,
                onToggleFavourite = onToggleFavourite,
                onLoadMore = { onEvent(UniversityListEvent.LoadMore) },
            )
        }
    }
}

@Composable
private fun UniversityList(
    items: List<University>,
    favouriteIds: Set<String>,
    applicationStatuses: Map<String, ApplicationStatus>,
    shortlistCounts: Map<String, Int>,
    favouritesOnly: Boolean,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    loadMoreError: ErrorType?,
    isLocalCatalog: Boolean,
    resultsVersion: Int,
    contentPadding: PaddingValues,
    onUniversityClick: (String) -> Unit,
    onToggleFavourite: (University) -> Unit,
    onLoadMore: () -> Unit,
) {
    val listState = rememberSaveable(resultsVersion, saver = LazyListState.Saver) { LazyListState() }
    val focusManager = LocalFocusManager.current
    val isScrolling = listState.isScrollInProgress
    LaunchedEffect(isScrolling) {
        if (isScrolling) focusManager.clearFocus()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag(TestTags.UNIVERSITY_LIST),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = items, key = { it.id }) { university ->
            UniversityCard(
                university = university,
                isFavourite = university.id in favouriteIds,
                onClick = { onUniversityClick(university.id) },
                onToggleFavourite = { onToggleFavourite(university) },
                modifier = Modifier.animateItem(),
                applicationStatus = applicationStatuses[university.id],
                shortlistCount = shortlistCounts[university.id] ?: 0,
            )
        }
        item(key = FOOTER_KEY) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (favouritesOnly && canLoadMore) {
                    Text(
                        text = stringResource(R.string.favourites_only_footer),
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                LoadMoreFooter(
                    isLoadingMore = isLoadingMore,
                    canLoadMore = canLoadMore,
                    loadMoreError = loadMoreError,
                    onLoadMore = onLoadMore,
                    listEndText = stringResource(
                        when {
                            isLocalCatalog -> R.string.local_catalog_list_end
                            favouritesOnly -> R.string.favourites_only_list_end
                            else -> R.string.list_end
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun LoadMoreFooter(
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    loadMoreError: ErrorType?,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    listEndText: String = stringResource(R.string.list_end),
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)

            .heightIn(min = FOOTER_CONTENT_MIN_HEIGHT),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isLoadingMore -> CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
            )
            loadMoreError != null -> LoadMoreError(type = loadMoreError, onRetry = onLoadMore)
            canLoadMore -> FilledTonalButton(
                onClick = onLoadMore,
                modifier = Modifier.testTag(TestTags.LOAD_MORE_BUTTON),
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(text = stringResource(R.string.action_load_more))
            }
            else -> Text(
                text = listEndText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LoadMoreError(type: ErrorType, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.load_more_error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(type.titleRes()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRetry, modifier = Modifier.testTag(TestTags.LOAD_MORE_RETRY_BUTTON)) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(text = stringResource(R.string.action_retry))
        }
    }
}

@Composable
private fun ShowAllButton(onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(text = stringResource(R.string.action_show_all))
    }
}

@Composable
private fun ResetFiltersButton(onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.Clear,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(text = stringResource(R.string.action_reset_filters))
    }
}

private const val FOOTER_KEY = "load_more_footer"
private const val CONTENT_FADE_MS = 220
private const val LOADING_SPINNER_DELAY_MS = 300L
private val LIST_TOP_PADDING = 8.dp
private val FOOTER_CONTENT_MIN_HEIGHT = 48.dp

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun UniversityListScreenPreview() {
    UniPortalTheme {
        UniversityListScreen(
            state = UniversityListUiState(
                query = "state",
                country = CountryFilter.ALL,
                favouriteIds = setOf(PreviewData.mtsu.id),
                applicationStatuses = mapOf(PreviewData.mtsu.id to ApplicationStatus.SUBMITTED),
                shortlistCounts = mapOf(PreviewData.msu.id to 2),
                content = ListContent.Success(PreviewData.universities),
                source = ResultsSource.CACHE,
                banner = SearchBanner.Offline(fetchedAt = System.currentTimeMillis() - PREVIEW_AGE_MS),
                canLoadMore = true,
            ),
            onEvent = {},
            onUniversityClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun UniversityListScreenLocalCatalogPreview() {
    UniPortalTheme {
        UniversityListScreen(
            state = UniversityListUiState(
                country = CountryFilter.RUSSIA,
                content = ListContent.Success(listOf(PreviewData.msu)),
                source = ResultsSource.LOCAL_CATALOG,
                banner = SearchBanner.LocalCatalog(isOnline = false),
            ),
            onEvent = {},
            onUniversityClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun UniversityListScreenErrorPreview() {
    UniPortalTheme {
        UniversityListScreen(
            state = UniversityListUiState(content = ListContent.Error(ErrorType.NETWORK)),
            onEvent = {},
            onUniversityClick = {},
        )
    }
}

private const val PREVIEW_AGE_MS = 25 * 60 * 1000L
