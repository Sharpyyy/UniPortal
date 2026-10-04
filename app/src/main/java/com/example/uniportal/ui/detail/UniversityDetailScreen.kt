package com.example.uniportal.ui.detail

import android.content.ActivityNotFoundException
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniportal.R
import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.ErrorType
import com.example.uniportal.domain.model.Shortlist
import com.example.uniportal.domain.model.University
import com.example.uniportal.ui.components.CountryBadge
import com.example.uniportal.ui.components.DeadlineChip
import com.example.uniportal.ui.components.ErrorState
import com.example.uniportal.ui.components.FavouriteButton
import com.example.uniportal.ui.components.LoadingState
import com.example.uniportal.ui.components.PortalRing
import com.example.uniportal.ui.components.PortalSnackbarHost
import com.example.uniportal.ui.components.SectionCard
import com.example.uniportal.ui.components.StatusPill
import com.example.uniportal.ui.components.TestTags
import com.example.uniportal.ui.components.TintedPill
import com.example.uniportal.ui.components.UniversityAvatar
import com.example.uniportal.ui.components.countryLabel
import com.example.uniportal.ui.components.formatFullDay
import com.example.uniportal.ui.components.labelRes
import com.example.uniportal.ui.components.priorityLabelRes
import com.example.uniportal.ui.components.rememberCurrentTimeMillis
import com.example.uniportal.ui.components.relativeTimeText
import com.example.uniportal.ui.preview.PreviewData
import com.example.uniportal.ui.theme.PortalTheme
import com.example.uniportal.ui.theme.UniPortalTheme
import com.example.uniportal.util.displayUrl
import com.example.uniportal.util.flagEmoji
import com.example.uniportal.util.withHttpScheme
import kotlinx.coroutines.launch

