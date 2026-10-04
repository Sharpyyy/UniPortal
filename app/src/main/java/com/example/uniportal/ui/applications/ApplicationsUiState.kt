package com.example.uniportal.ui.applications

import com.example.uniportal.domain.model.AppSettings
import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.ErrorType
import com.example.uniportal.domain.model.UniversityApplication
import java.time.LocalDate

data class ApplicationsUiState(
    val content: ApplicationsContent = ApplicationsContent.Loading,

    val statusFilter: ApplicationStatus? = null,
    val summary: ApplicationsSummary = ApplicationsSummary(),

    val today: LocalDate = LocalDate.now(),

    val message: ApplicationsMessage? = null,
)

enum class ApplicationsMessage {
    STATUS_CHANGE_FAILED,

    SETTINGS_UNAVAILABLE,
}

data class ApplicationsSummary(
    val total: Int = 0,

    val countByStatus: Map<ApplicationStatus, Int> = emptyMap(),

    val overdueCount: Int = 0,

    val dueSoonCount: Int = 0,
    val reminderDaysBefore: Int = AppSettings.DEFAULT_REMINDER_DAYS,

    val nearest: ApplicationItem? = null,
)

data class ApplicationItem(
    val application: UniversityApplication,

    val daysLeft: Int?,
    val isOverdue: Boolean,
    val isDueSoon: Boolean,
) {
    val universityId: String
        get() = application.university.id
}

data class ApplicationSection(
    val status: ApplicationStatus,
    val items: List<ApplicationItem>,
)

sealed interface ApplicationsContent {
    data object Loading : ApplicationsContent
    data class Error(val type: ErrorType) : ApplicationsContent

    data object Empty : ApplicationsContent

    data object NoMatches : ApplicationsContent

    data class Success(val sections: List<ApplicationSection>) : ApplicationsContent
}

sealed interface ApplicationsEvent {
    data class FilterSelected(val status: ApplicationStatus?) : ApplicationsEvent

    data class ChangeStatus(val universityId: String, val status: ApplicationStatus) : ApplicationsEvent

    data object Retry : ApplicationsEvent

    data object MessageShown : ApplicationsEvent
}
