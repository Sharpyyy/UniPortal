package com.example.uniportal.ui.settings

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as SystemSettings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniportal.R
import com.example.uniportal.data.sync.SyncReport
import com.example.uniportal.domain.model.AppSettings
import com.example.uniportal.domain.model.CacheTtl
import com.example.uniportal.domain.model.CountryFilter
import com.example.uniportal.domain.model.ThemeMode
import com.example.uniportal.ui.components.BannerTone
import com.example.uniportal.ui.components.LoadingState
import com.example.uniportal.ui.components.OfflineBanner
import com.example.uniportal.ui.components.PortalSnackbarHost
import com.example.uniportal.ui.components.SectionCard
import com.example.uniportal.ui.components.TestTags
import com.example.uniportal.ui.components.flag
import com.example.uniportal.ui.components.labelRes
import com.example.uniportal.ui.components.rememberCurrentTimeMillis
import com.example.uniportal.ui.components.relativeTimeText
import com.example.uniportal.ui.theme.LightBackground
import com.example.uniportal.ui.theme.LightGreen
import com.example.uniportal.ui.theme.PortalBackground
import com.example.uniportal.ui.theme.PortalGreen
import com.example.uniportal.ui.theme.PortalTheme
import com.example.uniportal.ui.theme.UniPortalTheme
import com.example.uniportal.work.SyncState

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenOffline: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var notificationsAllowed by remember { mutableStateOf(context.areNotificationsAllowed()) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsAllowed = context.areNotificationsAllowed()
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsAllowed = context.areNotificationsAllowed()
    }

    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    val requestNotificationPermission = {
        permissionRequested = true
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    SettingsScreen(
        state = state,
        notificationsAllowed = notificationsAllowed,
        onEvent = { event ->
            if (event is SettingsEvent.DeadlineRemindersChanged && event.enabled && context.needsNotificationPermission()) {
                requestNotificationPermission()
            }
            viewModel.onEvent(event)
        },
        onAllowNotifications = {
            val canShowDialog = !permissionRequested || context.shouldShowNotificationRationale()
            if (context.needsNotificationPermission() && canShowDialog) {
                requestNotificationPermission()
            } else {
                context.openNotificationSettings()
            }
        },
        onBack = onBack,
        onOpenOffline = onOpenOffline,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    notificationsAllowed: Boolean,
    onEvent: (SettingsEvent) -> Unit,
    onAllowNotifications: () -> Unit,
    onBack: () -> Unit,
    onOpenOffline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = when (val current = state.message) {
        is SettingsMessage.CacheCleared -> pluralStringResource(
            R.plurals.settings_cache_cleared,
            current.removed,
            current.removed,
        )
        SettingsMessage.SyncFailedToStart -> stringResource(R.string.settings_sync_failed_to_start)
        SettingsMessage.SaveFailed -> stringResource(R.string.error_save_failed)
        null -> null
    }
    val currentOnEvent by rememberUpdatedState(onEvent)
    LaunchedEffect(state.message) {
        if (messageText == null) return@LaunchedEffect
        snackbarHostState.showSnackbar(message = messageText, duration = SnackbarDuration.Short)
        currentOnEvent(SettingsEvent.MessageShown)
    }

    var showCountryDialog by rememberSaveable { mutableStateOf(false) }
    var showClearCacheDialog by rememberSaveable { mutableStateOf(false) }
    if (showCountryDialog) {
        HomeCountryDialog(
            selected = state.settings.homeCountry,
            onSelect = { country ->
                showCountryDialog = false
                onEvent(SettingsEvent.HomeCountrySelected(country))
            },
            onDismiss = { showCountryDialog = false },
        )
    }
    if (showClearCacheDialog) {
        ClearCacheDialog(
            onConfirm = {
                showClearCacheDialog = false
                onEvent(SettingsEvent.ClearCache)
            },
            onDismiss = { showClearCacheDialog = false },
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { PortalSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
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
        if (!state.isLoaded) {
            LoadingState(modifier = Modifier.padding(innerPadding), showAfterMillis = LOADING_SPINNER_DELAY_MS)
        } else {
            val layoutDirection = LocalLayoutDirection.current
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = innerPadding.calculateStartPadding(layoutDirection),
                        top = innerPadding.calculateTopPadding(),
                        end = innerPadding.calculateEndPadding(layoutDirection),
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 16.dp,
                        top = 8.dp,
                        end = 16.dp,
                        bottom = 8.dp + innerPadding.calculateBottomPadding(),
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppearanceSection(
                    selected = state.settings.theme,
                    onSelect = { onEvent(SettingsEvent.ThemeSelected(it)) },
                )
                SearchSection(
                    homeCountry = state.settings.homeCountry,
                    onChangeHomeCountry = { showCountryDialog = true },
                )
                DataSection(
                    state = state,
                    onEvent = onEvent,
                    onOpenOffline = onOpenOffline,
                    onClearCache = { showClearCacheDialog = true },
                )
                RemindersSection(
                    settings = state.settings,
                    notificationsAllowed = notificationsAllowed,
                    onEvent = onEvent,
                    onAllowNotifications = onAllowNotifications,
                )
                HistorySection(
                    limit = state.settings.historyLimit,
                    onSelect = { onEvent(SettingsEvent.HistoryLimitSelected(it)) },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AppearanceSection(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    SectionCard(title = stringResource(R.string.settings_section_appearance), icon = Icons.Filled.Star) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeMode.entries.forEach { mode ->
                ThemeOption(
                    mode = mode,
                    selected = mode == selected,
                    onClick = { if (mode != selected) onSelect(mode) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun ThemeOption(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    Surface(
        modifier = modifier
            .clip(shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .testTag(TestTags.settingsOption(OPTION_GROUP_THEME, mode.name)),
        shape = shape,
        color = if (selected) colors.primaryContainer.copy(alpha = SELECTED_TILE_ALPHA) else colors.surfaceContainerHigh,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) colors.primary else colors.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(SWATCH_SIZE)
                    .clip(CircleShape)
                    .background(mode.swatchBackground())
                    .border(1.dp, colors.outline, CircleShape)
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(SWATCH_DOT_SIZE)
                        .background(mode.swatchAccent(), CircleShape),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(mode.themeLabelRes()),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) colors.onSurface else colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (mode == ThemeMode.PORTAL) {
                Text(
                    text = stringResource(R.string.theme_portal_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SearchSection(
    homeCountry: CountryFilter,
    onChangeHomeCountry: () -> Unit,
) {
    SectionCard(title = stringResource(R.string.settings_section_search), icon = Icons.Filled.Search) {
        NavigationRow(
            title = stringResource(R.string.settings_home_country),
            subtitle = stringResource(R.string.settings_home_country_hint),
            value = homeCountry.flag() + " " + stringResource(homeCountry.labelRes()),
            onClick = onChangeHomeCountry,
            modifier = Modifier.testTag(TestTags.SETTINGS_HOME_COUNTRY_ROW),
        )
    }
}

@Composable
private fun DataSection(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    onOpenOffline: () -> Unit,
    onClearCache: () -> Unit,
) {
    val settings = state.settings
    SectionCard(title = stringResource(R.string.settings_section_data), icon = Icons.Filled.Refresh) {
        SettingLabel(
            title = stringResource(R.string.settings_cache_ttl),
            hint = stringResource(R.string.settings_cache_ttl_hint),
        )
        OptionChips(
            options = CacheTtl.entries,
            selected = settings.cacheTtl,
            optionLabel = { stringResource(it.ttlLabelRes()) },
            onSelect = { onEvent(SettingsEvent.CacheTtlSelected(it)) },
            tag = { TestTags.settingsOption(OPTION_GROUP_TTL, it.name) },
        )
        SectionDivider()
        SwitchRow(
            title = stringResource(R.string.settings_background_sync),
            subtitle = stringResource(R.string.settings_background_sync_hint),
            checked = settings.backgroundSync,
            onCheckedChange = { onEvent(SettingsEvent.BackgroundSyncChanged(it)) },
            modifier = Modifier.testTag(TestTags.SETTINGS_BACKGROUND_SYNC_SWITCH),
        )
        SwitchRow(
            title = stringResource(R.string.settings_wifi_only),
            subtitle = stringResource(R.string.settings_wifi_only_hint),
            checked = settings.syncWifiOnly,
            onCheckedChange = { onEvent(SettingsEvent.SyncWifiOnlyChanged(it)) },
            enabled = settings.backgroundSync,
            modifier = Modifier.testTag(TestTags.SETTINGS_WIFI_ONLY_SWITCH),
        )
        SectionDivider()
        SyncNowBlock(
            syncState = state.syncState,
            lastSyncAt = settings.lastSyncAt,
            inProgress = state.isSyncInProgress,
            onSyncNow = { onEvent(SettingsEvent.SyncNow) },
        )
        SectionDivider()
        NavigationRow(
            title = stringResource(R.string.settings_offline_catalog),
            subtitle = stringResource(R.string.settings_offline_catalog_hint),
            value = null,
            onClick = onOpenOffline,
            modifier = Modifier.testTag(TestTags.SETTINGS_OFFLINE_ROW),
        )
        SectionDivider()
        ClearCacheRow(isClearing = state.isClearingCache, onClick = onClearCache)
    }
}

@Composable
private fun SyncNowBlock(
    syncState: SyncState,
    lastSyncAt: Long?,
    inProgress: Boolean,
    onSyncNow: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val now = rememberCurrentTimeMillis()
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (lastSyncAt != null) {
                stringResource(R.string.settings_last_sync, relativeTimeText(lastSyncAt, now))
            } else {
                stringResource(R.string.settings_never_synced)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
        )
        AnimatedVisibility(visible = syncState != SyncState.Idle) {
            SyncStatusLine(syncState = syncState, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(
            onClick = onSyncNow,
            enabled = !inProgress,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.SETTINGS_SYNC_NOW_BUTTON),
        ) {
            if (inProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    color = LocalContentColor.current,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
            }
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(text = stringResource(R.string.settings_sync_now))
        }
    }
}

@Composable
private fun SyncStatusLine(
    syncState: SyncState,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val warning = PortalTheme.colors.warning
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        when (syncState) {
            SyncState.Idle -> Unit
            SyncState.Waiting -> {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(STATUS_ICON_SIZE),
                    tint = colors.onSurfaceVariant,
                )
                StatusText(text = stringResource(R.string.sync_state_waiting), color = colors.onSurfaceVariant)
            }
            SyncState.Running -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(STATUS_ICON_SIZE),
                    color = colors.primary,
                    strokeWidth = 2.dp,
                )
                StatusText(text = stringResource(R.string.sync_state_running), color = colors.primary)
            }
            is SyncState.Succeeded -> {
                val report = syncState.report
                val partial = report.failed > 0
                Icon(
                    imageVector = if (partial) Icons.Filled.Warning else Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(STATUS_ICON_SIZE),
                    tint = if (partial) warning else colors.primary,
                )
                StatusText(
                    text = if (partial) {
                        stringResource(R.string.sync_state_partial, report.refreshed, report.failed)
                    } else {
                        stringResource(R.string.sync_state_succeeded, report.refreshed, report.pruned)
                    },
                    color = if (partial) warning else colors.primary,
                )
            }
            SyncState.Failed -> {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(STATUS_ICON_SIZE),
                    tint = colors.error,
                )
                StatusText(text = stringResource(R.string.sync_state_failed), color = colors.error)
            }
        }
    }
}

@Composable
private fun StatusText(text: String, color: Color) {
    Spacer(Modifier.width(8.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
    )
}

@Composable
private fun ClearCacheRow(
    isClearing: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_clear_cache),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_clear_cache_hint),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        OutlinedButton(
            onClick = onClick,
            enabled = !isClearing,
            modifier = Modifier.testTag(TestTags.SETTINGS_CLEAR_CACHE_BUTTON),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
            border = BorderStroke(1.dp, colors.error.copy(alpha = DESTRUCTIVE_BORDER_ALPHA)),
        ) {
            if (isClearing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    color = colors.error,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
            }
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(text = stringResource(R.string.action_clear))
        }
    }
}

@Composable
private fun RemindersSection(
    settings: AppSettings,
    notificationsAllowed: Boolean,
    onEvent: (SettingsEvent) -> Unit,
    onAllowNotifications: () -> Unit,
) {
    SectionCard(title = stringResource(R.string.settings_section_reminders), icon = Icons.Filled.Notifications) {
        SwitchRow(
            title = stringResource(R.string.settings_deadline_reminders),
            subtitle = stringResource(R.string.settings_deadline_reminders_hint),
            checked = settings.deadlineReminders,
            onCheckedChange = { onEvent(SettingsEvent.DeadlineRemindersChanged(it)) },
            modifier = Modifier.testTag(TestTags.SETTINGS_REMINDERS_SWITCH),
        )
        AnimatedVisibility(visible = settings.deadlineReminders && !notificationsAllowed) {
            OfflineBanner(
                text = stringResource(R.string.settings_notifications_denied),
                tone = BannerTone.WARNING,
                modifier = Modifier.padding(vertical = 8.dp),
                icon = Icons.Filled.Notifications,
                actionLabel = stringResource(R.string.action_allow),
                onAction = onAllowNotifications,
            )
        }
        Spacer(Modifier.height(8.dp))
        SettingLabel(
            title = stringResource(R.string.settings_reminder_days),
            hint = null,
            enabled = settings.deadlineReminders,
        )
        OptionChips(
            options = AppSettings.REMINDER_DAYS_OPTIONS,
            selected = settings.reminderDaysBefore,
            optionLabel = { days -> pluralStringResource(R.plurals.days_count, days, days) },
            onSelect = { onEvent(SettingsEvent.ReminderDaysSelected(it)) },
            tag = { TestTags.settingsOption(OPTION_GROUP_REMINDER_DAYS, it.toString()) },
            enabled = settings.deadlineReminders,
        )
    }
}

@Composable
private fun HistorySection(
    limit: Int,
    onSelect: (Int) -> Unit,
) {
    SectionCard(title = stringResource(R.string.settings_section_history), icon = Icons.Filled.DateRange) {
        SettingLabel(
            title = stringResource(R.string.settings_history_limit),
            hint = stringResource(R.string.settings_history_limit_hint),
        )
        OptionChips(
            options = AppSettings.HISTORY_LIMIT_OPTIONS,
            selected = limit,
            optionLabel = { it.toString() },
            onSelect = onSelect,
            tag = { TestTags.settingsOption(OPTION_GROUP_HISTORY_LIMIT, it.toString()) },
        )
    }
}

@Composable
private fun SettingLabel(
    title: String,
    hint: String?,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) colors.onSurface else colors.onSurface.copy(alpha = DISABLED_ALPHA),
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> OptionChips(
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    tag: (T) -> String,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            FilterChip(
                selected = isSelected,
                onClick = { if (!isSelected) onSelect(option) },
                label = { Text(text = optionLabel(option)) },
                modifier = Modifier.testTag(tag(option)),
                enabled = enabled,
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = colors.surfaceContainerHigh,
                    labelColor = colors.onSurfaceVariant,
                    selectedContainerColor = colors.primaryContainer,
                    selectedLabelColor = colors.onPrimaryContainer,
                    selectedLeadingIconColor = colors.onPrimaryContainer,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = enabled,
                    selected = isSelected,
                    borderColor = colors.outline,
                    selectedBorderColor = colors.primary,
                    selectedBorderWidth = 1.dp,
                ),
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) colors.onSurface else colors.onSurface.copy(alpha = DISABLED_ALPHA),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) colors.onSurfaceVariant else colors.onSurfaceVariant.copy(alpha = DISABLED_ALPHA),
            )
        }
        Spacer(Modifier.width(12.dp))

        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun NavigationRow(
    title: String,
    subtitle: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        if (value != null) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = value,
                modifier = Modifier.widthIn(max = VALUE_MAX_WIDTH),
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun HomeCountryDialog(
    selected: CountryFilter,
    onSelect: (CountryFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
        title = { Text(text = stringResource(R.string.settings_home_country)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = COUNTRY_LIST_MAX_HEIGHT)
                    .verticalScroll(rememberScrollState()),
            ) {
                CountryFilter.entries.forEach { country ->
                    val isSelected = country == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(country) })
                            .testTag(TestTags.settingsOption(OPTION_GROUP_HOME_COUNTRY, country.name))
                            .heightIn(min = MIN_TOUCH_TARGET)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(text = country.flag(), modifier = Modifier.clearAndSetSemantics {})
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(country.labelRes()),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        iconContentColor = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ClearCacheDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Filled.Delete, contentDescription = null) },
        title = { Text(text = stringResource(R.string.settings_clear_cache_title)) },
        text = { Text(text = stringResource(R.string.settings_clear_cache_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(TestTags.CONFIRM_DELETE_BUTTON),
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

@StringRes
private fun ThemeMode.themeLabelRes(): Int = when (this) {
    ThemeMode.PORTAL -> R.string.theme_portal
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.SYSTEM -> R.string.theme_system
}

@StringRes
private fun CacheTtl.ttlLabelRes(): Int = when (this) {
    CacheTtl.H6 -> R.string.ttl_6h
    CacheTtl.H24 -> R.string.ttl_24h
    CacheTtl.D3 -> R.string.ttl_3d
    CacheTtl.D7 -> R.string.ttl_7d
}

private fun ThemeMode.swatchBackground(): Brush = when (this) {
    ThemeMode.PORTAL -> SolidColor(PortalBackground)
    ThemeMode.LIGHT -> SolidColor(LightBackground)
    ThemeMode.SYSTEM -> Brush.horizontalGradient(
        0f to LightBackground,
        0.5f to LightBackground,
        0.5f to PortalBackground,
        1f to PortalBackground,
    )
}

private fun ThemeMode.swatchAccent(): Brush = when (this) {
    ThemeMode.PORTAL -> SolidColor(PortalGreen)
    ThemeMode.LIGHT -> SolidColor(LightGreen)
    ThemeMode.SYSTEM -> Brush.horizontalGradient(listOf(LightGreen, PortalGreen))
}

private fun Context.needsNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

private fun Context.shouldShowNotificationRationale(): Boolean {
    val activity = findActivity() ?: return false
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.areNotificationsAllowed(): Boolean =
    !needsNotificationPermission() && NotificationManagerCompat.from(this).areNotificationsEnabled()

private fun Context.openNotificationSettings() {
    val notificationSettings = Intent(SystemSettings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(SystemSettings.EXTRA_APP_PACKAGE, packageName)
    try {
        startActivity(notificationSettings)
    } catch (e: ActivityNotFoundException) {
        val appDetails = Intent(SystemSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        try {
            startActivity(appDetails)
        } catch (fallbackError: ActivityNotFoundException) {
            Log.w(TAG, "No settings screen for notifications", fallbackError)
        }
    }
}

private const val TAG = "SettingsScreen"
private const val OPTION_GROUP_THEME = "theme"
private const val OPTION_GROUP_TTL = "ttl"
private const val OPTION_GROUP_REMINDER_DAYS = "reminder_days"
private const val OPTION_GROUP_HISTORY_LIMIT = "history_limit"
private const val OPTION_GROUP_HOME_COUNTRY = "home_country"
private const val LOADING_SPINNER_DELAY_MS = 300L
private const val SELECTED_TILE_ALPHA = 0.45f
private const val DESTRUCTIVE_BORDER_ALPHA = 0.5f
private const val DISABLED_ALPHA = 0.38f
private val SWATCH_SIZE = 40.dp
private val SWATCH_DOT_SIZE = 14.dp
private val STATUS_ICON_SIZE = 14.dp
private val VALUE_MAX_WIDTH = 160.dp
private val COUNTRY_LIST_MAX_HEIGHT = 420.dp
private val MIN_TOUCH_TARGET = 48.dp

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14, heightDp = 1600)
@Composable
private fun SettingsScreenPreview() {
    UniPortalTheme {
        SettingsScreen(
            state = SettingsUiState(
                settings = AppSettings(
                    homeCountry = CountryFilter.RUSSIA,
                    lastSyncAt = System.currentTimeMillis() - PREVIEW_SYNC_AGE_MS,
                ),
                isLoaded = true,
                syncState = SyncState.Succeeded(SyncReport(refreshed = 12, failed = 0, pruned = 40)),
            ),
            notificationsAllowed = false,
            onEvent = {},
            onAllowNotifications = {},
            onBack = {},
            onOpenOffline = {},
        )
    }
}

private const val PREVIEW_SYNC_AGE_MS = 45 * 60 * 1000L
