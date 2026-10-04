package com.example.uniportal.ui.applications

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniportal.R
import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.rules.ApplicationRules
import com.example.uniportal.ui.components.BackToSearchButton
import com.example.uniportal.ui.components.DeadlineChip
import com.example.uniportal.ui.components.EmptyState
import com.example.uniportal.ui.components.ErrorState
import com.example.uniportal.ui.components.LoadingState
import com.example.uniportal.ui.components.PortalSnackbarHost
import com.example.uniportal.ui.components.TestTags
import com.example.uniportal.ui.components.TintedPill
import com.example.uniportal.ui.components.UniversityAvatar
import com.example.uniportal.ui.components.accentColor
import com.example.uniportal.ui.components.emoji
import com.example.uniportal.ui.components.formatDay
import com.example.uniportal.ui.components.labelRes
import com.example.uniportal.ui.preview.PreviewData
import com.example.uniportal.ui.theme.PortalTheme
import com.example.uniportal.ui.theme.UniPortalTheme
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun ApplicationsRoute(
    onUniversityClick: (String) -> Unit,
    onEditApplication: (String) -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: ApplicationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ApplicationsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onUniversityClick = onUniversityClick,
        onEditApplication = onEditApplication,
        onOpenSearch = onOpenSearch,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationsScreen(
    state: ApplicationsUiState,
    onEvent: (ApplicationsEvent) -> Unit,
    onUniversityClick: (String) -> Unit,
    onEditApplication: (String) -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val undoText = stringResource(R.string.action_undo)
    val statusChangedTexts = ApplicationStatus.entries.associateWith { status ->
        stringResource(R.string.detail_status_changed, stringResource(status.labelRes()))
    }
    val statusChangeFailedText = stringResource(R.string.applications_status_change_failed)
    val settingsUnavailableText = stringResource(R.string.settings_unavailable)
    val currentOnEvent by rememberUpdatedState(onEvent)

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(
            message = when (message) {
                ApplicationsMessage.STATUS_CHANGE_FAILED -> statusChangeFailedText
                ApplicationsMessage.SETTINGS_UNAVAILABLE -> settingsUnavailableText
            },
            duration = SnackbarDuration.Long,
        )
        currentOnEvent(ApplicationsEvent.MessageShown)
    }

    val onChangeStatus: (ApplicationItem, ApplicationStatus) -> Unit = { item, status ->
        val previous = item.application.status
        currentOnEvent(ApplicationsEvent.ChangeStatus(item.universityId, status))
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = statusChangedTexts.getValue(status),
                actionLabel = undoText,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                currentOnEvent(ApplicationsEvent.ChangeStatus(item.universityId, previous))
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { PortalSnackbarHost(hostState = snackbarHostState) },
        topBar = { ApplicationsTopBar(scrollBehavior = scrollBehavior) },
    ) { innerPadding ->
        AnimatedContent(
            targetState = state.content,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { fadeIn(tween(CONTENT_FADE_MS)) togetherWith fadeOut(tween(CONTENT_FADE_MS)) },
            label = "applicationsContent",

            contentKey = { it.kind() },
        ) { content ->
            when (content) {
                ApplicationsContent.Loading -> LoadingState(
                    modifier = Modifier.padding(innerPadding),
                    showAfterMillis = LOADING_SPINNER_DELAY_MS,
                )
                is ApplicationsContent.Error -> ErrorState(
                    type = content.type,
                    onRetry = { onEvent(ApplicationsEvent.Retry) },
                    modifier = Modifier.padding(innerPadding),
                )
                ApplicationsContent.Empty -> EmptyState(
                    emoji = "🗓️",
                    title = stringResource(R.string.applications_empty_title),
                    message = stringResource(R.string.applications_empty_message),
                    modifier = Modifier.padding(innerPadding),
                ) {
                    BackToSearchButton(onClick = onOpenSearch)
                }
                ApplicationsContent.NoMatches, is ApplicationsContent.Success -> ApplicationsDashboard(
                    state = state,
                    sections = (content as? ApplicationsContent.Success)?.sections.orEmpty(),
                    contentPadding = innerPadding,
                    onSelectFilter = { onEvent(ApplicationsEvent.FilterSelected(it)) },
                    onOpenEditor = onEditApplication,
                    onOpenUniversity = onUniversityClick,
                    onChangeStatus = onChangeStatus,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplicationsTopBar(scrollBehavior: TopAppBarScrollBehavior) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = stringResource(R.string.applications_title),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.applications_subtitle),
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
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun ApplicationsDashboard(
    state: ApplicationsUiState,
    sections: List<ApplicationSection>,
    contentPadding: PaddingValues,
    onSelectFilter: (ApplicationStatus?) -> Unit,
    onOpenEditor: (String) -> Unit,
    onOpenUniversity: (String) -> Unit,
    onChangeStatus: (ApplicationItem, ApplicationStatus) -> Unit,
) {
    val itemPadding = Modifier.padding(horizontal = 16.dp)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()

            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .testTag(TestTags.APPLICATIONS_LIST),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = SUMMARY_KEY) {
            SummaryCard(
                summary = state.summary,
                today = state.today,
                onOpenNearest = onOpenEditor,
                modifier = itemPadding,
            )
        }
        item(key = FILTERS_KEY) {
            StatusFilterRow(
                selected = state.statusFilter,
                counts = state.summary.countByStatus,
                total = state.summary.total,
                onSelect = onSelectFilter,
            )
        }
        if (sections.isEmpty()) {
            item(key = NO_MATCHES_KEY) {
                NoMatchesMessage(onShowAll = { onSelectFilter(null) }, modifier = itemPadding)
            }
        }
        sections.forEach { section ->

            item(key = "section:${section.status.name}") {
                SectionHeader(section = section, modifier = Modifier.animateItem().then(itemPadding))
            }
            items(items = section.items, key = { it.universityId }) { item ->
                ApplicationCard(
                    item = item,
                    today = state.today,
                    onClick = { onOpenEditor(item.universityId) },
                    onOpenUniversity = { onOpenUniversity(item.universityId) },
                    onChangeStatus = { status -> onChangeStatus(item, status) },
                    modifier = Modifier.animateItem().then(itemPadding),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryCard(
    summary: ApplicationsSummary,
    today: LocalDate,
    onOpenNearest: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.APPLICATIONS_SUMMARY),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainer,
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(listOf(colors.primaryContainer.copy(alpha = SUMMARY_TINT_ALPHA), Color.Transparent)),
                )
                .padding(16.dp),
        ) {
            Row {
                Text(
                    text = summary.total.toString(),
                    modifier = Modifier.alignByBaseline(),
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = pluralStringResource(R.plurals.applications_word, summary.total),
                    modifier = Modifier.alignByBaseline(),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
            }
            if (summary.overdueCount > 0 || summary.dueSoonCount > 0) {
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (summary.overdueCount > 0) {
                        TintedPill(color = colors.error) {
                            Icon(imageVector = Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.applications_overdue, summary.overdueCount),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                            )
                        }
                    }
                    if (summary.dueSoonCount > 0) {
                        TintedPill(color = PortalTheme.colors.warning) {
                            Icon(imageVector = Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.applications_due_soon, summary.dueSoonCount),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            PipelineBar(counts = summary.countByStatus)
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ApplicationStatus.entries.forEach { status ->
                    val count = summary.countByStatus[status] ?: 0
                    if (count > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(status.accentColor(), CircleShape),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.label_with_count, stringResource(status.labelRes()), count),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = colors.outlineVariant)
            Spacer(Modifier.height(12.dp))
            NearestDeadline(nearest = summary.nearest, today = today, onOpen = onOpenNearest)
        }
    }
}

@Composable
private fun PipelineBar(
    counts: Map<ApplicationStatus, Int>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PIPELINE_HEIGHT)
            .clip(CircleShape),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ApplicationStatus.entries.forEach { status ->
            val count = counts[status] ?: 0
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .weight(count.toFloat())
                        .fillMaxHeight()
                        .background(status.accentColor()),
                )
            }
        }
    }
}

@Composable
private fun NearestDeadline(
    nearest: ApplicationItem?,
    today: LocalDate,
    onOpen: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    if (nearest == null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colors.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.applications_no_upcoming),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    } else {
        val application = nearest.application
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(role = Role.Button, onClick = { onOpen(nearest.universityId) })
                .testTag(TestTags.APPLICATIONS_NEAREST_DEADLINE)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colors.onSecondaryContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.applications_nearest_deadline),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = application.university.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                application.deadline?.let { deadline ->
                    Text(
                        text = deadline.formatDay(today),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            nearest.daysLeft?.let { daysLeft ->
                Spacer(Modifier.width(8.dp))
                DeadlineChip(
                    daysLeft = daysLeft,
                    isOverdue = nearest.isOverdue,
                    isActive = application.status.isActive,
                )
            }
        }
    }
}

@Composable
private fun StatusFilterRow(
    selected: ApplicationStatus?,
    counts: Map<ApplicationStatus, Int>,
    total: Int,
    onSelect: (ApplicationStatus?) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val options: List<ApplicationStatus?> =
        listOf<ApplicationStatus?>(null) + ApplicationStatus.entries.filter { (counts[it] ?: 0) > 0 || it == selected }
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = options, key = { it?.name ?: ALL_FILTER_KEY }) { status ->
            val isSelected = status == selected
            val count = if (status == null) total else counts[status] ?: 0
            val label = if (status == null) {
                stringResource(R.string.applications_filter_all)
            } else {
                stringResource(status.labelRes())
            }
            val accent = status?.accentColor() ?: colors.primary
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(status) },
                label = { Text(text = stringResource(R.string.label_with_count, label, count)) },
                modifier = Modifier.testTag(TestTags.statusFilter(status)),
                leadingIcon = if (status != null) {
                    {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(accent, CircleShape),
                        )
                    }
                } else {
                    null
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = colors.surfaceContainer,
                    labelColor = colors.onSurfaceVariant,
                    selectedContainerColor = accent.copy(alpha = SELECTED_CHIP_ALPHA),
                    selectedLabelColor = colors.onSurface,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = colors.outline,
                    selectedBorderColor = accent,
                    selectedBorderWidth = 1.dp,
                ),
            )
        }
    }
}

