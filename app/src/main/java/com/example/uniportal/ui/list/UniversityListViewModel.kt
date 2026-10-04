package com.example.uniportal.ui.list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniportal.data.connectivity.ConnectivityObserver
import com.example.uniportal.data.repository.ApplicationRepository
import com.example.uniportal.data.repository.FavouritesRepository
import com.example.uniportal.data.repository.OfflineCatalogRepository
import com.example.uniportal.data.repository.SettingsRepository
import com.example.uniportal.data.repository.ShortlistRepository
import com.example.uniportal.data.repository.UniversityRepository
import com.example.uniportal.data.repository.toggle
import com.example.uniportal.domain.model.AppSettings
import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.CachedSearch
import com.example.uniportal.domain.model.CountryFilter
import com.example.uniportal.domain.model.ErrorType
import com.example.uniportal.domain.model.SearchParams
import com.example.uniportal.domain.model.University
import com.example.uniportal.domain.model.toErrorType
import com.example.uniportal.domain.rules.CachePolicy
import com.example.uniportal.util.TimeProvider
import com.example.uniportal.util.WhileUiSubscribed
import com.example.uniportal.util.runCatchingCancellable
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class ReloadReason {
    INITIAL,

    RETRY,

    REFRESH,

    RECONNECTED,
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class UniversityListViewModel @Inject constructor(
    private val repository: UniversityRepository,
    private val favouritesRepository: FavouritesRepository,
    applicationRepository: ApplicationRepository,
    shortlistRepository: ShortlistRepository,
    private val settingsRepository: SettingsRepository,
    offlineCatalogRepository: OfflineCatalogRepository,
    connectivityObserver: ConnectivityObserver,
    private val timeProvider: TimeProvider,
) : ViewModel() {
    private val query = MutableStateFlow("")

    private val country = MutableStateFlow<CountryFilter?>(null)
    private val favouritesOnly = MutableStateFlow(false)

    private val reloadRequests = MutableSharedFlow<ReloadReason>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val loadMoreRequests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val messageShownRequests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val isOnline: Flow<Boolean> = connectivityObserver.isOnline
        .catch { error ->

            Log.w(TAG, "Failed to observe connectivity", error)
            emit(true)
        }

    private val reconnections: Flow<Unit> = isOnline
        .distinctUntilChanged()
        .scan(ConnectivityChange(wasOnline = true, isOnline = true)) { previous, online ->
            ConnectivityChange(wasOnline = previous.isOnline, isOnline = online)
        }
        .filter { change -> change.isOnline && !change.wasOnline }
        .map { }

    private val userMarks: Flow<UserMarks> = combine(
        favouritesRepository.observeFavouriteIds().catch { error ->
            Log.w(TAG, "Failed to observe favourites", error)
            emit(emptySet())
        },
        applicationRepository.observeStatuses().catch { error ->
            Log.w(TAG, "Failed to observe applications", error)
            emit(emptyMap())
        },
        shortlistRepository.observeMembershipCounts().catch { error ->
            Log.w(TAG, "Failed to observe shortlists", error)
            emit(emptyMap())
        },
    ) { favouriteIds, statuses, shortlistCounts -> UserMarks(favouriteIds, statuses, shortlistCounts) }

    private val downloadedCountries: Flow<Set<String>> = offlineCatalogRepository.observeCountries()
        .map { countries -> countries.map { it.countryName.lowercase() }.toSet() }
        .catch { error ->
            Log.w(TAG, "Failed to observe the offline catalogue", error)
            emit(emptySet())
        }

    private val favouriteWrites = Mutex()

    private val searchParams: Flow<SearchParams> = combine(query, country.filterNotNull()) { text, selectedCountry ->
        SearchInput(text, selectedCountry)
    }

        .scan(null as SearchInput?) { previous, input ->
            input.copy(countryChanged = previous != null && previous.country != input.country)
        }
        .filterNotNull()
        .debounce { input -> if (input.text.isBlank() || input.countryChanged) 0L else SEARCH_DEBOUNCE_MS }
        .map { input -> SearchParams(input.text.trim(), input.country) }
        .distinctUntilChanged()

    private val searchState: StateFlow<SearchState> = searchParams
        .withIndex()
        .flatMapLatest { (index, params) -> searchSession(params, version = index + 1) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SearchState())

    private val settingsUnavailable = MutableStateFlow(false)

    val uiState: StateFlow<UniversityListUiState> = combine(
        query,
        country,
        favouritesOnly,
        combine(searchState, settingsUnavailable, ::Pair),
        userMarks,
    ) { text, selectedCountry, onlyFavourites, (search, settingsFailed), marks ->

        val content = if (onlyFavourites) search.content.onlyFavourites(marks.favouriteIds) else search.content
        UniversityListUiState(
            query = text,
            country = selectedCountry ?: CountryFilter.ALL,
            favouritesOnly = onlyFavourites,
            favouriteIds = marks.favouriteIds,
            applicationStatuses = marks.applicationStatuses,
            shortlistCounts = marks.shortlistCounts,
            content = content,
            source = search.source,
            banner = search.banner,
            allHiddenByFavouritesOnly = search.content is ListContent.Success && content is ListContent.Empty,
            isRefreshing = search.isRefreshing,
            isUpdating = search.isUpdating,
            isLoadingMore = search.isLoadingMore,
            canLoadMore = search.canLoadMore,
            loadMoreError = search.loadMoreError,
            message = search.message ?: SearchMessage.SETTINGS_UNAVAILABLE.takeIf { settingsFailed },
            resultsVersion = search.resultsVersion,
        )
    }.stateIn(viewModelScope, WhileUiSubscribed, UniversityListUiState())

    init {
        viewModelScope.launch {
            val homeCountry = currentSettings().homeCountry

            country.compareAndSet(null, homeCountry)
        }
    }

    fun onEvent(event: UniversityListEvent) {
        when (event) {
            is UniversityListEvent.QueryChanged -> query.value = event.query
            is UniversityListEvent.CountrySelected -> country.value = event.country
            is UniversityListEvent.FavouritesOnlyChanged -> favouritesOnly.value = event.enabled
            UniversityListEvent.ClearFilters -> {
                query.value = ""
                country.value = CountryFilter.ALL
                favouritesOnly.value = false
            }
            UniversityListEvent.Retry -> reloadRequests.tryEmit(ReloadReason.RETRY)
            UniversityListEvent.Refresh -> reloadRequests.tryEmit(ReloadReason.REFRESH)
            UniversityListEvent.LoadMore -> loadMoreRequests.tryEmit(Unit)
            UniversityListEvent.MessageShown -> {
                settingsUnavailable.value = false
                messageShownRequests.tryEmit(Unit)
            }
            is UniversityListEvent.ToggleFavourite -> writeFavourite(event.university) {
                favouritesRepository.toggle(event.university)
            }
            is UniversityListEvent.RestoreFavourite -> writeFavourite(event.university) {
                if (!favouritesRepository.observeIsFavourite(event.university.id).first()) {
                    favouritesRepository.add(event.university)
                }
            }
        }
    }

    private fun searchSession(params: SearchParams, version: Int): Flow<SearchState> {
        val cached = repository.observeSearch(params).catch { error ->
            Log.w(TAG, "Failed to read the cached search", error)
            emit(null)
        }
        val localLimit = MutableStateFlow(UniversityRepository.LOCAL_SEARCH_LIMIT)

        val showsLocalCatalog = MutableStateFlow(false)
        val syncStatus = merge<SyncEvent>(
            syncEvents(
                params = params,
                cached = cached,
                loadMoreLocally = {
                    val pagesLocally = showsLocalCatalog.value
                    if (pagesLocally) {
                        localLimit.update { limit -> limit + UniversityRepository.LOCAL_SEARCH_LIMIT }
                    }
                    pagesLocally
                },
            ),
            messageShownRequests.map { SyncEvent.MessageShown },
        ).scan(SyncStatus()) { status, event -> status.reduce(event) }
        val apiCountry = params.country.apiName?.lowercase()
        val countryDownloaded = downloadedCountries
            .map { names -> apiCountry != null && apiCountry in names }
            .distinctUntilChanged()
        return combine(cached, syncStatus, isOnline, countryDownloaded) { cache, status, online, downloaded ->
            SessionInputs(cached = cache, status = status, isOnline = online, countryDownloaded = downloaded)
        }
            .flatMapLatest { inputs ->
                if (inputs.needsLocalFallback) {
                    localLimit.flatMapLatest { limit ->

                        repository.observeLocalSearch(params, limit = limit + 1)
                            .catch { error ->
                                Log.w(TAG, "Failed to search the offline catalogue", error)
                                emit(emptyList())
                            }
                            .map { local ->
                                inputs.toSearchState(
                                    version = version,
                                    localResults = local.take(limit),
                                    hasMoreLocal = local.size > limit,
                                )
                            }
                    }
                } else {
                    flowOf(inputs.toSearchState(version, localResults = emptyList(), hasMoreLocal = false))
                }
            }
            .onEach { state -> showsLocalCatalog.value = state.source == ResultsSource.LOCAL_CATALOG }

            .onStart { emit(SearchState(resultsVersion = version)) }
    }

    private fun syncEvents(
        params: SearchParams,
        cached: Flow<CachedSearch?>,
        loadMoreLocally: () -> Boolean,
    ): Flow<SyncEvent> = channelFlow {
        var lastAttemptFailed = false
        var loadMoreJob: Job? = null

        val cacheLost: Flow<ReloadReason> = cached
            .map { cache -> cache == null }
            .distinctUntilChanged()
            .drop(1)
            .filter { missing -> missing }
            .map { ReloadReason.INITIAL }

        launch {
            loadMoreRequests.collect {
                if (loadMoreLocally()) return@collect
                if (loadMoreJob?.isActive == true) return@collect
                loadMoreJob = launch {
                    if (!isOnlineNow()) {
                        send(SyncEvent.LoadMoreOffline)
                    } else {
                        send(SyncEvent.LoadMoreStarted)
                        runCatchingCancellable { repository.loadNextPage(params) }.fold(
                            onSuccess = { send(SyncEvent.LoadMoreSucceeded) },
                            onFailure = { error -> send(SyncEvent.LoadMoreFailed(error.toErrorType())) },
                        )
                    }
                }
            }
        }

        merge(reloadRequests, reconnections.map { ReloadReason.RECONNECTED }, cacheLost)
            .onStart { emit(ReloadReason.INITIAL) }
            .collectLatest { reason ->
                val automatic = reason == ReloadReason.INITIAL || reason == ReloadReason.RECONNECTED
                when {
                    automatic && !lastAttemptFailed && !isStale(params) -> send(SyncEvent.UpToDate)

                    automatic && !isOnlineNow() -> send(SyncEvent.SkippedOffline)
                    else -> {
                        send(SyncEvent.RefreshStarted(reason))
                        runCatchingCancellable { repository.refreshSearch(params) }.fold(
                            onSuccess = {
                                lastAttemptFailed = false
                                send(SyncEvent.RefreshSucceeded)
                            },
                            onFailure = { error ->
                                lastAttemptFailed = true
                                send(SyncEvent.RefreshFailed(error.toErrorType(), reason))
                            },
                        )
                    }
                }
            }
    }

    private suspend fun isStale(params: SearchParams): Boolean {
        val fetchedAt = runCatchingCancellable { repository.searchFetchedAt(params) }.getOrNull()
        return CachePolicy.isStale(fetchedAt, timeProvider.now(), currentSettings().cacheTtl)
    }

    private suspend fun isOnlineNow(): Boolean = isOnline.first()

    private suspend fun currentSettings(): AppSettings =
        runCatchingCancellable { settingsRepository.settings.first() }
            .onFailure { error ->
                Log.w(TAG, "Failed to read settings", error)
                settingsUnavailable.value = true
            }
            .getOrDefault(AppSettings())

    private fun writeFavourite(university: University, write: suspend () -> Unit) {
        viewModelScope.launch {
            favouriteWrites.withLock {
                runCatchingCancellable { write() }
                    .onFailure { error -> Log.w(TAG, "Failed to update favourite ${university.id}", error) }
            }
        }
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
        private const val TAG = "UniversityListVM"
    }
}

private data class SearchInput(
    val text: String,
    val country: CountryFilter,
    val countryChanged: Boolean = false,
)

private data class ConnectivityChange(val wasOnline: Boolean, val isOnline: Boolean)

private data class UserMarks(
    val favouriteIds: Set<String>,
    val applicationStatuses: Map<String, ApplicationStatus>,
    val shortlistCounts: Map<String, Int>,
)

private sealed interface SyncEvent {
    data class RefreshStarted(val reason: ReloadReason) : SyncEvent
    data object RefreshSucceeded : SyncEvent
    data class RefreshFailed(val error: ErrorType, val reason: ReloadReason) : SyncEvent

    data object UpToDate : SyncEvent

    data object SkippedOffline : SyncEvent
    data object LoadMoreStarted : SyncEvent
    data object LoadMoreSucceeded : SyncEvent
    data class LoadMoreFailed(val error: ErrorType) : SyncEvent
    data object LoadMoreOffline : SyncEvent
    data object MessageShown : SyncEvent
}

private data class SyncStatus(

    val refreshing: ReloadReason? = null,

    val refreshError: ErrorType? = null,

    val skippedOffline: Boolean = false,

    val networkMissed: Boolean = false,
    val isLoadingMore: Boolean = false,
    val loadMoreError: ErrorType? = null,
    val message: SearchMessage? = null,
)

private fun SyncStatus.reduce(event: SyncEvent): SyncStatus = when (event) {
    is SyncEvent.RefreshStarted -> copy(
        refreshing = event.reason,
        message = message.takeUnless { it == SearchMessage.REFRESH_FAILED },
    )
    SyncEvent.RefreshSucceeded -> copy(
        refreshing = null,
        refreshError = null,
        skippedOffline = false,
        loadMoreError = null,
    )
    is SyncEvent.RefreshFailed -> copy(
        refreshing = null,
        refreshError = event.error,
        networkMissed = true,

        message = if (event.reason == ReloadReason.REFRESH) SearchMessage.REFRESH_FAILED else message,
    )
    SyncEvent.UpToDate -> copy(refreshing = null, skippedOffline = false)
    SyncEvent.SkippedOffline -> copy(refreshing = null, skippedOffline = true, networkMissed = true)
    SyncEvent.LoadMoreStarted -> copy(isLoadingMore = true, loadMoreError = null)
    SyncEvent.LoadMoreSucceeded -> copy(isLoadingMore = false)
    is SyncEvent.LoadMoreFailed -> copy(isLoadingMore = false, loadMoreError = event.error)
    SyncEvent.LoadMoreOffline -> copy(message = SearchMessage.LOAD_MORE_OFFLINE)
    SyncEvent.MessageShown -> copy(message = null)
}

private data class SessionInputs(
    val cached: CachedSearch?,
    val status: SyncStatus,
    val isOnline: Boolean,

    val countryDownloaded: Boolean,
) {
    val prefersLocalCatalog: Boolean
        get() = !isOnline && countryDownloaded && (cached == null || cached.hasMore)

    val needsLocalFallback: Boolean
        get() = prefersLocalCatalog || (cached == null && status.networkMissed)

    fun toSearchState(version: Int, localResults: List<University>, hasMoreLocal: Boolean): SearchState {
        val cache = cached
        val refreshing = status.refreshing
        val refreshError = status.refreshError
        val (content, source) = when {
            prefersLocalCatalog -> {
                val localContent = if (localResults.isEmpty()) ListContent.Empty else ListContent.Success(localResults)
                localContent to ResultsSource.LOCAL_CATALOG
            }
            cache != null -> {
                val cachedContent = if (cache.items.isEmpty()) ListContent.Empty else ListContent.Success(cache.items)
                cachedContent to ResultsSource.CACHE
            }
            localResults.isNotEmpty() -> ListContent.Success(localResults) to ResultsSource.LOCAL_CATALOG
            refreshing != null -> ListContent.Loading to ResultsSource.NONE
            refreshError != null -> ListContent.Error(refreshError) to ResultsSource.NONE
            status.skippedOffline -> ListContent.Error(ErrorType.NETWORK) to ResultsSource.NONE
            else -> ListContent.Loading to ResultsSource.NONE
        }
        val isShown = content is ListContent.Success || content is ListContent.Empty
        val fromCache = source == ResultsSource.CACHE
        val banner = when {
            source == ResultsSource.LOCAL_CATALOG -> SearchBanner.LocalCatalog(isOnline = isOnline)
            cache != null && !isOnline -> SearchBanner.Offline(fetchedAt = cache.fetchedAt)
            cache != null && refreshError != null && refreshing == null ->
                SearchBanner.RefreshFailed(fetchedAt = cache.fetchedAt, error = refreshError)
            else -> null
        }
        return SearchState(
            content = content,
            source = source,
            banner = banner,
            isRefreshing = isShown && refreshing == ReloadReason.REFRESH,
            isUpdating = isShown && refreshing != null && refreshing != ReloadReason.REFRESH,
            isLoadingMore = fromCache && status.isLoadingMore,
            canLoadMore = when (source) {
                ResultsSource.CACHE -> cache?.hasMore == true
                ResultsSource.LOCAL_CATALOG -> hasMoreLocal
                ResultsSource.NONE -> false
            },
            loadMoreError = if (fromCache) status.loadMoreError else null,

            message = status.message?.takeUnless { it == SearchMessage.REFRESH_FAILED && !isShown },
            resultsVersion = version,
        )
    }
}

private data class SearchState(
    val content: ListContent = ListContent.Loading,
    val source: ResultsSource = ResultsSource.NONE,
    val banner: SearchBanner? = null,
    val isRefreshing: Boolean = false,
    val isUpdating: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val loadMoreError: ErrorType? = null,
    val message: SearchMessage? = null,
    val resultsVersion: Int = 0,
)

private fun ListContent.onlyFavourites(ids: Set<String>): ListContent {
    if (this !is ListContent.Success) return this
    val favourites = items.filter { it.id in ids }
    return if (favourites.isEmpty()) ListContent.Empty else ListContent.Success(favourites)
}
