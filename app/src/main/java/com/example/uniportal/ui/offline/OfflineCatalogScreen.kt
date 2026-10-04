package com.example.uniportal.ui.offline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniportal.R
import com.example.uniportal.domain.model.CountryFilter
import com.example.uniportal.ui.components.BannerTone
import com.example.uniportal.ui.components.LoadingState
import com.example.uniportal.ui.components.OfflineBanner
import com.example.uniportal.ui.components.TestTags
import com.example.uniportal.ui.components.countryLabel
import com.example.uniportal.ui.components.labelRes
import com.example.uniportal.ui.components.rememberCurrentTimeMillis
import com.example.uniportal.ui.components.relativeTimeText
import com.example.uniportal.ui.preview.PreviewData
import com.example.uniportal.ui.theme.UniPortalTheme
import com.example.uniportal.util.flagEmoji
import com.example.uniportal.work.DownloadState

@Composable
fun OfflineCatalogRoute(
    onBack: () -> Unit,
    viewModel: OfflineCatalogViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OfflineCatalogScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineCatalogScreen(
    state: OfflineCatalogUiState,
    onEvent: (OfflineCatalogEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingRemoval by rememberSaveable { mutableStateOf<String?>(null) }
    val removal = pendingRemoval?.let { name -> state.downloaded.firstOrNull { it.country.countryName == name } }
    if (removal != null) {
        val country = removal.country
        RemoveCountryDialog(
            countryName = countryLabel(country.countryName, country.countryCode),
            onConfirm = {
                pendingRemoval = null
                onEvent(OfflineCatalogEvent.Remove(country.countryName))
            },
            onDismiss = { pendingRemoval = null },
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.offline_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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

                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingState(modifier = Modifier.padding(innerPadding), showAfterMillis = LOADING_SPINNER_DELAY_MS)
        } else {
            OfflineCatalogList(
                state = state,
                contentPadding = innerPadding,
                onEvent = onEvent,
                onRemove = { countryName -> pendingRemoval = countryName },
            )
        }
    }
}

@Composable
private fun OfflineCatalogList(
    state: OfflineCatalogUiState,
    contentPadding: PaddingValues,
    onEvent: (OfflineCatalogEvent) -> Unit,
    onRemove: (String) -> Unit,
) {
    val now = rememberCurrentTimeMillis()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()

            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .testTag(TestTags.OFFLINE_LIST),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            end = 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "offline:intro") {
            IntroCard(cachedCount = state.cachedCount, isOnline = state.isOnline)
        }
        item(key = "offline:downloaded_title") {
            GroupTitle(text = stringResource(R.string.offline_section_downloaded))
        }
        if (state.downloaded.isEmpty()) {
            item(key = "offline:no_downloads") {
                MutedText(text = stringResource(R.string.offline_no_downloads))
            }
        }
        items(items = state.downloaded, key = { "downloaded:" + it.country.countryName }) { item ->
            val filter = item.filter
            DownloadedCountryCard(
                item = item,
                now = now,
                onUpdate = if (filter != null) {
                    { onEvent(OfflineCatalogEvent.Download(filter)) }
                } else {
                    null
                },
                onCancel = if (filter != null) {
                    { onEvent(OfflineCatalogEvent.CancelDownload(filter)) }
                } else {
                    null
                },
                onRemove = { onRemove(item.country.countryName) },
                modifier = Modifier.animateItem(),
            )
        }
        item(key = "offline:available_title") {
            GroupTitle(text = stringResource(R.string.offline_section_available))
        }
        if (state.available.isEmpty()) {
            item(key = "offline:all_downloaded") {
                MutedText(text = stringResource(R.string.offline_all_downloaded))
            }
        }
        items(items = state.available, key = { "available:" + it.filter.name }) { item ->
            AvailableCountryCard(
                item = item,
                onDownload = { onEvent(OfflineCatalogEvent.Download(item.filter)) },
                onCancel = { onEvent(OfflineCatalogEvent.CancelDownload(item.filter)) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun IntroCard(
    cachedCount: Int,
    isOnline: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainer,
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(listOf(colors.secondaryContainer.copy(alpha = INTRO_TINT_ALPHA), Color.Transparent)),
                )
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(colors.secondary.copy(alpha = ICON_CIRCLE_ALPHA), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = null,
                        tint = colors.secondary,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.offline_intro_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                    )
                    Text(
                        text = stringResource(
                            R.string.offline_on_device,
                            pluralStringResource(R.plurals.universities_count, cachedCount, cachedCount),
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.secondary,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.offline_intro_message),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            AnimatedVisibility(visible = !isOnline) {
                OfflineBanner(
                    text = stringResource(R.string.offline_no_network),
                    tone = BannerTone.WARNING,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun DownloadedCountryCard(
    item: DownloadedCountry,
    now: Long,
    onUpdate: (() -> Unit)?,
    onCancel: (() -> Unit)?,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val country = item.country
    val download = item.download
    val isActive = download?.isActive == true
    CountryCard(tag = TestTags.offlineCountry(country.countryName), modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlagBadge(flag = flagEmoji(country.countryCode))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = countryLabel(country.countryName, country.countryCode),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.offline_country_meta,
                        pluralStringResource(R.plurals.universities_count, country.universityCount, country.universityCount),
                        relativeTimeText(country.downloadedAt, now),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            if (isActive && onCancel != null) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_cancel_download),
                        tint = colors.onSurfaceVariant,
                    )
                }
            } else if (onUpdate != null) {
                IconButton(onClick = onUpdate) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.cd_update_country),
                        tint = colors.primary,
                    )
                }
            }
            IconButton(onClick = onRemove, enabled = !isActive) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.cd_remove_country),
                    tint = if (isActive) colors.onSurfaceVariant.copy(alpha = DISABLED_ALPHA) else colors.error,
                )
            }
        }
        if (download != null) {
            DownloadProgress(state = download, modifier = Modifier.padding(top = 8.dp, end = 12.dp))
        }
    }
}

