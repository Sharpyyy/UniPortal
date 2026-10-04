package com.example.uniportal.ui.detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniportal.data.connectivity.ConnectivityObserver
import com.example.uniportal.data.repository.ApplicationRepository
import com.example.uniportal.data.repository.FavouritesRepository
import com.example.uniportal.data.repository.HistoryRepository
import com.example.uniportal.data.repository.SettingsRepository
import com.example.uniportal.data.repository.ShortlistRepository
import com.example.uniportal.data.repository.UniversityRepository
import com.example.uniportal.data.repository.toggle
import com.example.uniportal.domain.model.AppSettings
import com.example.uniportal.domain.model.ErrorType
import com.example.uniportal.domain.model.Shortlist
import com.example.uniportal.domain.model.University
import com.example.uniportal.domain.model.UniversityApplication
import com.example.uniportal.domain.model.toErrorType
import com.example.uniportal.domain.rules.ApplicationRules
import com.example.uniportal.domain.rules.CachePolicy
import com.example.uniportal.domain.rules.ShortlistRules
import com.example.uniportal.domain.rules.ShortlistSaveResult
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UniversityDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val universityRepository: UniversityRepository,
    private val historyRepository: HistoryRepository,
    private val favouritesRepository: FavouritesRepository,
    private val shortlistRepository: ShortlistRepository,
    private val applicationRepository: ApplicationRepository,
    private val settingsRepository: SettingsRepository,
    connectivityObserver: ConnectivityObserver,
    private val timeProvider: TimeProvider,
) : ViewModel() {
    val universityId: String = checkNotNull(savedStateHandle.get<String>(Routes.ARG_ID)) {
        "Route argument '${Routes.ARG_ID}' is missing"
    }

    private val retryRequests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val writes = Mutex()

    private val shortlistDialog = MutableStateFlow<ShortlistDialogState?>(null)

    private val message = MutableStateFlow<DetailMessage?>(null)

    private val isOnline: Flow<Boolean> = connectivityObserver.isOnline
        .catch { error ->
            Log.w(TAG, "Failed to observe connectivity", error)
            emit(true)
        }

    private val reconnections: Flow<Unit> = isOnline
        .distinctUntilChanged()
        .scan(true to true) { (_, wasOnline), online -> wasOnline to online }
        .filter { (wasOnline, online) -> online && !wasOnline }
        .map { }

    private val cachedUniversity: StateFlow<University?> = universityRepository.observeUniversity(universityId)
        .catch { error ->
            Log.w(TAG, "Failed to read cached university $universityId", error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var lastRefreshFailed = false

    private val refreshStatus: StateFlow<RefreshStatus> = merge(
        retryRequests.map { RefreshTrigger.RETRY },
        reconnections.map { RefreshTrigger.RECONNECTED },
    )
        .onStart { emit(RefreshTrigger.INITIAL) }
        .flatMapLatest { trigger -> refresh(trigger) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RefreshStatus.Idle)

    private val universityPart: Flow<UniversityPart> = combine(cachedUniversity, refreshStatus) { university, refresh ->
        university to refresh
    }.mapLatest { (university, refresh) ->
        val fetchedAt = if (university == null) {
            null
        } else {
            runCatchingCancellable { universityRepository.cachedUniversityFetchedAt(universityId) }.getOrNull()
        }
        UniversityPart(university = university, refresh = refresh, fetchedAt = fetchedAt)
    }

    private val userPart: Flow<UserPart> = combine(
        favouritesRepository.observeIsFavourite(universityId).catch { error ->
            Log.w(TAG, "Failed to observe favourite $universityId", error)
            emit(false)
        },
        applicationRepository.observeApplication(universityId).catch { error ->
            Log.w(TAG, "Failed to observe the application to $universityId", error)
            emit(null)
        },
        shortlistRepository.observeMembership(universityId).catch { error ->
            Log.w(TAG, "Failed to observe shortlists of $universityId", error)
            emit(emptySet())
        },
        shortlistRepository.observeShortlists().catch { error ->
            Log.w(TAG, "Failed to observe shortlists", error)
            emit(emptyList())
        },
    ) { favourite, application, membership, shortlists ->
        UserPart(isFavourite = favourite, application = application, membership = membership, shortlists = shortlists)
    }

    val uiState: StateFlow<DetailUiState> = combine(
        universityPart,
        userPart,
        shortlistDialog,
        message,
    ) { universityState, user, dialog, currentMessage ->
        val university = universityState.university
        val refresh = universityState.refresh
        val content = when {
            university != null -> DetailContent.Success(university)
            refresh is RefreshStatus.Failed -> DetailContent.Error(refresh.type)
            refresh is RefreshStatus.Offline -> DetailContent.Error(ErrorType.NETWORK)
            else -> DetailContent.Loading
        }
        val today = timeProvider.today()
        DetailUiState(
            content = content,
            fetchedAt = universityState.fetchedAt,
            isRefreshing = university != null && refresh is RefreshStatus.Refreshing,

            refreshFailed = university != null && (
                refresh is RefreshStatus.Offline ||
                    (refresh is RefreshStatus.Failed && refresh.type != ErrorType.NOT_FOUND)
                ),
            isFavourite = user.isFavourite,
            application = user.application?.let { application ->
                ApplicationSummary(
                    application = application,
                    daysLeft = ApplicationRules.daysLeft(application, today),
                    isOverdue = ApplicationRules.isOverdue(application, today),
                    nextStatus = ApplicationRules.nextStep(application.status),
                )
            },
            shortlists = user.shortlists,
            memberShortlistIds = user.membership,
            shortlistDialog = dialog,
            message = currentMessage,
        )
    }.stateIn(viewModelScope, WhileUiSubscribed, DetailUiState())

    init {
        viewModelScope.launch {
            val university = cachedUniversity.filterNotNull().first()
            val limit = currentSettings().historyLimit
            runCatchingCancellable { historyRepository.recordView(university, limit) }
                .onFailure { error -> Log.w(TAG, "Failed to record a view of ${university.id}", error) }
        }
    }

    fun onEvent(event: DetailEvent) {
        when (event) {
            DetailEvent.Retry -> retryRequests.tryEmit(Unit)
            DetailEvent.ToggleFavourite -> toggleFavourite()
            DetailEvent.AdvanceStatus -> advanceStatus()
            DetailEvent.OpenShortlistDialog -> openShortlistDialog()
            is DetailEvent.ShortlistToggled -> shortlistDialog.update { dialog ->
                dialog?.let { it.copy(selectedIds = it.selectedIds.toggled(event.shortlistId)) }
            }
            is DetailEvent.NewShortlistNameChanged -> shortlistDialog.update { dialog ->
                dialog?.let { it.copy(newName = event.name, nameError = null) }
            }
            DetailEvent.CreateShortlist -> createShortlist()
            DetailEvent.ConfirmShortlists -> confirmShortlists()
            DetailEvent.DismissShortlistDialog -> shortlistDialog.value = null
            DetailEvent.MessageShown -> message.value = null
        }
    }

    private fun refresh(trigger: RefreshTrigger): Flow<RefreshStatus> = flow {
        val forced = trigger == RefreshTrigger.RETRY
        if (!forced && !lastRefreshFailed && !isStale()) {
            emit(RefreshStatus.Idle)
            return@flow
        }

        if (!forced && !isOnline.first()) {
            emit(RefreshStatus.Offline)
            return@flow
        }
        emit(RefreshStatus.Refreshing)
        runCatchingCancellable { universityRepository.refreshUniversity(universityId) }.fold(
            onSuccess = {
                lastRefreshFailed = false
                emit(RefreshStatus.Idle)
            },
            onFailure = { error ->
                lastRefreshFailed = true
                emit(RefreshStatus.Failed(error.toErrorType()))
            },
        )
    }

    private suspend fun isStale(): Boolean {
        val fetchedAt = runCatchingCancellable { universityRepository.cachedUniversityFetchedAt(universityId) }
            .getOrNull()
        return CachePolicy.isStale(fetchedAt, timeProvider.now(), currentSettings().cacheTtl)
    }

    private suspend fun currentSettings(): AppSettings =
        runCatchingCancellable { settingsRepository.settings.first() }
            .onFailure { error ->
                Log.w(TAG, "Failed to read settings", error)
                message.value = DetailMessage.SettingsUnavailable
            }
            .getOrDefault(AppSettings())

    private fun toggleFavourite() {
        val university = cachedUniversity.value ?: return
        write("update favourite ${university.id}") { favouritesRepository.toggle(university) }
    }

    private fun advanceStatus() {
        val next = uiState.value.application?.nextStatus ?: return
        write("advance the application to $next") {
            applicationRepository.updateStatus(universityId, next)
            message.value = DetailMessage.StatusChanged(next)
        }
    }

    private fun openShortlistDialog() {
        if (cachedUniversity.value == null) return
        shortlistDialog.value = ShortlistDialogState(selectedIds = uiState.value.memberShortlistIds)
    }

    private fun createShortlist() {
        val dialog = shortlistDialog.value ?: return
        if (dialog.isCreating) return
        shortlistDialog.value = dialog.copy(isCreating = true, nameError = null)
        viewModelScope.launch {
            val result = createFromName(dialog.newName)
            shortlistDialog.update { current ->
                current?.let { open -> open.afterCreate(result) }
            }
        }
    }

    private fun confirmShortlists() {
        val dialog = shortlistDialog.value ?: return
        if (dialog.isCreating) return
        val university = cachedUniversity.value ?: return
        if (dialog.newName.isBlank()) {
            shortlistDialog.value = null
            saveMembership(university, dialog.selectedIds)
            return
        }
        shortlistDialog.value = dialog.copy(isCreating = true, nameError = null)
        viewModelScope.launch {
            val result = createFromName(dialog.newName)
            if (result is ShortlistSaveResult.Saved) {
                val current = shortlistDialog.value ?: return@launch
                shortlistDialog.value = null
                saveMembership(university, current.selectedIds + result.id)
            } else {
                shortlistDialog.update { current -> current?.afterCreate(result) }
            }
        }
    }

    private suspend fun createFromName(name: String): ShortlistSaveResult? {
        val options = ShortlistRules.EMOJI_OPTIONS
        val emoji = options[uiState.value.shortlists.size % options.size]
        return runCatchingCancellable {
            shortlistRepository.create(name = name, emoji = emoji, description = "")
        }.onFailure { error ->
            Log.w(TAG, "Failed to create a shortlist", error)
            message.value = DetailMessage.SaveFailed
        }.getOrNull()
    }

    private fun saveMembership(university: University, selectedIds: Set<Long>) {
        if (selectedIds == uiState.value.memberShortlistIds) return
        write("save shortlists of ${university.id}") {
            shortlistRepository.setMembership(university, selectedIds)
            message.value = DetailMessage.ShortlistsSaved(selectedIds.size)
        }
    }

    private fun write(description: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            writes.withLock {
                runCatchingCancellable { action() }
                    .onFailure { error ->
                        Log.w(TAG, "Failed to $description", error)
                        message.value = DetailMessage.SaveFailed
                    }
            }
        }
    }

    private companion object {
        const val TAG = "UniversityDetailVM"
    }
}

private enum class RefreshTrigger { INITIAL, RETRY, RECONNECTED }

private sealed interface RefreshStatus {
    data object Idle : RefreshStatus
    data object Refreshing : RefreshStatus

    data object Offline : RefreshStatus
    data class Failed(val type: ErrorType) : RefreshStatus
}

private data class UniversityPart(
    val university: University?,
    val refresh: RefreshStatus,
    val fetchedAt: Long?,
)

private data class UserPart(
    val isFavourite: Boolean,
    val application: UniversityApplication?,
    val membership: Set<Long>,
    val shortlists: List<Shortlist>,
)

private fun Set<Long>.toggled(id: Long): Set<Long> = if (id in this) this - id else this + id

private fun ShortlistDialogState.afterCreate(result: ShortlistSaveResult?): ShortlistDialogState = when (result) {
    is ShortlistSaveResult.Saved -> copy(
        selectedIds = selectedIds + result.id,
        newName = "",
        nameError = null,
        isCreating = false,
    )
    is ShortlistSaveResult.Invalid -> copy(nameError = result.error, isCreating = false)
    null -> copy(isCreating = false)
}