@Composable
fun UniversityDetailRoute(
    onBack: () -> Unit,
    onEditApplication: (String) -> Unit,
    viewModel: UniversityDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UniversityDetailScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onEditApplication = { onEditApplication(viewModel.universityId) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversityDetailScreen(
    state: DetailUiState,
    onEvent: (DetailEvent) -> Unit,
    onBack: () -> Unit,
    onEditApplication: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val openLinkError = stringResource(R.string.error_open_link)
    val openLink: (String) -> Unit = { url ->
        val opened = try {
            uriHandler.openUri(url.withHttpScheme())
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
        if (!opened) {
            scope.launch { snackbarHostState.showSnackbar(openLinkError) }
        }
    }
    val messageText = state.message?.let { detailMessageText(it) }
    val currentOnEvent by rememberUpdatedState(onEvent)
    LaunchedEffect(state.message) {
        if (messageText == null) return@LaunchedEffect
        snackbarHostState.showSnackbar(message = messageText, duration = SnackbarDuration.Short)
        currentOnEvent(DetailEvent.MessageShown)
    }

    val university = (state.content as? DetailContent.Success)?.university
    state.shortlistDialog?.let { dialog ->
        ShortlistPickerDialog(
            shortlists = state.shortlists,
            state = dialog,
            onToggle = { onEvent(DetailEvent.ShortlistToggled(it)) },
            onNameChange = { onEvent(DetailEvent.NewShortlistNameChanged(it)) },
            onCreate = { onEvent(DetailEvent.CreateShortlist) },
            onConfirm = { onEvent(DetailEvent.ConfirmShortlists) },
            onDismiss = { onEvent(DetailEvent.DismissShortlistDialog) },
        )
    }

    val contentScrollState = rememberScrollState()
    val density = LocalDensity.current

    val barFadePx = with(density) { TOP_BAR_FADE_DISTANCE.toPx() }
    val barColor = MaterialTheme.colorScheme.surface

    val titleThresholdPx = with(density) { TITLE_SCROLL_THRESHOLD.toPx() }
    val showBarTitle by remember(titleThresholdPx) {
        derivedStateOf { contentScrollState.value > titleThresholdPx }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { PortalSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (university != null) {
                        AnimatedVisibility(visible = showBarTitle, enter = fadeIn(), exit = fadeOut()) {
                            Text(
                                text = university.name,
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
                    if (university != null) {
                        FavouriteButton(
                            isFavourite = state.isFavourite,
                            onClick = { onEvent(DetailEvent.ToggleFavourite) },
                            modifier = Modifier.testTag(TestTags.DETAIL_FAVOURITE_BUTTON),
                        )
                    }
                },
                modifier = Modifier.drawBehind {
                    val fraction = (contentScrollState.value / barFadePx).coerceIn(0f, 1f)
                    if (fraction > 0f) drawRect(color = barColor, alpha = fraction)
                },

                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = state.content,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { fadeIn(tween(CONTENT_FADE_MS)) togetherWith fadeOut(tween(CONTENT_FADE_MS)) },
            label = "detailContent",
            contentKey = { it::class },
        ) { content ->
            when (content) {
                DetailContent.Loading -> LoadingState(
                    modifier = Modifier.padding(innerPadding),
                    showAfterMillis = LOADING_SPINNER_DELAY_MS,
                )
                is DetailContent.Error -> ErrorState(
                    type = content.type,
                    onRetry = { onEvent(DetailEvent.Retry) },
                    modifier = Modifier.padding(innerPadding),

                    secondaryAction = if (content.type == ErrorType.NOT_FOUND) {
                        {
                            TextButton(onClick = onBack) {
                                Text(text = stringResource(R.string.action_go_back))
                            }
                        }
                    } else {
                        null
                    },
                )
                is DetailContent.Success -> UniversityDetailContent(
                    university = content.university,
                    state = state,
                    scrollState = contentScrollState,
                    contentPadding = innerPadding,
                    onOpenLink = openLink,
                    onEvent = onEvent,
                    onEditApplication = onEditApplication,
                )
            }
        }
    }
}

@Composable
private fun detailMessageText(message: DetailMessage): String = when (message) {
    is DetailMessage.ShortlistsSaved -> if (message.count == 0) {
        stringResource(R.string.detail_shortlists_cleared)
    } else {
        pluralStringResource(R.plurals.detail_shortlists_saved, message.count, message.count)
    }
    is DetailMessage.StatusChanged -> stringResource(
        R.string.detail_status_changed,
        stringResource(message.status.labelRes()),
    )
    DetailMessage.SaveFailed -> stringResource(R.string.error_save_failed)
    DetailMessage.SettingsUnavailable -> stringResource(R.string.settings_unavailable)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UniversityDetailContent(
    university: University,
    state: DetailUiState,
    scrollState: ScrollState,
    contentPadding: PaddingValues,
    onOpenLink: (String) -> Unit,
    onEvent: (DetailEvent) -> Unit,
    onEditApplication: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {
        DetailHero(
            university = university,
            state = state,
            topInset = contentPadding.calculateTopPadding(),
            onOpenLink = onOpenLink,
            onOpenShortlists = { onEvent(DetailEvent.OpenShortlistDialog) },
            onRetry = { onEvent(DetailEvent.Retry) },
        )
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 24.dp + contentPadding.calculateBottomPadding(),
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ApplicationSection(
                summary = state.application,
                onEditApplication = onEditApplication,
                onAdvanceStatus = { onEvent(DetailEvent.AdvanceStatus) },
            )
            ShortlistsSection(
                shortlists = state.memberShortlists,
                onEdit = { onEvent(DetailEvent.OpenShortlistDialog) },
            )
            WebsitesSection(webPages = university.webPages, onOpenLink = onOpenLink)
            DomainsSection(domains = university.domains)
            CountryCodeSection(country = university.country, countryCode = university.countryCode)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailHero(
    university: University,
    state: DetailUiState,
    topInset: Dp,
    onOpenLink: (String) -> Unit,
    onOpenShortlists: () -> Unit,
    onRetry: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(colors.primaryContainer, colors.background)))

            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(start = 24.dp, end = 24.dp, top = topInset + 8.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PortalRing {
            UniversityAvatar(id = university.id, name = university.name, size = 96.dp)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = university.name,
            modifier = Modifier.testTag(TestTags.DETAIL_TITLE),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
        )
        if (university.country.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            CountryBadge(country = university.country, countryCode = university.countryCode)
        }
        university.stateProvince?.let { region ->
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = region,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        SyncCaption(
            fetchedAt = state.fetchedAt,
            isRefreshing = state.isRefreshing,
            refreshFailed = state.refreshFailed,
            onRetry = onRetry,
        )
        Spacer(Modifier.height(20.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            university.webPages.firstOrNull()?.let { url ->
                Button(onClick = { onOpenLink(url) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(R.string.detail_open_website))
                }
            }

            val memberCount = state.memberShortlistIds.size
            FilledTonalButton(
                onClick = onOpenShortlists,
                modifier = Modifier.testTag(TestTags.DETAIL_SHORTLIST_BUTTON),
                colors = if (memberCount > 0) {
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = colors.tertiaryContainer,
                        contentColor = colors.onTertiaryContainer,
                    )
                } else {
                    ButtonDefaults.filledTonalButtonColors()
                },
            ) {
                Icon(
                    imageVector = if (memberCount > 0) Icons.Filled.Check else Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(
                    text = if (memberCount > 0) {
                        pluralStringResource(R.plurals.shortlist_mark, memberCount, memberCount)
                    } else {
                        stringResource(R.string.detail_add_to_shortlist)
                    },
                )
            }
        }
    }
}

@Composable
private fun SyncCaption(
    fetchedAt: Long?,
    isRefreshing: Boolean,
    refreshFailed: Boolean,
    onRetry: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.heightIn(min = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            isRefreshing -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    color = colors.primary,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.detail_refreshing),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            fetchedAt != null -> {
                val now = rememberCurrentTimeMillis()
                Text(
                    text = stringResource(R.string.detail_updated_at, relativeTimeText(fetchedAt, now)),
                    modifier = Modifier.testTag(TestTags.DETAIL_UPDATED_AT),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        if (refreshFailed && !isRefreshing) {
            val warning = PortalTheme.colors.warning
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = warning,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.detail_refresh_failed),
                    style = MaterialTheme.typography.labelMedium,
                    color = warning,
                )
                TextButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.textButtonColors(contentColor = warning),
                ) {
                    Text(text = stringResource(R.string.action_retry))
                }
            }
        }
    }
}

@Composable
private fun ApplicationSection(
    summary: ApplicationSummary?,
    onEditApplication: () -> Unit,
    onAdvanceStatus: () -> Unit,
) {
    SectionCard(
        title = stringResource(R.string.detail_section_application),
        icon = Icons.Filled.DateRange,
        modifier = Modifier.testTag(TestTags.DETAIL_APPLICATION_CARD),
        action = if (summary != null) {
            {
                IconButton(
                    onClick = onEditApplication,
                    modifier = Modifier.testTag(TestTags.DETAIL_EDIT_APPLICATION_BUTTON),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.cd_edit_application),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            null
        },
    ) {
        if (summary == null) {
            Text(
                text = stringResource(R.string.detail_application_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onEditApplication,
                modifier = Modifier.testTag(TestTags.DETAIL_ADD_APPLICATION_BUTTON),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(text = stringResource(R.string.detail_add_application))
            }
        } else {
            ApplicationDetails(summary = summary, onAdvanceStatus = onAdvanceStatus)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ApplicationDetails(
    summary: ApplicationSummary,
    onAdvanceStatus: () -> Unit,
) {
    val application = summary.application
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusPill(status = application.status)
        PriorityPill(priority = application.priority)
    }
    Spacer(Modifier.height(12.dp))
    val deadline = application.deadline
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.DateRange,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (deadline != null) {
                stringResource(R.string.detail_deadline, deadline.formatFullDay())
            } else {
                stringResource(R.string.detail_no_deadline)
            },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    val daysLeft = summary.daysLeft
    if (daysLeft != null) {
        Spacer(Modifier.height(8.dp))
        DeadlineChip(
            daysLeft = daysLeft,
            isOverdue = summary.isOverdue,
            isActive = application.status.isActive,
        )
    }
    if (application.note.isNotBlank()) {
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text(
                text = application.note,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = NOTE_PREVIEW_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    summary.nextStatus?.let { next ->
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(
            onClick = onAdvanceStatus,
            modifier = Modifier.testTag(TestTags.DETAIL_NEXT_STATUS_BUTTON),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(
                text = stringResource(R.string.detail_next_status, stringResource(next.labelRes())),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PriorityPill(priority: Int) {
    TintedPill(color = MaterialTheme.colorScheme.onSurfaceVariant) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.priority_pill, stringResource(priorityLabelRes(priority))),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShortlistsSection(
    shortlists: List<Shortlist>,
    onEdit: () -> Unit,
) {
    SectionCard(
        title = stringResource(R.string.detail_section_shortlists),
        icon = Icons.AutoMirrored.Filled.List,
        action = {
            TextButton(onClick = onEdit) {
                Text(
                    text = stringResource(
                        if (shortlists.isEmpty()) R.string.action_add else R.string.action_change,
                    ),
                )
            }
        },
    ) {
        if (shortlists.isEmpty()) {
            Text(
                text = stringResource(R.string.detail_shortlists_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                shortlists.forEach { shortlist ->
                    TintedPill(color = MaterialTheme.colorScheme.tertiary) {
                        Text(
                            text = shortlist.emoji,
                            modifier = Modifier.clearAndSetSemantics {},
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = shortlist.name,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WebsitesSection(webPages: List<String>, onOpenLink: (String) -> Unit) {
    SectionCard(title = stringResource(R.string.detail_section_websites), icon = Icons.Filled.Home) {
        if (webPages.isEmpty()) {
            Text(
                text = stringResource(R.string.detail_no_websites),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        webPages.forEachIndexed { index, url ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = DIVIDER_ALPHA))
            }
            LinkRow(url = url, onClick = { onOpenLink(url) })
        }
    }
}

@Composable
private fun LinkRow(url: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(
                onClickLabel = stringResource(R.string.detail_open_website),
                role = Role.Button,
                onClick = onClick,
            )

            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = url.displayUrl(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DomainsSection(domains: List<String>) {
    SectionCard(title = stringResource(R.string.detail_section_domains), icon = Icons.Filled.Info) {
        SelectionContainer {
            Column {
                domains.forEach { domain ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(MaterialTheme.colorScheme.secondary, CircleShape),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = domain,
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountryCodeSection(country: String, countryCode: String) {
    SectionCard(title = stringResource(R.string.detail_section_country_code), icon = Icons.Filled.Place) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = flagEmoji(countryCode),
                modifier = Modifier.clearAndSetSemantics {},

                fontSize = with(LocalDensity.current) { COUNTRY_FLAG_SIZE.toSp() },
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = countryCode.ifBlank { "—" },
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                )

                val label = countryLabel(country, countryCode)
                if (label.isNotBlank()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (country.isNotBlank() && !country.equals(label, ignoreCase = true)) {
                    Text(
                        text = country,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private const val CONTENT_FADE_MS = 220
private const val LOADING_SPINNER_DELAY_MS = 300L
private const val DIVIDER_ALPHA = 0.5f
private const val NOTE_PREVIEW_LINES = 3

private val TOP_BAR_FADE_DISTANCE = 120.dp
private val COUNTRY_FLAG_SIZE = 32.dp

private val TITLE_SCROLL_THRESHOLD = 150.dp

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14, heightDp = 1400)
@Composable
private fun UniversityDetailScreenPreview() {
    UniPortalTheme {
        UniversityDetailScreen(
            state = DetailUiState(
                content = DetailContent.Success(PreviewData.mtsu),
                fetchedAt = System.currentTimeMillis() - PREVIEW_AGE_MS,
                isFavourite = true,
                application = ApplicationSummary(
                    application = PreviewData.application,
                    daysLeft = 5,
                    isOverdue = false,
                    nextStatus = ApplicationStatus.SUBMITTED,
                ),
                shortlists = PreviewData.shortlists,
                memberShortlistIds = setOf(PreviewData.shortlists.first().id),
            ),
            onEvent = {},
            onBack = {},
            onEditApplication = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14, heightDp = 1200)
@Composable
private fun UniversityDetailScreenNoApplicationPreview() {
    UniPortalTheme {
        UniversityDetailScreen(
            state = DetailUiState(
                content = DetailContent.Success(PreviewData.msu),
                fetchedAt = System.currentTimeMillis() - PREVIEW_AGE_MS,
                refreshFailed = true,
            ),
            onEvent = {},
            onBack = {},
            onEditApplication = {},
        )
    }
}

private const val PREVIEW_AGE_MS = 3 * 60 * 60 * 1000L
