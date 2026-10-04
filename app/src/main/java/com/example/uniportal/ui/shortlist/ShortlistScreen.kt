package com.example.uniportal.ui.shortlist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniportal.R
import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.Shortlist
import com.example.uniportal.domain.model.ShortlistDetails
import com.example.uniportal.domain.model.ShortlistItem
import com.example.uniportal.ui.components.BackToSearchButton
import com.example.uniportal.ui.components.EmojiBadge
import com.example.uniportal.ui.components.EmptyState
import com.example.uniportal.ui.components.ErrorState
import com.example.uniportal.ui.components.LoadingState
import com.example.uniportal.ui.components.PortalSnackbarHost
import com.example.uniportal.ui.components.StatusPill
import com.example.uniportal.ui.components.TestTags
import com.example.uniportal.ui.components.TintedPill
import com.example.uniportal.ui.components.UniversityCard
import com.example.uniportal.ui.components.rememberCurrentTimeMillis
import com.example.uniportal.ui.components.relativeTimeText
import com.example.uniportal.ui.preview.PreviewData
import com.example.uniportal.ui.theme.UniPortalTheme
import kotlinx.coroutines.launch

@Composable
fun ShortlistRoute(
    onUniversityClick: (String) -> Unit,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: ShortlistViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) currentOnBack()
    }
    ShortlistScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onUniversityClick = onUniversityClick,
        onBack = onBack,
        onOpenSearch = onOpenSearch,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortlistScreen(
    state: ShortlistUiState,
    onEvent: (ShortlistEvent) -> Unit,
    onUniversityClick: (String) -> Unit,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val removedText = stringResource(R.string.shortlist_item_removed)
    val undoText = stringResource(R.string.action_undo)
    val currentOnEvent by rememberUpdatedState(onEvent)
    val onRemove: (ShortlistItem) -> Unit = { item ->
        currentOnEvent(ShortlistEvent.RemoveItem(item))
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = removedText,
                actionLabel = undoText,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                currentOnEvent(ShortlistEvent.RestoreItem(item))
            }
        }
    }
    val messageText = when (state.message) {
        ShortlistMessage.Saved -> stringResource(R.string.shortlist_saved)
        ShortlistMessage.SaveFailed -> stringResource(R.string.error_save_failed)
        null -> null
    }
    LaunchedEffect(state.message) {
        if (messageText == null) return@LaunchedEffect
        snackbarHostState.showSnackbar(message = messageText, duration = SnackbarDuration.Short)
        currentOnEvent(ShortlistEvent.MessageShown)
    }

    val details = (state.content as? ShortlistContent.Success)?.details
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    if (showDeleteDialog && details != null) {
        DeleteShortlistDialog(
            name = details.shortlist.name,
            onConfirm = {
                showDeleteDialog = false
                onEvent(ShortlistEvent.Delete)
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
    state.form?.let { form ->
        ShortlistFormDialog(state = form, onEvent = { onEvent(ShortlistEvent.Form(it)) })
    }

    val listState = rememberLazyListState()

    val showBarTitle by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { PortalSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (details != null) {
                        AnimatedVisibility(visible = showBarTitle, enter = fadeIn(), exit = fadeOut()) {
                            Text(
                                text = details.shortlist.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
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
                    if (details != null) {
                        IconButton(
                            onClick = { onEvent(ShortlistEvent.OpenEditForm) },
                            enabled = !state.isDeleting,
                            modifier = Modifier.testTag(TestTags.SHORTLIST_EDIT_BUTTON),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.cd_edit_shortlist),
                            )
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !state.isDeleting,
                            modifier = Modifier.testTag(TestTags.SHORTLIST_DELETE_BUTTON),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.cd_delete_shortlist),
                            )
                        }
                    }
                },

                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = state.content,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { fadeIn(tween(CONTENT_FADE_MS)) togetherWith fadeOut(tween(CONTENT_FADE_MS)) },
            label = "shortlistContent",

            contentKey = { it::class },
        ) { content ->
            when (content) {
                ShortlistContent.Loading -> LoadingState(
                    modifier = Modifier.padding(innerPadding),
                    showAfterMillis = LOADING_SPINNER_DELAY_MS,
                )
                is ShortlistContent.Error -> ErrorState(
                    type = content.type,
                    onRetry = { onEvent(ShortlistEvent.Retry) },
                    modifier = Modifier.padding(innerPadding),
                )
                ShortlistContent.NotFound -> EmptyState(
                    emoji = "🕳️",
                    title = stringResource(R.string.shortlist_not_found_title),
                    message = stringResource(R.string.shortlist_not_found_message),
                    modifier = Modifier.padding(innerPadding),
                ) {
                    TextButton(onClick = onBack) {
                        Text(text = stringResource(R.string.action_go_back))
                    }
                }
                is ShortlistContent.Success -> ShortlistBody(
                    details = content.details,
                    applicationStatuses = state.applicationStatuses,
                    listState = listState,
                    contentPadding = innerPadding,
                    onUniversityClick = onUniversityClick,
                    onRemove = onRemove,
                    onOpenSearch = onOpenSearch,
                )
            }
        }
    }
}