@Composable
private fun AvailableCountryCard(
    item: AvailableCountry,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val download = item.download
    val apiName = item.filter.apiName.orEmpty()
    CountryCard(tag = TestTags.offlineCountry(apiName), modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlagBadge(flag = flagEmoji(item.filter.code.orEmpty()))
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(item.filter.labelRes()),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (download?.isActive == true) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_cancel_download),
                        tint = colors.onSurfaceVariant,
                    )
                }
            } else {
                FilledTonalButton(
                    onClick = onDownload,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .testTag(TestTags.downloadButton(item.filter)),
                ) {
                    Icon(
                        imageVector = if (download == DownloadState.Failed) Icons.Filled.Refresh else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(
                        text = stringResource(
                            if (download == DownloadState.Failed) R.string.action_retry else R.string.action_download,
                        ),
                    )
                }
            }
        }
        if (download != null) {
            DownloadProgress(state = download, modifier = Modifier.padding(top = 8.dp, end = 12.dp))
        }
    }
}

@Composable
private fun CountryCard(
    tag: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            content = content,
        )
    }
}

@Composable
private fun DownloadProgress(
    state: DownloadState,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        when (state) {
            DownloadState.Waiting -> {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.onSurfaceVariant,
                    trackColor = colors.surfaceContainerHigh,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.offline_waiting),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            is DownloadState.Running -> {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.primary,
                    trackColor = colors.surfaceContainerHigh,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.offline_running, state.downloaded),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.primary,
                )
            }
            DownloadState.Failed -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = colors.error,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.offline_failed),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.error,
                )
            }
            is DownloadState.Succeeded -> Unit
        }
    }
}

@Composable
private fun FlagBadge(flag: String) {
    val fontSize = with(LocalDensity.current) { FLAG_SIZE.toSp() }
    Box(
        modifier = Modifier
            .size(FLAG_BADGE_SIZE)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)

            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(text = flag, fontSize = fontSize, maxLines = 1)
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .padding(start = 4.dp, top = 8.dp)
            .semantics { heading() },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun MutedText(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 4.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RemoveCountryDialog(
    countryName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Filled.Delete, contentDescription = null) },
        title = { Text(text = stringResource(R.string.offline_remove_title)) },
        text = { Text(text = stringResource(R.string.offline_remove_message, countryName)) },
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

private const val LOADING_SPINNER_DELAY_MS = 300L
private const val INTRO_TINT_ALPHA = 0.5f
private const val ICON_CIRCLE_ALPHA = 0.16f
private const val DISABLED_ALPHA = 0.38f
private val FLAG_BADGE_SIZE = 40.dp
private val FLAG_SIZE = 20.dp

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14, heightDp = 1200)
@Composable
private fun OfflineCatalogScreenPreview() {
    val downloaded = PreviewData.offlineCountries.mapIndexed { index, country ->
        DownloadedCountry(
            country = country,
            filter = CountryFilter.entries.firstOrNull { it.apiName == country.countryName },
            download = if (index == 0) DownloadState.Running(downloaded = 400) else null,
        )
    }
    val downloadedNames = PreviewData.offlineCountries.map { it.countryName }.toSet()
    UniPortalTheme {
        OfflineCatalogScreen(
            state = OfflineCatalogUiState(
                isLoading = false,
                downloaded = downloaded,
                available = CountryFilter.entries
                    .filter { filter ->
                        val name = filter.apiName
                        name != null && name !in downloadedNames
                    }
                    .mapIndexed { index, filter ->
                        AvailableCountry(filter = filter, download = if (index == 1) DownloadState.Waiting else null)
                    },
                cachedCount = 1171,
                isOnline = false,
            ),
            onEvent = {},
            onBack = {},
        )
    }
}
