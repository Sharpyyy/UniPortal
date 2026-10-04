package com.example.uniportal.ui.applications

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniportal.data.repository.ApplicationRepository
import com.example.uniportal.data.repository.SettingsRepository
import com.example.uniportal.domain.model.AppSettings
import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.ErrorType
import com.example.uniportal.domain.model.UniversityApplication
import com.example.uniportal.domain.model.toErrorType
import com.example.uniportal.domain.rules.ApplicationRules
import com.example.uniportal.util.TimeProvider
import com.example.uniportal.util.WhileUiSubscribed
import com.example.uniportal.util.runCatchingCancellable
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ApplicationsViewModel @Inject constructor(
    private val repository: ApplicationRepository,
    settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {
    private val statusFilter = MutableStateFlow<ApplicationStatus?>(null)

    private val retryRequests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val writes = Mutex()

    private val message = MutableStateFlow<ApplicationsMessage?>(null)

    private val applications: Flow<ApplicationsLoad> = retryRequests
        .onStart { emit(Unit) }
        .flatMapLatest {
            repository.observeApplications()
                .map<List<UniversityApplication>, ApplicationsLoad> { list -> ApplicationsLoad.Loaded(list) }
                .catch { error ->
                    Log.w(TAG, "Failed to observe applications", error)
                    emit(ApplicationsLoad.Failed(error.toErrorType()))
                }
        }

    private val reminderDaysBefore: Flow<Int> = settingsRepository.settings
        .map { settings -> settings.reminderDaysBefore }
        .catch { error ->
            Log.w(TAG, "Failed to read settings", error)
            message.value = ApplicationsMessage.SETTINGS_UNAVAILABLE
            emit(AppSettings.DEFAULT_REMINDER_DAYS)
        }
        .distinctUntilChanged()

    val uiState: StateFlow<ApplicationsUiState> = combine(
        applications,
        statusFilter,
        reminderDaysBefore,
        message,
    ) { load, filter, daysBefore, pending ->
        when (load) {
            is ApplicationsLoad.Failed -> ApplicationsUiState(
                content = ApplicationsContent.Error(load.type),
                statusFilter = filter,
                today = timeProvider.today(),
                message = pending,
            )
            is ApplicationsLoad.Loaded ->
                dashboard(load.applications, filter, daysBefore, timeProvider.today()).copy(message = pending)
        }
    }.stateIn(viewModelScope, WhileUiSubscribed, ApplicationsUiState())

    fun onEvent(event: ApplicationsEvent) {
        when (event) {
            is ApplicationsEvent.FilterSelected -> statusFilter.value = event.status
            is ApplicationsEvent.ChangeStatus -> changeStatus(event.universityId, event.status)
            ApplicationsEvent.Retry -> retryRequests.tryEmit(Unit)
            ApplicationsEvent.MessageShown -> message.value = null
        }
    }

    private fun changeStatus(universityId: String, status: ApplicationStatus) {
        viewModelScope.launch {
            writes.withLock {
                runCatchingCancellable { repository.updateStatus(universityId, status) }
                    .onFailure { error ->
                        Log.w(TAG, "Failed to change the status of $universityId", error)

                        message.value = ApplicationsMessage.STATUS_CHANGE_FAILED
                    }
            }
        }
    }

    private companion object {
        const val TAG = "ApplicationsVM"
    }
}

private sealed interface ApplicationsLoad {
    data class Loaded(val applications: List<UniversityApplication>) : ApplicationsLoad
    data class Failed(val type: ErrorType) : ApplicationsLoad
}

private fun dashboard(
    applications: List<UniversityApplication>,
    filter: ApplicationStatus?,
    daysBefore: Int,
    today: LocalDate,
): ApplicationsUiState {
    fun UniversityApplication.toItem() = ApplicationItem(
        application = this,
        daysLeft = ApplicationRules.daysLeft(this, today),
        isOverdue = ApplicationRules.isOverdue(this, today),
        isDueSoon = ApplicationRules.needsReminder(this, today, daysBefore),
    )

    val sections = ApplicationRules.groupByStatus(applications, today)
        .filterKeys { status -> filter == null || status == filter }
        .map { (status, group) -> ApplicationSection(status = status, items = group.map { it.toItem() }) }
    return ApplicationsUiState(
        content = when {
            applications.isEmpty() -> ApplicationsContent.Empty
            sections.isEmpty() -> ApplicationsContent.NoMatches
            else -> ApplicationsContent.Success(sections)
        },
        statusFilter = filter,
        summary = ApplicationsSummary(
            total = applications.size,
            countByStatus = ApplicationRules.countByStatus(applications),
            overdueCount = applications.count { ApplicationRules.isOverdue(it, today) },
            dueSoonCount = applications.count { ApplicationRules.needsReminder(it, today, daysBefore) },
            reminderDaysBefore = daysBefore,
            nearest = ApplicationRules.nearestDeadline(applications, today)?.toItem(),
        ),
        today = today,
    )
}