@Composable
private fun ShortlistBody(
    details: ShortlistDetails,
    applicationStatuses: Map<String, ApplicationStatus>,
    listState: LazyListState,
    contentPadding: PaddingValues,
    onUniversityClick: (String) -> Unit,
    onRemove: (ShortlistItem) -> Unit,
    onOpenSearch: () -> Unit,
) {
    val now = rememberCurrentTimeMillis()
    val sideInsets = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    if (details.items.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(sideInsets)
                .padding(
                    top = contentPadding.calculateTopPadding() + 8.dp,
                    bottom = contentPadding.calculateBottomPadding(),
                ),
        ) {
            ShortlistHero(
                shortlist = details.shortlist,
                itemCount = 0,
                now = now,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            EmptyState(
                emoji = "🧺",
                title = stringResource(R.string.shortlist_empty_title),
                message = stringResource(R.string.shortlist_empty_message),
                modifier = Modifier.weight(1f),
            ) {
                BackToSearchButton(onClick = onOpenSearch)
            }
        }
    } else {
        ShortlistItemsList(
            details = details,
            applicationStatuses = applicationStatuses,
            listState = listState,
            contentPadding = contentPadding,
            now = now,
            sideInsets = sideInsets,
            onUniversityClick = onUniversityClick,
            onRemove = onRemove,
        )
    }
}

@Composable
private fun ShortlistItemsList(
    details: ShortlistDetails,
    applicationStatuses: Map<String, ApplicationStatus>,
    listState: LazyListState,
    contentPadding: PaddingValues,
    now: Long,
    sideInsets: Modifier,
    onUniversityClick: (String) -> Unit,
    onRemove: (ShortlistItem) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .then(sideInsets)
            .testTag(TestTags.SHORTLIST_ITEMS_LIST),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            end = 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = HERO_KEY) {
            ShortlistHero(shortlist = details.shortlist, itemCount = details.items.size, now = now)
        }
        items(items = details.items, key = { it.university.id }) { item ->
            val university = item.university
            val status = applicationStatuses[university.id]
            UniversityCard(
                university = university,
                onClick = { onUniversityClick(university.id) },
                modifier = Modifier.animateItem(),
                caption = stringResource(R.string.shortlist_item_added, relativeTimeText(item.addedAt, now)),
                marks = if (status != null) {
                    { StatusPill(status = status, modifier = Modifier.testTag(TestTags.statusPill(university.id))) }
                } else {
                    null
                },
            ) {
                IconButton(
                    onClick = { onRemove(item) },
                    modifier = Modifier.testTag(TestTags.removeFromShortlist(university.id)),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_remove_from_shortlist),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShortlistHero(
    shortlist: Shortlist,
    itemCount: Int,
    now: Long,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainer,
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(listOf(colors.tertiaryContainer.copy(alpha = HERO_TINT_ALPHA), Color.Transparent)),
                )
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBadge(emoji = shortlist.emoji, size = HERO_EMOJI_SIZE, seed = shortlist.id.hashCode())
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shortlist.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.onSurface,
                    )
                    Text(
                        text = pluralStringResource(R.plurals.universities_count, itemCount, itemCount),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                    )
                }
            }
            if (shortlist.description.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = shortlist.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TintedPill(color = colors.secondary) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.shortlist_available_offline),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
                TintedPill(color = colors.onSurfaceVariant) {
                    Icon(imageVector = Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.shortlist_updated_at, relativeTimeText(shortlist.updatedAt, now)),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteShortlistDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Filled.Delete, contentDescription = null) },
        title = { Text(text = stringResource(R.string.shortlist_delete_title)) },
        text = { Text(text = stringResource(R.string.shortlist_delete_message, name)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(TestTags.CONFIRM_DELETE_BUTTON),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(text = stringResource(R.string.action_delete))
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

private const val HERO_KEY = "shortlist:hero"
private const val CONTENT_FADE_MS = 220
private const val LOADING_SPINNER_DELAY_MS = 300L
private const val HERO_TINT_ALPHA = 0.55f
private val HERO_EMOJI_SIZE = 64.dp

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun ShortlistScreenPreview() {
    UniPortalTheme {
        ShortlistScreen(
            state = ShortlistUiState(
                content = ShortlistContent.Success(PreviewData.shortlistDetails),
                applicationStatuses = mapOf(PreviewData.mtsu.id to ApplicationStatus.PREPARING),
            ),
            onEvent = {},
            onUniversityClick = {},
            onBack = {},
            onOpenSearch = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun ShortlistScreenEmptyPreview() {
    UniPortalTheme {
        ShortlistScreen(
            state = ShortlistUiState(
                content = ShortlistContent.Success(
                    PreviewData.shortlistDetails.copy(items = emptyList()),
                ),
            ),
            onEvent = {},
            onUniversityClick = {},
            onBack = {},
            onOpenSearch = {},
        )
    }
}