@Composable
private fun SectionHeader(
    section: ApplicationSection,
    modifier: Modifier = Modifier,
) {
    val accent = section.status.accentColor()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .semantics { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = section.status.emoji(),
            modifier = Modifier.clearAndSetSemantics {},
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(section.status.labelRes()),
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.titleSmall,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        TintedPill(color = accent) {
            Text(
                text = section.items.size.toString(),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ApplicationCard(
    item: ApplicationItem,
    today: LocalDate,
    onClick: () -> Unit,
    onOpenUniversity: () -> Unit,
    onChangeStatus: (ApplicationStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val application = item.application
    val university = application.university
    val deadline = application.deadline
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.applicationItem(university.id)),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (item.isOverdue) {
                lerp(colors.surfaceContainer, colors.error, OVERDUE_TINT_FRACTION)
            } else {
                colors.surfaceContainer
            },
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (item.isOverdue) colors.error.copy(alpha = OVERDUE_BORDER_ALPHA) else colors.outline,
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            UniversityAvatar(id = university.id, name = university.name, size = AVATAR_SIZE)
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = university.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StatusMenuPill(
                        status = application.status,
                        onSelect = onChangeStatus,
                        modifier = Modifier.testTag(TestTags.applicationStatusButton(university.id)),
                    )
                    item.daysLeft?.let { daysLeft ->
                        DeadlineChip(
                            daysLeft = daysLeft,
                            isOverdue = item.isOverdue,
                            isActive = application.status.isActive,
                        )
                    }
                    if (application.priority >= ApplicationRules.MAX_PRIORITY) {
                        TintedPill(color = PortalTheme.colors.warning) {
                            Icon(imageVector = Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.applications_high_priority),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                            )
                        }
                    }
                }
                Text(
                    text = if (deadline != null) {
                        stringResource(R.string.applications_deadline_on, deadline.formatDay(today))
                    } else {
                        stringResource(R.string.applications_no_deadline)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.isOverdue) colors.error else colors.onSurfaceVariant,
                )
                if (application.note.isNotBlank()) {
                    Text(
                        text = application.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onOpenUniversity) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = stringResource(R.string.cd_open_university),
                    tint = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusMenuPill(
    status: ApplicationStatus,
    onSelect: (ApplicationStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val accent = status.accentColor()
    Box {
        TintedPill(
            color = accent,
            modifier = modifier
                .clip(CircleShape)
                .clickable(
                    onClickLabel = stringResource(R.string.cd_change_status),
                    role = Role.Button,
                    onClick = { expanded = true },
                ),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(accent, CircleShape),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(status.labelRes()),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Text(
                text = stringResource(R.string.applications_status_menu_title),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ApplicationStatus.entries.forEach { option ->
                val optionAccent = option.accentColor()
                DropdownMenuItem(
                    text = { Text(text = stringResource(option.labelRes())) },
                    onClick = {
                        expanded = false
                        if (option != status) onSelect(option)
                    },
                    modifier = Modifier.testTag(TestTags.statusMenuItem(option)),
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(optionAccent, CircleShape),
                        )
                    },
                    trailingIcon = if (option == status) {
                        { Icon(imageVector = Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun NoMatchesMessage(
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "🔎", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.applications_no_matches_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.applications_no_matches_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onShowAll) {
            Text(text = stringResource(R.string.action_show_all))
        }
    }
}

private fun ApplicationsContent.kind(): Any = when (this) {
    is ApplicationsContent.Success, ApplicationsContent.NoMatches -> DASHBOARD_KIND
    else -> this::class
}

private const val DASHBOARD_KIND = "dashboard"
private const val SUMMARY_KEY = "applications:summary"
private const val FILTERS_KEY = "applications:filters"
private const val NO_MATCHES_KEY = "applications:no_matches"
private const val ALL_FILTER_KEY = "ALL"
private const val CONTENT_FADE_MS = 220
private const val LOADING_SPINNER_DELAY_MS = 300L
private const val SUMMARY_TINT_ALPHA = 0.5f
private const val SELECTED_CHIP_ALPHA = 0.18f
private const val OVERDUE_TINT_FRACTION = 0.07f
private const val OVERDUE_BORDER_ALPHA = 0.7f
private val PIPELINE_HEIGHT = 8.dp
private val AVATAR_SIZE = 44.dp

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14, heightDp = 1100)
@Composable
private fun ApplicationsScreenPreview() {
    val today = LocalDate.now()
    val items = PreviewData.applications.map { application ->
        ApplicationItem(
            application = application,
            daysLeft = ApplicationRules.daysLeft(application, today),
            isOverdue = ApplicationRules.isOverdue(application, today),
            isDueSoon = ApplicationRules.needsReminder(application, today, 7),
        )
    }
    UniPortalTheme {
        ApplicationsScreen(
            state = ApplicationsUiState(
                content = ApplicationsContent.Success(
                    items.groupBy { it.application.status }.map { (status, group) -> ApplicationSection(status, group) },
                ),
                summary = ApplicationsSummary(
                    total = items.size,
                    countByStatus = ApplicationRules.countByStatus(PreviewData.applications),
                    overdueCount = items.count { it.isOverdue },
                    dueSoonCount = items.count { it.isDueSoon },
                    nearest = items.first(),
                ),
                today = today,
            ),
            onEvent = {},
            onUniversityClick = {},
            onEditApplication = {},
            onOpenSearch = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun ApplicationsScreenEmptyPreview() {
    UniPortalTheme {
        ApplicationsScreen(
            state = ApplicationsUiState(content = ApplicationsContent.Empty),
            onEvent = {},
            onUniversityClick = {},
            onEditApplication = {},
            onOpenSearch = {},
        )
    }
}
