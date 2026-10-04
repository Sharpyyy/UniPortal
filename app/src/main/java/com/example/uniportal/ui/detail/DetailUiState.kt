package com.example.uniportal.ui.detail

import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.ErrorType
import com.example.uniportal.domain.model.Shortlist
import com.example.uniportal.domain.model.University
import com.example.uniportal.domain.model.UniversityApplication
import com.example.uniportal.domain.rules.ShortlistError

data class DetailUiState(
    val content: DetailContent = DetailContent.Loading,

    val fetchedAt: Long? = null,

    val isRefreshing: Boolean = false,

    val refreshFailed: Boolean = false,

    val isFavourite: Boolean = false,
    val application: ApplicationSummary? = null,

    val shortlists: List<Shortlist> = emptyList(),

    val memberShortlistIds: Set<Long> = emptySet(),

    val shortlistDialog: ShortlistDialogState? = null,

    val message: DetailMessage? = null,
) {
    val memberShortlists: List<Shortlist>
        get() = shortlists.filter { it.id in memberShortlistIds }
}

sealed interface DetailContent {
    data object Loading : DetailContent

    data class Error(val type: ErrorType) : DetailContent
    data class Success(val university: University) : DetailContent
}

data class ApplicationSummary(
    val application: UniversityApplication,
    val daysLeft: Int?,
    val isOverdue: Boolean,
    val nextStatus: ApplicationStatus?,
)

data class ShortlistDialogState(
    val selectedIds: Set<Long>,
    val newName: String = "",
    val nameError: ShortlistError? = null,
    val isCreating: Boolean = false,
)

sealed interface DetailMessage {
    data class ShortlistsSaved(val count: Int) : DetailMessage

    data class StatusChanged(val status: ApplicationStatus) : DetailMessage

    data object SaveFailed : DetailMessage

    data object SettingsUnavailable : DetailMessage
}

sealed interface DetailEvent {
    data object Retry : DetailEvent
    data object ToggleFavourite : DetailEvent

    data object AdvanceStatus : DetailEvent

    data object OpenShortlistDialog : DetailEvent
    data class ShortlistToggled(val shortlistId: Long) : DetailEvent
    data class NewShortlistNameChanged(val name: String) : DetailEvent

    data object CreateShortlist : DetailEvent

    data object ConfirmShortlists : DetailEvent
    data object DismissShortlistDialog : DetailEvent
    data object MessageShown : DetailEvent
}
