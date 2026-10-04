package com.example.uniportal.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniportal.data.repository.HistoryRepository
import com.example.uniportal.data.repository.SettingsRepository
import com.example.uniportal.data.repository.UniversityRepository
import com.example.uniportal.domain.model.AppSettings
import com.example.uniportal.util.WhileUiSubscribed
import com.example.uniportal.util.runCatchingCancellable
import com.example.uniportal.work.SyncState
import com.example.uniportal.work.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    private val universityRepository: UniversityRepository,
    private val workScheduler: WorkScheduler,
) : ViewModel() {
    private val isClearingCache = MutableStateFlow(false)
    private val message = MutableStateFlow<SettingsMessage?>(null)

    private val writes = Mutex()

    private val settings: Flow<AppSettings> = settingsRepository.settings
        .catch { error ->
            Log.w(TAG, "Failed to read settings", error)
            emit(AppSettings())
        }

    private val syncState: Flow<SyncState> = workScheduler.observeSyncState()
        .catch { error ->
            Log.w(TAG, "Failed to observe the sync", error)
            emit(SyncState.Idle)
        }

        .onStart { emit(SyncState.Idle) }
        .distinctUntilChanged()

    val uiState: StateFlow<SettingsUiState> = combine(
        settings,
        syncState,
        isClearingCache,
        message,
    ) { current, sync, clearing, currentMessage ->
        SettingsUiState(
            settings = current,
            isLoaded = true,
            syncState = sync,
            isClearingCache = clearing,
            message = currentMessage,
        )
    }.stateIn(viewModelScope, WhileUiSubscribed, SettingsUiState())

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ThemeSelected -> write { settingsRepository.setTheme(event.theme) }
            is SettingsEvent.HomeCountrySelected -> write { settingsRepository.setHomeCountry(event.country) }
            is SettingsEvent.CacheTtlSelected -> write { settingsRepository.setCacheTtl(event.ttl) }
            is SettingsEvent.BackgroundSyncChanged -> write { settingsRepository.setBackgroundSync(event.enabled) }
            is SettingsEvent.SyncWifiOnlyChanged -> write { settingsRepository.setSyncWifiOnly(event.enabled) }
            is SettingsEvent.DeadlineRemindersChanged -> write {
                settingsRepository.setDeadlineReminders(event.enabled)
            }
            is SettingsEvent.ReminderDaysSelected -> write { settingsRepository.setReminderDaysBefore(event.days) }
            is SettingsEvent.HistoryLimitSelected -> write {
                settingsRepository.setHistoryLimit(event.limit)

                historyRepository.trimTo(event.limit)
            }
            SettingsEvent.SyncNow -> syncNow()
            SettingsEvent.ClearCache -> clearCache()
            SettingsEvent.MessageShown -> message.value = null
        }
    }

    private fun syncNow() {
        try {
            workScheduler.syncNow()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Failed to request a sync", e)
            message.value = SettingsMessage.SyncFailedToStart
        }
    }

    private fun clearCache() {
        if (isClearingCache.value) return
        isClearingCache.value = true
        viewModelScope.launch {
            runCatchingCancellable { universityRepository.clearUnpinnedCache() }.fold(
                onSuccess = { removed -> message.value = SettingsMessage.CacheCleared(removed) },
                onFailure = { error ->
                    Log.w(TAG, "Failed to clear the cache", error)
                    message.value = SettingsMessage.SaveFailed
                },
            )
            isClearingCache.value = false
        }
    }

    private fun write(action: suspend () -> Unit) {
        viewModelScope.launch {
            writes.withLock {
                runCatchingCancellable { action() }
                    .onFailure { error ->
                        Log.w(TAG, "Failed to save a setting", error)
                        message.value = SettingsMessage.SaveFailed
                    }
            }
        }
    }

    private companion object {
        const val TAG = "SettingsVM"
    }
}
