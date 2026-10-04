package com.example.uniportal.ui.history

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniportal.R
import com.example.uniportal.domain.model.HistoryEntry
import com.example.uniportal.ui.components.BackToSearchButton
import com.example.uniportal.ui.components.EmptyState
import com.example.uniportal.ui.components.ErrorState
import com.example.uniportal.ui.components.LoadingState
import com.example.uniportal.ui.components.PortalSnackbarHost
import com.example.uniportal.ui.components.TestTags
import com.example.uniportal.ui.components.TitleWithCount
import com.example.uniportal.ui.components.UniversityCard
import com.example.uniportal.ui.preview.PreviewData
import com.example.uniportal.ui.theme.UniPortalTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun HistoryRoute(
    onUniversityClick: (String) -> Unit,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onUniversityClick = onUniversityClick,
        onBack = onBack,
        onOpenSearch = onOpenSearch,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onEvent: (HistoryEvent) -> Unit,
    onUniversityClick: (String) -> Unit,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val removedText = stringResource(R.string.history_removed)
    val undoText = stringResource(R.string.action_undo)
    val currentOnEvent by rememberUpdatedState(onEvent)
    val onRemove: (HistoryEntry) -> Unit = { entry ->
        currentOnEvent(HistoryEvent.Remove(entry.university.id))
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = removedText,
                actionLabel = undoText,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                currentOnEvent(HistoryEvent.Restore(entry))
            }
        }
    }

    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    if (showClearDialog) {
        ClearHistoryDialog(
            onConfirm = {
                showClearDialog = false

                snackbarHostState.currentSnackbarData?.dismiss()
                onEvent(HistoryEvent.ClearAll)
            },
            onDismiss = { showClearDialog = false },
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { PortalSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            HistoryTopBar(
                entriesCount = (state.content as? HistoryContent.Success)?.entries?.size ?: 0,
                scrollBehavior = scrollBehavior,
                onClear = { showClearDialog = true },
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = state.content,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { fadeIn(tween(CONTENT_FADE_MS)) togetherWith fadeOut(tween(CONTENT_FADE_MS)) },
            label = "historyContent",

            contentKey = { it::class },
        ) { current ->
            when (current) {
                HistoryContent.Loading -> LoadingState(
                    modifier = Modifier.padding(innerPadding),
                    showAfterMillis = LOADING_SPINNER_DELAY_MS,
                )
                is HistoryContent.Error -> ErrorState(
                    type = current.type,
                    onRetry = { onEvent(HistoryEvent.Retry) },
                    modifier = Modifier.padding(innerPadding),
                )
                HistoryContent.Empty -> EmptyState(
                    emoji = "🕰️",
                    title = stringResource(R.string.history_empty_title),
                    message = pluralStringResource(R.plurals.history_empty_message, state.limit, state.limit),
                    modifier = Modifier.padding(innerPadding),
                ) {
                    BackToSearchButton(onClick = onOpenSearch)
                }
                is HistoryContent.Success -> HistoryList(
                    entries = current.entries,
                    limit = state.limit,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        end = 16.dp,
                        bottom = innerPadding.calculateBottomPadding() + 16.dp,
                    ),
                    onUniversityClick = onUniversityClick,
                    onRemove = onRemove,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTopBar(
    entriesCount: Int,
    scrollBehavior: TopAppBarScrollBehavior,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    LargeTopAppBar(
        title = {
            TitleWithCount(
                title = stringResource(R.string.history_title),
                count = entriesCount,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                )
            }
        },
        actions = {
            IconButton(onClick = onClear, enabled = entriesCount > 0) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.cd_clear_history),
                )
            }
        },

        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
        ),
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun HistoryList(
    entries: List<HistoryEntry>,
    limit: Int,
    contentPadding: PaddingValues,
    onUniversityClick: (String) -> Unit,
    onRemove: (HistoryEntry) -> Unit,
) {
    val listState = rememberLazyListState()

    val newestId = entries.firstOrNull()?.university?.id
    var shownNewestId by rememberSaveable { mutableStateOf(newestId) }
    LaunchedEffect(newestId) {
        if (newestId != shownNewestId) {
            shownNewestId = newestId
            listState.scrollToItem(0)
        }
    }

    val zone = remember { ZoneId.systemDefault() }

    val days = remember(entries, zone) {
        entries.groupBy { entry -> Instant.ofEpochMilli(entry.viewedAt).atZone(zone).toLocalDate() }
    }
    val today = LocalDate.now(zone)

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()

            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .testTag(TestTags.HISTORY_LIST),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = LIMIT_CAPTION_KEY, contentType = CONTENT_TYPE_CAPTION) {
            Text(
                text = pluralStringResource(R.plurals.history_limit_caption, limit, limit),
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        days.forEach { (day, dayEntries) ->

            item(key = "day:$day", contentType = CONTENT_TYPE_DAY) {
                DayHeader(day = day, today = today, modifier = Modifier.animateItem())
            }
            items(
                items = dayEntries,
                key = { entry -> entry.university.id },
                contentType = { CONTENT_TYPE_ENTRY },
            ) { entry ->
                val university = entry.university
                UniversityCard(
                    university = university,
                    onClick = { onUniversityClick(university.id) },
                    modifier = Modifier.animateItem(),
                    caption = stringResource(
                        R.string.history_viewed_at,
                        Instant.ofEpochMilli(entry.viewedAt).atZone(zone).format(TIME_FORMATTER),
                    ),
                ) {
                    IconButton(onClick = { onRemove(entry) }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_remove_from_history),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(
    day: LocalDate,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val text = when (day) {
        today -> stringResource(R.string.history_day_today)
        today.minusDays(1) -> stringResource(R.string.history_day_yesterday)
        else -> day.format(if (day.year == today.year) DAY_FORMATTER else DAY_WITH_YEAR_FORMATTER)
    }
    Text(
        text = text,
        modifier = modifier
            .padding(start = 4.dp, top = 8.dp)
            .semantics { heading() },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ClearHistoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Filled.Delete, contentDescription = null) },
        title = { Text(text = stringResource(R.string.history_clear_title)) },
        text = { Text(text = stringResource(R.string.history_clear_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(text = stringResource(R.string.action_clear))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        iconContentColor = MaterialTheme.colorScheme.error,
    )
}

private val RUSSIAN = Locale("ru")
private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", RUSSIAN)
private val DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", RUSSIAN)
private val DAY_WITH_YEAR_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", RUSSIAN)

private const val LIMIT_CAPTION_KEY = "history:limit"
private const val CONTENT_TYPE_CAPTION = "caption"
private const val CONTENT_TYPE_DAY = "day"
private const val CONTENT_TYPE_ENTRY = "entry"
private const val CONTENT_FADE_MS = 220
private const val LOADING_SPINNER_DELAY_MS = 300L

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun HistoryScreenPreview() {
    UniPortalTheme {
        HistoryScreen(
            state = HistoryUiState(content = HistoryContent.Success(PreviewData.history)),
            onEvent = {},
            onUniversityClick = {},
            onBack = {},
            onOpenSearch = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun HistoryScreenEmptyPreview() {
    UniPortalTheme {
        HistoryScreen(
            state = HistoryUiState(content = HistoryContent.Empty),
            onEvent = {},
            onUniversityClick = {},
            onBack = {},
            onOpenSearch = {},
        )
    }
}
