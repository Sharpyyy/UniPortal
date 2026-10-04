package com.example.uniportal.ui.lists

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniportal.data.repository.ShortlistRepository
import com.example.uniportal.domain.model.ErrorType
import com.example.uniportal.domain.model.Shortlist
import com.example.uniportal.domain.model.toErrorType
import com.example.uniportal.domain.rules.ShortlistRules
import com.example.uniportal.domain.rules.ShortlistSaveResult
import com.example.uniportal.ui.shortlist.ShortlistFormEvent
import com.example.uniportal.ui.shortlist.ShortlistFormInput
import com.example.uniportal.ui.shortlist.edit
import com.example.uniportal.ui.shortlist.toFormState
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
class ShortlistsViewModel @Inject constructor(
    private val repository: ShortlistRepository,
) : ViewModel() {
    private val retryRequests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val formInput = MutableStateFlow<ShortlistFormInput?>(null)

    private val message = MutableStateFlow<ShortlistsMessage?>(null)

    private val shortlists: Flow<ShortlistsLoad> = retryRequests
        .onStart { emit(Unit) }
        .flatMapLatest {
            repository.observeShortlists()
                .map<List<Shortlist>, ShortlistsLoad> { list -> ShortlistsLoad.Loaded(list) }
                .catch { error ->
                    Log.w(TAG, "Failed to observe shortlists", error)
                    emit(ShortlistsLoad.Failed(error.toErrorType()))
                }
        }

    val uiState: StateFlow<ShortlistsUiState> = combine(
        shortlists,
        formInput,
        message,
    ) { load, input, currentMessage ->
        val stored = (load as? ShortlistsLoad.Loaded)?.shortlists.orEmpty()
        ShortlistsUiState(
            content = when (load) {
                is ShortlistsLoad.Failed -> ShortlistsContent.Error(load.type)
                is ShortlistsLoad.Loaded -> if (stored.isEmpty()) {
                    ShortlistsContent.Empty
                } else {
                    ShortlistsContent.Success(stored)
                }
            },

            form = input?.toFormState(otherNames = stored.map { it.name }),
            message = currentMessage,
        )
    }.stateIn(viewModelScope, WhileUiSubscribed, ShortlistsUiState())

    fun onEvent(event: ShortlistsEvent) {
        when (event) {
            ShortlistsEvent.Retry -> retryRequests.tryEmit(Unit)
            ShortlistsEvent.OpenCreateForm -> openForm()
            is ShortlistsEvent.Form -> when (event.event) {
                ShortlistFormEvent.Submit -> submit()
                ShortlistFormEvent.Dismiss -> formInput.update { current -> current?.takeIf { it.isSaving } }
                else -> formInput.update { current -> current?.edit(event.event) }
            }
            ShortlistsEvent.MessageShown -> message.value = null
        }
    }

    private fun openForm() {
        if (formInput.value != null) return

        val options = ShortlistRules.EMOJI_OPTIONS
        val count = uiState.value.shortlistCount
        formInput.value = ShortlistFormInput(emoji = options[count % options.size])
    }

    private fun submit() {
        val input = formInput.value ?: return
        if (input.isSaving) return
        formInput.value = input.copy(isSaving = true, submitError = null)
        viewModelScope.launch {
            val result = runCatchingCancellable {
                repository.create(name = input.name, emoji = input.emoji, description = input.description)
            }
            result.onFailure { error -> Log.w(TAG, "Failed to create a shortlist", error) }
            when (val saved = result.getOrNull()) {
                is ShortlistSaveResult.Saved -> {
                    formInput.value = null
                    message.value = ShortlistsMessage.Created(id = saved.id, name = input.name.trim())
                }
                is ShortlistSaveResult.Invalid -> formInput.update { current ->
                    current?.copy(isSaving = false, submitError = saved.error, nameTouched = true)
                }
                null -> {
                    formInput.update { current -> current?.copy(isSaving = false) }
                    message.value = ShortlistsMessage.SaveFailed
                }
            }
        }
    }

    private companion object {
        const val TAG = "ShortlistsVM"
    }
}

private sealed interface ShortlistsLoad {
    data class Loaded(val shortlists: List<Shortlist>) : ShortlistsLoad
    data class Failed(val type: ErrorType) : ShortlistsLoad
}
