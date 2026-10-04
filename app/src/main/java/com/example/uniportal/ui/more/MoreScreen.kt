package com.example.uniportal.ui.more

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniportal.BuildConfig
import com.example.uniportal.R
import com.example.uniportal.ui.components.PortalLogo
import com.example.uniportal.ui.components.TestTags
import com.example.uniportal.ui.components.rememberCurrentTimeMillis
import com.example.uniportal.ui.components.relativeTimeText
import com.example.uniportal.ui.theme.PortalTheme
import com.example.uniportal.ui.theme.UniPortalTheme

@Composable
fun MoreRoute(
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenOffline: () -> Unit,
    onOpenFavourites: () -> Unit,
    onOpenShortlists: () -> Unit,
    onOpenApplications: () -> Unit,
    viewModel: MoreViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MoreScreen(
        state = state,
        onOpenHistory = onOpenHistory,
        onOpenSettings = onOpenSettings,
        onOpenOffline = onOpenOffline,
        onOpenFavourites = onOpenFavourites,
        onOpenShortlists = onOpenShortlists,
        onOpenApplications = onOpenApplications,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    state: MoreUiState,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenOffline: () -> Unit,
    onOpenFavourites: () -> Unit,
    onOpenShortlists: () -> Unit,
    onOpenApplications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAbout by rememberSaveable { mutableStateOf(false) }
    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.more_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },

                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DashboardCard(
                state = state,
                onOpenFavourites = onOpenFavourites,
                onOpenShortlists = onOpenShortlists,
                onOpenApplications = onOpenApplications,
                onOpenOffline = onOpenOffline,
            )
            MenuCard(
                state = state,
                onOpenHistory = onOpenHistory,
                onOpenOffline = onOpenOffline,
                onOpenSettings = onOpenSettings,
                onOpenAbout = { showAbout = true },
            )
        }
    }
}

@Composable
private fun DashboardCard(
    state: MoreUiState,
    onOpenFavourites: () -> Unit,
    onOpenShortlists: () -> Unit,
    onOpenApplications: () -> Unit,
    onOpenOffline: () -> Unit,
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
                    Brush.verticalGradient(listOf(colors.primaryContainer.copy(alpha = DASHBOARD_TINT_ALPHA), Color.Transparent)),
                )
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PortalLogo(size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.more_dashboard_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.more_dashboard_subtitle),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatTile(
                    icon = Icons.Filled.Favorite,
                    accent = PortalTheme.colors.favourite,
                    value = state.favouritesCount,
                    label = stringResource(R.string.more_stat_favourites),
                    onClick = onOpenFavourites,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                StatTile(
                    icon = Icons.AutoMirrored.Filled.List,
                    accent = colors.tertiary,
                    value = state.shortlistsCount,
                    label = stringResource(R.string.more_stat_shortlists),
                    onClick = onOpenShortlists,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatTile(
                    icon = Icons.Filled.DateRange,
                    accent = colors.secondary,
                    value = state.applicationsCount,
                    label = stringResource(R.string.more_stat_applications),
                    supporting = if (state.activeApplicationsCount > 0) {
                        stringResource(R.string.more_stat_active, state.activeApplicationsCount)
                    } else {
                        null
                    },
                    onClick = onOpenApplications,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                StatTile(
                    icon = Icons.Filled.Place,
                    accent = colors.primary,
                    value = state.cachedUniversitiesCount,
                    label = stringResource(R.string.more_stat_offline),
                    onClick = onOpenOffline,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    accent: Color,
    value: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(accent.copy(alpha = ICON_CIRCLE_ALPHA), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = accent,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onSurface,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MenuCard(
    state: MoreUiState,
    onOpenHistory: () -> Unit,
    onOpenOffline: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val now = rememberCurrentTimeMillis()
    val lastSyncAt = state.lastSyncAt
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
        border = BorderStroke(1.dp, colors.outline),
    ) {
        MenuRow(
            icon = Icons.Filled.DateRange,
            accent = colors.secondary,
            title = stringResource(R.string.more_history),
            subtitle = if (state.historyCount > 0) {
                pluralStringResource(R.plurals.more_history_count, state.historyCount, state.historyCount)
            } else {
                stringResource(R.string.more_history_empty)
            },
            onClick = onOpenHistory,
            modifier = Modifier.testTag(TestTags.MORE_HISTORY_ROW),
        )
        MenuDivider()
        MenuRow(
            icon = Icons.Filled.Place,
            accent = colors.primary,
            title = stringResource(R.string.more_offline),
            subtitle = if (state.offlineCountriesCount > 0) {
                pluralStringResource(
                    R.plurals.more_offline_countries,
                    state.offlineCountriesCount,
                    state.offlineCountriesCount,
                )
            } else {
                stringResource(R.string.more_offline_empty)
            },
            onClick = onOpenOffline,
            modifier = Modifier.testTag(TestTags.MORE_OFFLINE_ROW),
        )
        MenuDivider()
        MenuRow(
            icon = Icons.Filled.Settings,
            accent = colors.tertiary,
            title = stringResource(R.string.more_settings),
            subtitle = if (lastSyncAt != null) {
                stringResource(R.string.settings_last_sync, relativeTimeText(lastSyncAt, now))
            } else {
                stringResource(R.string.more_settings_subtitle)
            },
            onClick = onOpenSettings,
            modifier = Modifier.testTag(TestTags.MORE_SETTINGS_ROW),
        )
        MenuDivider()
        MenuRow(
            icon = Icons.Filled.Info,
            accent = colors.onSurfaceVariant,
            title = stringResource(R.string.more_about),
            subtitle = stringResource(R.string.more_version, BuildConfig.VERSION_NAME),
            onClick = onOpenAbout,
            modifier = Modifier.testTag(TestTags.MORE_ABOUT_ROW),
        )
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accent.copy(alpha = ICON_CIRCLE_ALPHA), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = accent,
                )
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = MENU_DIVIDER_INSET),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { PortalLogo(size = 40.dp) },
        title = { Text(text = stringResource(R.string.app_name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.about_message))
                Text(
                    text = stringResource(R.string.about_data_source),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_close))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}

private const val DASHBOARD_TINT_ALPHA = 0.5f
private const val ICON_CIRCLE_ALPHA = 0.16f

private val MENU_DIVIDER_INSET = 72.dp

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14, heightDp = 900)
@Composable
private fun MoreScreenPreview() {
    UniPortalTheme {
        MoreScreen(
            state = MoreUiState(
                favouritesCount = 12,
                shortlistsCount = 3,
                applicationsCount = 5,
                activeApplicationsCount = 2,
                cachedUniversitiesCount = 1171,
                offlineCountriesCount = 2,
                historyCount = 37,
                lastSyncAt = System.currentTimeMillis() - PREVIEW_SYNC_AGE_MS,
            ),
            onOpenHistory = {},
            onOpenSettings = {},
            onOpenOffline = {},
            onOpenFavourites = {},
            onOpenShortlists = {},
            onOpenApplications = {},
        )
    }
}

private const val PREVIEW_SYNC_AGE_MS = 2 * 60 * 60 * 1000L
