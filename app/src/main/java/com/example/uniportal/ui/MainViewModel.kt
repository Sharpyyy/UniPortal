package com.example.uniportal.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniportal.data.repository.ApplicationRepository
import com.example.uniportal.data.repository.SettingsRepository
import com.example.uniportal.domain.model.AppSettings
import com.example.uniportal.domain.model.ThemeMode
import com.example.uniportal.domain.rules.ApplicationRules
import com.example.uniportal.ui.navigation.TopLevelDestination
import com.example.uniportal.util.TimeProvider
import com.example.uniportal.util.WhileUiSubscribed
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    applicationRepository: ApplicationRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {
    private val settings: Flow<AppSettings> = settingsRepository.settings
        .catch { error ->
            Log.w(TAG, "Failed to read settings", error)
            emit(AppSettings())
        }

    val themeMode: StateFlow<ThemeMode?> = settings
        .map { it.theme }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val applicationsBadgeCount: StateFlow<Int> = combine(
        applicationRepository.observeApplications().catch { error ->
            Log.w(TAG, "Failed to observe applications", error)
            emit(emptyList())
        },
        settings.map { it.reminderDaysBefore }.distinctUntilChanged(),
    ) { applications, daysBefore ->
        val today = timeProvider.today()
        applications.count { application ->
            ApplicationRules.isOverdue(application, today) ||
                ApplicationRules.needsReminder(application, today, daysBefore)
        }
    }.stateIn(viewModelScope, WhileUiSubscribed, 0)

    private val pendingTabRequest = MutableStateFlow<TopLevelDestination?>(null)

    val pendingTab: StateFlow<TopLevelDestination?> = pendingTabRequest.asStateFlow()

    fun openTab(tab: TopLevelDestination) {
        pendingTabRequest.value = tab
    }

    fun onPendingTabHandled() {
        pendingTabRequest.value = null
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}
