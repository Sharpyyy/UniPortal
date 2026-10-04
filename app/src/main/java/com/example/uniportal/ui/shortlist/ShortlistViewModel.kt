package com.example.uniportal.ui.shortlist

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniportal.data.repository.ApplicationRepository
import com.example.uniportal.data.repository.ShortlistRepository
import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.ErrorType
import com.example.uniportal.domain.model.Shortlist
import com.example.uniportal.domain.model.ShortlistDetails
import com.example.uniportal.domain.model.toErrorType
import com.example.uniportal.domain.rules.ShortlistSaveResult
import com.example.uniportal.ui.navigation.Routes
import com.example.uniportal.util.WhileUiSubscribed
import com.example.uniportal.util.runCatchingCancellable
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShortlistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ShortlistRepository,
    applicationRepository: ApplicationRepository,
) : ViewModel() {
    val shortlistId: Long = checkNotNull(savedStateHandle.get<Long>(Routes.ARG_SHORTLIST_ID)) {
        "Route argument '${Routes.ARG_SHORTLIST_ID}' is missing"
    }

    private val retryRequests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val formInput = MutableStateFlow<ShortlistFormInput?>(null)

    private val operation = MutableStateFlow(ShortlistOperation.IDLE)

    private val message = MutableStateFlow<ShortlistMessage?>(null)

    private val writes = Mutex()

    private val details: Flow<DetailsLoad> = retryRequests
        .onStart { emit(Unit) }
        .flatMapLatest {
            repository.observeShortlist(shortlistId)
                .map<ShortlistDetails?, DetailsLoad> { found -> DetailsLoad.Loaded(found) }
                .catch { error ->
                    Log.w(TAG, "Failed to observe shortlist $shortlistId", error)
                    emit(DetailsLoad.Failed(error.toErrorType()))
                }
        }

    private val content: Flow<ShortlistContent> = combine(details, operation) { load, currentOperation ->
        ContentInputs(load, currentOperation)
    }.scan<ContentInputs, ShortlistContent>(ShortlistContent.Loading) { previous, inputs -> inputs.toContent(previous) }

    private val statuses: Flow<Map<String, ApplicationStatus>> = applicationRepository.observeStatuses()
        .catch { error ->
            Log.w(TAG, "Failed to observe applications", error)
            emit(emptyMap())
        }
        .onStart { emit(emptyMap()) }

    private val otherNames: Flow<List<String>> = repository.observeShortlists()
        .map { shortlists: List<Shortlist> -> shortlists.filter { it.id != shortlistId }.map { it.name } }
        .catch { error ->
            Log.w(TAG, "Failed to observe shortlists", error)
            emit(emptyList())
        }

    private val form: Flow<ShortlistFormState?> = combine(formInput, otherNames) { input, names ->
        input?.toFormState(otherNames = names)
    }

    val uiState: StateFlow<ShortlistUiState> = combine(
        content,
        statuses,
        form,
        operation,
        message,
    ) { currentContent, applicationStatuses, currentForm, currentOperation, currentMessage ->
        ShortlistUiState(
            content = currentContent,
            applicationStatuses = applicationStatuses,
            form = currentForm,
            isDeleting = currentOperation == ShortlistOperation.DELETING,
            isDeleted = currentOperation == ShortlistOperation.DELETED,
            message = currentMessage,
        )
    }.stateIn(viewModelScope, WhileUiSubscribed, ShortlistUiState())

    fun onEvent(event: ShortlistEvent) {
        when (event) {
            ShortlistEvent.Retry -> retryRequests.tryEmit(Unit)
            is ShortlistEvent.RemoveItem -> write("remove ${event.item.university.id}") {
                repository.remove(shortlistId, event.item.university.id)
            }
            is ShortlistEvent.RestoreItem -> write("restore ${event.item.university.id}") {
                repository.restore(shortlistId, event.item.university, event.item.addedAt)
            }
            ShortlistEvent.OpenEditForm -> openEditForm()
            is ShortlistEvent.Form -> when (event.event) {
                ShortlistFormEvent.Submit -> submit()
                ShortlistFormEvent.Dismiss -> formInput.update { current -> current?.takeIf { it.isSaving } }
                else -> formInput.update { current -> current?.edit(event.event) }
            }
            ShortlistEvent.Delete -> delete()
            ShortlistEvent.MessageShown -> message.value = null
        }
    }

    private fun openEditForm() {
        if (formInput.value != null) return
        val shortlist = (uiState.value.content as? ShortlistContent.Success)?.details?.shortlist ?: return
        formInput.value = ShortlistFormInput(
            editingId = shortlist.id,
            name = shortlist.name,
            emoji = shortlist.emoji,
            description = shortlist.description,
        )
    }

    private fun submit() {
        val input = formInput.value ?: return
        if (input.isSaving) return
        formInput.value = input.copy(isSaving = true, submitError = null)
        viewModelScope.launch {
            val result = runCatchingCancellable {
                repository.update(
                    id = shortlistId,
                    name = input.name,
                    emoji = input.emoji,
                    description = input.description,
                )
            }
            result.onFailure { error -> Log.w(TAG, "Failed to update shortlist $shortlistId", error) }
            when (val saved = result.getOrNull()) {
                is ShortlistSaveResult.Saved -> {
                    formInput.value = null
                    message.value = ShortlistMessage.Saved
                }
                is ShortlistSaveResult.Invalid -> formInput.update { current ->
                    current?.copy(isSaving = false, submitError = saved.error, nameTouched = true)
                }
                null -> {
                    formInput.update { current -> current?.copy(isSaving = false) }
                    message.value = ShortlistMessage.SaveFailed
                }
            }
        }
    }

    private fun delete() {
        if (operation.value != ShortlistOperation.IDLE) return
        operation.value = ShortlistOperation.DELETING
        viewModelScope.launch {
            operation.value = runCatchingCancellable { repository.delete(shortlistId) }.fold(
                onSuccess = { ShortlistOperation.DELETED },
                onFailure = { error ->
                    Log.w(TAG, "Failed to delete shortlist $shortlistId", error)
                    message.value = ShortlistMessage.SaveFailed
                    ShortlistOperation.IDLE
                },
            )
        }
    }

    private fun write(description: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            writes.withLock {
                runCatchingCancellable { action() }
                    .onFailure { error ->
                        Log.w(TAG, "Failed to $description in shortlist $shortlistId", error)
                        message.value = ShortlistMessage.SaveFailed
                    }
            }
        }
    }

    private companion object {
        const val TAG = "ShortlistVM"
    }
}

private enum class ShortlistOperation {
    IDLE,
    DELETING,
    DELETED,
    ;

    val isClosing: Boolean
        get() = this != IDLE
}

private sealed interface DetailsLoad {
    data class Loaded(val details: ShortlistDetails?) : DetailsLoad
    data class Failed(val type: ErrorType) : DetailsLoad
}

private data class ContentInputs(val load: DetailsLoad, val operation: ShortlistOperation) {
    fun toContent(previous: ShortlistContent): ShortlistContent = when (load) {
        is DetailsLoad.Failed -> ShortlistContent.Error(load.type)
        is DetailsLoad.Loaded -> {
            val found = load.details
            when {
                found != null -> ShortlistContent.Success(found)

                operation.isClosing && previous is ShortlistContent.Success -> previous
                else -> ShortlistContent.NotFound
            }
        }
    }
}
