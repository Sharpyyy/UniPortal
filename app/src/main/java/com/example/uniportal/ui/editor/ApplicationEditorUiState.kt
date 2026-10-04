package com.example.uniportal.ui.editor

import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.ErrorType
import com.example.uniportal.domain.model.University
import com.example.uniportal.domain.rules.ApplicationError
import com.example.uniportal.domain.rules.ApplicationRules
import java.time.LocalDate

data class ApplicationForm(
    val status: ApplicationStatus = ApplicationStatus.INTERESTED,
    val deadline: LocalDate? = null,
    val priority: Int = ApplicationRules.DEFAULT_PRIORITY,
    val note: String = "",
)

sealed interface ApplicationEditorUiState {
    data object Loading : ApplicationEditorUiState

    data class Error(val type: ErrorType) : ApplicationEditorUiState

    data class Editing(
        val university: University,
        val form: ApplicationForm,

        val isNew: Boolean,

        val errors: Set<ApplicationError>,

        val daysLeft: Int?,
        val isOverdue: Boolean,
        val today: LocalDate,

        val hasChanges: Boolean,

        val isBusy: Boolean,

        val createdAt: Long? = null,
        val updatedAt: Long? = null,

        val completion: EditorCompletion? = null,

        val operationFailed: Boolean = false,
    ) : ApplicationEditorUiState {
        val noteLength: Int
            get() = form.note.trim().length

        val canSave: Boolean
            get() = errors.isEmpty() && !isBusy && completion == null && (isNew || hasChanges)
    }
}

enum class EditorCompletion { SAVED, DELETED }

sealed interface ApplicationEditorEvent {
    data class StatusSelected(val status: ApplicationStatus) : ApplicationEditorEvent

    data class DeadlineSelected(val date: LocalDate?) : ApplicationEditorEvent
    data class PriorityChanged(val priority: Int) : ApplicationEditorEvent
    data class NoteChanged(val note: String) : ApplicationEditorEvent
    data object Save : ApplicationEditorEvent

    data object Delete : ApplicationEditorEvent
    data object Retry : ApplicationEditorEvent
    data object ErrorShown : ApplicationEditorEvent
}
