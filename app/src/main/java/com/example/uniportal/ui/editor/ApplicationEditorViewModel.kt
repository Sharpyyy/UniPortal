package com.example.uniportal.ui.editor

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniportal.data.repository.ApplicationRepository
import com.example.uniportal.data.repository.UniversityRepository
import com.example.uniportal.domain.model.ErrorType
import com.example.uniportal.domain.model.University
import com.example.uniportal.domain.model.UniversityApplication
import com.example.uniportal.domain.model.toErrorType
import com.example.uniportal.domain.rules.ApplicationRules
import com.example.uniportal.domain.rules.ApplicationValidation
import com.example.uniportal.ui.navigation.Routes
import com.example.uniportal.util.TimeProvider
import com.example.uniportal.util.WhileUiSubscribed
import com.example.uniportal.util.runCatchingCancellable
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ApplicationEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val applicationRepository: ApplicationRepository,
    private val universityRepository: UniversityRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {
    val universityId: String = checkNotNull(savedStateHandle.get<String>(Routes.ARG_UNIVERSITY_ID)) {
        "Route argument '${Routes.ARG_UNIVERSITY_ID}' is missing"
    }

    private val retryRequests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val form = MutableStateFlow<ApplicationForm?>(null)

    private val start = MutableStateFlow<EditorStart?>(null)

    private val operation = MutableStateFlow<EditorOperation>(EditorOperation.Idle)

    private val universityLoad: StateFlow<UniversityLoad> = retryRequests
        .onStart { emit(Unit) }
        .flatMapLatest {
            universityRepository.observeUniversity(universityId)
                .transformLatest<University?, UniversityLoad> { cached ->
                    if (cached != null) {
                        emit(UniversityLoad.Loaded(cached))
                    } else {
                        emit(UniversityLoad.Loading)

                        runCatchingCancellable { universityRepository.refreshUniversity(universityId) }
                            .onFailure { error -> emit(UniversityLoad.Failed(error.toErrorType())) }
                    }
                }
                .catch { error ->
                    Log.w(TAG, "Failed to load university $universityId", error)
                    emit(UniversityLoad.Failed(ErrorType.UNKNOWN))
                }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UniversityLoad.Loading)

    private val storedApplication: Flow<UniversityApplication?> =
        applicationRepository.observeApplication(universityId).catch { error ->
            Log.w(TAG, "Failed to observe the application to $universityId", error)
            emit(null)
        }

    val uiState: StateFlow<ApplicationEditorUiState> = combine(
        universityLoad,
        form,
        start,
        storedApplication,
        operation,
    ) { load, current, opening, stored, currentOperation ->
        val university = (load as? UniversityLoad.Loaded)?.university
        when {
            load is UniversityLoad.Failed -> ApplicationEditorUiState.Error(load.type)
            university == null || current == null || opening == null -> ApplicationEditorUiState.Loading
            else -> editingState(university, current, opening, stored, currentOperation)
        }
    }.stateIn(viewModelScope, WhileUiSubscribed, ApplicationEditorUiState.Loading)

    init {
        viewModelScope.launch {
            val existing = runCatchingCancellable { applicationRepository.observeApplication(universityId).first() }
                .onFailure { error -> Log.w(TAG, "Failed to read the application to $universityId", error) }
                .getOrNull()
            val initialForm = existing?.toForm() ?: ApplicationForm()
            form.compareAndSet(null, initialForm)
            start.value = EditorStart(form = initialForm, existed = existing != null)
        }
    }

    fun onEvent(event: ApplicationEditorEvent) {
        when (event) {
            is ApplicationEditorEvent.StatusSelected -> editForm { it.copy(status = event.status) }
            is ApplicationEditorEvent.DeadlineSelected -> editForm { it.copy(deadline = event.date) }
            is ApplicationEditorEvent.PriorityChanged -> editForm {
                it.copy(priority = event.priority.coerceIn(ApplicationRules.MIN_PRIORITY, ApplicationRules.MAX_PRIORITY))
            }
            is ApplicationEditorEvent.NoteChanged -> editForm { it.copy(note = event.note) }
            ApplicationEditorEvent.Save -> save()
            ApplicationEditorEvent.Delete -> delete()
            ApplicationEditorEvent.Retry -> retryRequests.tryEmit(Unit)
            ApplicationEditorEvent.ErrorShown -> operation.update { current ->
                if (current == EditorOperation.Failed) EditorOperation.Idle else current
            }
        }
    }

    private fun editForm(change: (ApplicationForm) -> ApplicationForm) {
        form.update { current -> current?.let(change) }
    }

    private fun save() {
        val current = form.value ?: return
        val university = (universityLoad.value as? UniversityLoad.Loaded)?.university ?: return
        if (!operation.value.acceptsCommands) return

        if (ApplicationRules.validate(current.note.trim(), current.priority) is ApplicationValidation.Invalid) return
        operation.value = EditorOperation.Saving
        viewModelScope.launch {
            operation.value = runCatchingCancellable {
                applicationRepository.save(
                    university = university,
                    status = current.status,
                    deadline = current.deadline,
                    priority = current.priority,
                    note = current.note,
                )
            }.fold(
                onSuccess = { validation ->
                    if (validation is ApplicationValidation.Valid) {
                        EditorOperation.Done(EditorCompletion.SAVED)
                    } else {
                        EditorOperation.Idle
                    }
                },
                onFailure = { error ->
                    Log.w(TAG, "Failed to save the application to $universityId", error)
                    EditorOperation.Failed
                },
            )
        }
    }

    private fun delete() {
        if (!operation.value.acceptsCommands) return
        operation.value = EditorOperation.Deleting
        viewModelScope.launch {
            operation.value = runCatchingCancellable { applicationRepository.delete(universityId) }.fold(
                onSuccess = { EditorOperation.Done(EditorCompletion.DELETED) },
                onFailure = { error ->
                    Log.w(TAG, "Failed to delete the application to $universityId", error)
                    EditorOperation.Failed
                },
            )
        }
    }

    private fun editingState(
        university: University,
        current: ApplicationForm,
        opening: EditorStart,
        stored: UniversityApplication?,
        currentOperation: EditorOperation,
    ): ApplicationEditorUiState.Editing {
        val isDone = currentOperation is EditorOperation.Done
        val today = timeProvider.today()
        val validation = ApplicationRules.validate(note = current.note.trim(), priority = current.priority)

        val draft = UniversityApplication(
            university = university,
            status = current.status,
            deadline = current.deadline,
            priority = current.priority,
            note = current.note,
            createdAt = stored?.createdAt ?: 0L,
            updatedAt = stored?.updatedAt ?: 0L,
        )
        return ApplicationEditorUiState.Editing(
            university = university,
            form = current,

            isNew = if (isDone) !opening.existed else stored == null,
            errors = (validation as? ApplicationValidation.Invalid)?.errors.orEmpty(),
            daysLeft = ApplicationRules.daysLeft(draft, today),
            isOverdue = ApplicationRules.isOverdue(draft, today),
            today = today,
            hasChanges = current != opening.form,
            isBusy = currentOperation == EditorOperation.Saving || currentOperation == EditorOperation.Deleting,
            createdAt = stored?.createdAt,
            updatedAt = stored?.updatedAt,
            completion = (currentOperation as? EditorOperation.Done)?.completion,
            operationFailed = currentOperation == EditorOperation.Failed,
        )
    }

    private companion object {
        const val TAG = "ApplicationEditorVM"
    }
}

private data class EditorStart(val form: ApplicationForm, val existed: Boolean)

private fun UniversityApplication.toForm(): ApplicationForm = ApplicationForm(
    status = status,
    deadline = deadline,
    priority = priority,
    note = note,
)

private sealed interface UniversityLoad {
    data object Loading : UniversityLoad
    data class Loaded(val university: University) : UniversityLoad
    data class Failed(val type: ErrorType) : UniversityLoad
}

private sealed interface EditorOperation {
    data object Idle : EditorOperation
    data object Saving : EditorOperation
    data object Deleting : EditorOperation
    data class Done(val completion: EditorCompletion) : EditorOperation
    data object Failed : EditorOperation

    val acceptsCommands: Boolean
        get() = this == Idle || this == Failed
}
