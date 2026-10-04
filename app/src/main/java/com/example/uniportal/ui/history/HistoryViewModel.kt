package com.example.uniportal.ui.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniportal.data.repository.HistoryRepository
import com.example.uniportal.data.repository.SettingsRepository
import com.example.uniportal.domain.model.AppSettings
import com.example.uniportal.domain.model.HistoryEntry
import com.example.uniportal.domain.model.toErrorType
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
class HistoryViewModel @Inject constructor(
    private val repository: HistoryRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val retryRequests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val writes = Mutex()

    private val content: Flow<HistoryContent> = retryRequests
        .map { true }
        .onStart { emit(false) }
        .flatMapLatest { isRetry ->

            if (isRetry) observeHistory().onStart { emit(HistoryContent.Loading) } else observeHistory()
        }

    private val historyLimit: Flow<Int> = settingsRepository.settings
        .map { settings -> settings.historyLimit }
        .catch { error ->
            Log.w(TAG, "Failed to read the history limit", error)
            emit(AppSettings.DEFAULT_HISTORY_LIMIT)
        }
        .distinctUntilChanged()

    val uiState: StateFlow<HistoryUiState> = combine(content, historyLimit) { current, limit ->
        HistoryUiState(content = current.limitedTo(limit), limit = limit)
    }.stateIn(viewModelScope, WhileUiSubscribed, HistoryUiState())

    fun onEvent(event: HistoryEvent) {
        when (event) {
            HistoryEvent.Retry -> retryRequests.tryEmit(Unit)
            is HistoryEvent.Remove -> write { repository.remove(event.universityId) }
            is HistoryEvent.Restore -> write { repository.restore(event.entry) }
            HistoryEvent.ClearAll -> write { repository.clear() }
        }
    }

    private fun observeHistory(): Flow<HistoryContent> = repository.observeHistory()
        .map { entries -> entries.toContent() }
        .catch { error ->
            Log.w(TAG, "Failed to read the history", error)
            emit(HistoryContent.Error(error.toErrorType()))
        }

    private fun write(action: suspend () -> Unit) {
        viewModelScope.launch {
            writes.withLock {
                runCatchingCancellable { action() }
                    .onFailure { error -> Log.w(TAG, "Failed to update the history", error) }
            }
        }
    }

    private fun List<HistoryEntry>.toContent(): HistoryContent =
        if (isEmpty()) HistoryContent.Empty else HistoryContent.Success(this)

    private companion object {
        const val TAG = "HistoryVM"
    }
}

private fun HistoryContent.limitedTo(limit: Int): HistoryContent =
    if (this is HistoryContent.Success && entries.size > limit) HistoryContent.Success(entries.take(limit)) else this
