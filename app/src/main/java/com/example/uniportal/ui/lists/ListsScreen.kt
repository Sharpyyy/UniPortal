package com.example.uniportal.ui.lists

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniportal.R
import com.example.uniportal.domain.model.Shortlist
import com.example.uniportal.domain.model.University
import com.example.uniportal.ui.components.EmojiBadge
import com.example.uniportal.ui.components.EmptyState
import com.example.uniportal.ui.components.ErrorState
import com.example.uniportal.ui.components.LoadingState
import com.example.uniportal.ui.components.PortalSnackbarHost
import com.example.uniportal.ui.components.TestTags
import com.example.uniportal.ui.components.rememberCurrentTimeMillis
import com.example.uniportal.ui.components.relativeTimeText
import com.example.uniportal.ui.favourites.FavouritesContent
import com.example.uniportal.ui.favourites.FavouritesEvent
import com.example.uniportal.ui.favourites.FavouritesPage
import com.example.uniportal.ui.favourites.FavouritesUiState
import com.example.uniportal.ui.favourites.FavouritesViewModel
import com.example.uniportal.ui.preview.PreviewData
import com.example.uniportal.ui.shortlist.ShortlistFormDialog
import com.example.uniportal.ui.theme.UniPortalTheme
import kotlinx.coroutines.launch

