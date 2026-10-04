package com.example.uniportal.ui.settings

import com.example.uniportal.domain.model.AppSettings
import com.example.uniportal.domain.model.CacheTtl
import com.example.uniportal.domain.model.CountryFilter
import com.example.uniportal.domain.model.ThemeMode
import com.example.uniportal.work.SyncState

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),

    val isLoaded: Boolean = false,

    val syncState: SyncState = SyncState.Idle,
    val isClearingCache: Boolean = false,

    val message: SettingsMessage? = null,
) {
    val isSyncInProgress: Boolean
        get() = syncState == SyncState.Waiting || syncState == SyncState.Running
}

sealed interface SettingsMessage {
    data class CacheCleared(val removed: Int) : SettingsMessage

    data object SyncFailedToStart : SettingsMessage

    data object SaveFailed : SettingsMessage
}

sealed interface SettingsEvent {
    data class ThemeSelected(val theme: ThemeMode) : SettingsEvent
    data class HomeCountrySelected(val country: CountryFilter) : SettingsEvent
    data class CacheTtlSelected(val ttl: CacheTtl) : SettingsEvent
    data class BackgroundSyncChanged(val enabled: Boolean) : SettingsEvent
    data class SyncWifiOnlyChanged(val enabled: Boolean) : SettingsEvent
    data class DeadlineRemindersChanged(val enabled: Boolean) : SettingsEvent
    data class ReminderDaysSelected(val days: Int) : SettingsEvent

    data class HistoryLimitSelected(val limit: Int) : SettingsEvent
    data object SyncNow : SettingsEvent

    data object ClearCache : SettingsEvent
    data object MessageShown : SettingsEvent
}