@Composable
fun ListsRoute(
    selectedTab: ListsTab,
    onTabSelected: (ListsTab) -> Unit,
    onUniversityClick: (String) -> Unit,
    onShortlistClick: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    favouritesViewModel: FavouritesViewModel = hiltViewModel(),
    shortlistsViewModel: ShortlistsViewModel = hiltViewModel(),
) {
    val favouritesState by favouritesViewModel.uiState.collectAsStateWithLifecycle()
    val shortlistsState by shortlistsViewModel.uiState.collectAsStateWithLifecycle()
    ListsScreen(
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        favouritesState = favouritesState,
        onFavouritesEvent = favouritesViewModel::onEvent,
        shortlistsState = shortlistsState,
        onShortlistsEvent = shortlistsViewModel::onEvent,
        onUniversityClick = onUniversityClick,
        onShortlistClick = onShortlistClick,
        onOpenSearch = onOpenSearch,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(
    selectedTab: ListsTab,
    onTabSelected: (ListsTab) -> Unit,
    favouritesState: FavouritesUiState,
    onFavouritesEvent: (FavouritesEvent) -> Unit,
    shortlistsState: ShortlistsUiState,
    onShortlistsEvent: (ShortlistsEvent) -> Unit,
    onUniversityClick: (String) -> Unit,
    onShortlistClick: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val removedText = stringResource(R.string.favourites_removed)
    val undoText = stringResource(R.string.action_undo)
    val openText = stringResource(R.string.action_open)
    val saveFailedText = stringResource(R.string.error_save_failed)
    val currentOnFavouritesEvent by rememberUpdatedState(onFavouritesEvent)
    val currentOnShortlistsEvent by rememberUpdatedState(onShortlistsEvent)
    val currentOnShortlistClick by rememberUpdatedState(onShortlistClick)

    val onRemoveFavourite: (University) -> Unit = { university ->
        currentOnFavouritesEvent(FavouritesEvent.Remove(university))
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = removedText,
                actionLabel = undoText,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                currentOnFavouritesEvent(FavouritesEvent.Restore(university))
            }
        }
    }

    val message = shortlistsState.message
    val createdText = (message as? ShortlistsMessage.Created)?.let { stringResource(R.string.shortlist_created, it.name) }
    LaunchedEffect(message) {
        when (message) {
            null -> return@LaunchedEffect
            is ShortlistsMessage.Created -> {
                val result = snackbarHostState.showSnackbar(
                    message = createdText.orEmpty(),
                    actionLabel = openText,
                    duration = SnackbarDuration.Short,
                )
                currentOnShortlistsEvent(ShortlistsEvent.MessageShown)
                if (result == SnackbarResult.ActionPerformed) currentOnShortlistClick(message.id)
            }
            ShortlistsMessage.SaveFailed -> {
                snackbarHostState.showSnackbar(message = saveFailedText, duration = SnackbarDuration.Short)
                currentOnShortlistsEvent(ShortlistsEvent.MessageShown)
            }
        }
    }

    shortlistsState.form?.let { form ->
        ShortlistFormDialog(
            state = form,
            onEvent = { onShortlistsEvent(ShortlistsEvent.Form(it)) },
        )
    }

    val pageStates = rememberSaveableStateHolder()
    val showFab = selectedTab == ListsTab.SHORTLISTS &&
        shortlistsState.content !is ShortlistsContent.Loading &&
        shortlistsState.content !is ShortlistsContent.Error

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,

        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { PortalSnackbarHost(hostState = snackbarHostState) },
        topBar = { ListsTopBar() },
        floatingActionButton = {
            AnimatedVisibility(visible = showFab, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                ExtendedFloatingActionButton(
                    text = { Text(text = stringResource(R.string.action_new_shortlist)) },
                    icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                    onClick = { onShortlistsEvent(ShortlistsEvent.OpenCreateForm) },
                    modifier = Modifier.testTag(TestTags.NEW_SHORTLIST_BUTTON),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())

                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            ListsTabRow(
                selectedTab = selectedTab,
                favouritesCount = favouritesState.totalCount,
                shortlistsCount = shortlistsState.shortlistCount,
                onTabSelected = onTabSelected,
            )
            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = { fadeIn(tween(TAB_FADE_MS)) togetherWith fadeOut(tween(TAB_FADE_MS)) },
                label = "listsTab",
            ) { tab ->
                pageStates.SaveableStateProvider(tab.name) {
                    when (tab) {
                        ListsTab.FAVOURITES -> FavouritesPage(
                            state = favouritesState,
                            onEvent = onFavouritesEvent,
                            onUniversityClick = onUniversityClick,
                            onRemove = onRemoveFavourite,
                            onOpenSearch = onOpenSearch,
                            bottomInset = innerPadding.calculateBottomPadding(),
                        )
                        ListsTab.SHORTLISTS -> ShortlistsPage(
                            state = shortlistsState,
                            bottomInset = innerPadding.calculateBottomPadding(),
                            onEvent = onShortlistsEvent,
                            onShortlistClick = onShortlistClick,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListsTopBar() {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = stringResource(R.string.lists_title),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.lists_subtitle),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },

        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

@Composable
private fun ListsTabRow(
    selectedTab: ListsTab,
    favouritesCount: Int,
    shortlistsCount: Int,
    onTabSelected: (ListsTab) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    TabRow(
        selectedTabIndex = selectedTab.ordinal,
        modifier = Modifier.testTag(TestTags.LISTS_TAB_ROW),
        containerColor = colors.background,
        contentColor = colors.primary,
        divider = { HorizontalDivider(color = colors.outlineVariant) },
    ) {
        ListsTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            val count = when (tab) {
                ListsTab.FAVOURITES -> favouritesCount
                ListsTab.SHORTLISTS -> shortlistsCount
            }
            Tab(
                selected = selected,
                onClick = { onTabSelected(tab) },
                modifier = Modifier.testTag(TestTags.listsTab(tab.name)),
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(tab.labelRes()),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (count > 0) {
                            Badge(
                                containerColor = if (selected) colors.primaryContainer else colors.surfaceContainerHigh,
                                contentColor = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                            ) {
                                Text(text = count.toString())
                            }
                        }
                    }
                },
                selectedContentColor = colors.primary,
                unselectedContentColor = colors.onSurfaceVariant,
            )
        }
    }
}

private fun ListsTab.labelRes(): Int = when (this) {
    ListsTab.FAVOURITES -> R.string.lists_tab_favourites
    ListsTab.SHORTLISTS -> R.string.lists_tab_shortlists
}

@Composable
private fun ShortlistsPage(
    state: ShortlistsUiState,
    bottomInset: Dp,
    onEvent: (ShortlistsEvent) -> Unit,
    onShortlistClick: (Long) -> Unit,
) {
    AnimatedContent(
        targetState = state.content,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = { fadeIn(tween(CONTENT_FADE_MS)) togetherWith fadeOut(tween(CONTENT_FADE_MS)) },
        label = "shortlistsContent",

        contentKey = { it::class },
    ) { content ->
        val stateModifier = Modifier.padding(bottom = bottomInset)
        when (content) {
            ShortlistsContent.Loading -> LoadingState(
                modifier = stateModifier,
                showAfterMillis = LOADING_SPINNER_DELAY_MS,
            )
            is ShortlistsContent.Error -> ErrorState(
                type = content.type,
                onRetry = { onEvent(ShortlistsEvent.Retry) },
                modifier = stateModifier,
            )
            ShortlistsContent.Empty -> EmptyState(
                emoji = "🗂️",
                title = stringResource(R.string.shortlists_empty_title),
                message = stringResource(R.string.shortlists_empty_message),

                modifier = stateModifier.padding(bottom = FAB_CLEARANCE),
            )
            is ShortlistsContent.Success -> ShortlistsGrid(
                shortlists = content.shortlists,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = bottomInset + FAB_CLEARANCE,
                ),
                onShortlistClick = onShortlistClick,
            )
        }
    }
}

@Composable
private fun ShortlistsGrid(
    shortlists: List<Shortlist>,
    contentPadding: PaddingValues,
    onShortlistClick: (Long) -> Unit,
) {
    val now = rememberCurrentTimeMillis()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = SHORTLIST_CARD_MIN_WIDTH),
        modifier = Modifier
            .fillMaxSize()
            .testTag(TestTags.SHORTLISTS_GRID),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = shortlists, key = { it.id }) { shortlist ->
            ShortlistCard(
                shortlist = shortlist,
                now = now,
                onClick = { onShortlistClick(shortlist.id) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun ShortlistCard(
    shortlist: Shortlist,
    now: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.shortlistCard(shortlist.id)),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            EmojiBadge(emoji = shortlist.emoji, seed = shortlist.id.hashCode())
            Spacer(Modifier.height(12.dp))
            Text(
                text = shortlist.name,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(R.plurals.universities_count, shortlist.itemCount, shortlist.itemCount),
                style = MaterialTheme.typography.labelMedium,
                color = colors.primary,
                maxLines = 1,
            )
            Spacer(Modifier.height(8.dp))

            Text(
                text = shortlist.description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                minLines = DESCRIPTION_LINES,
                maxLines = DESCRIPTION_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.shortlist_updated_at, relativeTimeText(shortlist.updatedAt, now)),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private const val TAB_FADE_MS = 180
private const val CONTENT_FADE_MS = 220
private const val LOADING_SPINNER_DELAY_MS = 300L
private const val DESCRIPTION_LINES = 2
private val SHORTLIST_CARD_MIN_WIDTH = 160.dp

private val FAB_CLEARANCE = 88.dp

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun ListsScreenShortlistsPreview() {
    UniPortalTheme {
        ListsScreen(
            selectedTab = ListsTab.SHORTLISTS,
            onTabSelected = {},
            favouritesState = FavouritesUiState(
                totalCount = PreviewData.universities.size,
                content = FavouritesContent.Success(PreviewData.universities),
            ),
            onFavouritesEvent = {},
            shortlistsState = ShortlistsUiState(content = ShortlistsContent.Success(PreviewData.shortlists)),
            onShortlistsEvent = {},
            onUniversityClick = {},
            onShortlistClick = {},
            onOpenSearch = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun ListsScreenFavouritesPreview() {
    UniPortalTheme {
        ListsScreen(
            selectedTab = ListsTab.FAVOURITES,
            onTabSelected = {},
            favouritesState = FavouritesUiState(
                totalCount = PreviewData.universities.size,
                content = FavouritesContent.Success(PreviewData.universities),
            ),
            onFavouritesEvent = {},
            shortlistsState = ShortlistsUiState(content = ShortlistsContent.Empty),
            onShortlistsEvent = {},
            onUniversityClick = {},
            onShortlistClick = {},
            onOpenSearch = {},
        )
    }
}
