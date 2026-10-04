package com.example.uniportal.data.sync

import android.util.Log
import com.example.uniportal.data.repository.SettingsRepository
import com.example.uniportal.data.repository.UniversityRepository
import com.example.uniportal.domain.model.SearchParams
import com.example.uniportal.domain.model.UniversityNotFoundException
import com.example.uniportal.domain.rules.CachePolicy
import com.example.uniportal.util.TimeProvider
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

data class SyncReport(
    val refreshed: Int,
    val failed: Int,
    val pruned: Int,
)

interface UniversitySyncer {
    suspend fun sync(): SyncReport
}

class DefaultUniversitySyncer @Inject constructor(
    private val repository: UniversityRepository,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
) : UniversitySyncer {
    override suspend fun sync(): SyncReport {
        val settings = settingsRepository.settings.first()
        val now = timeProvider.now()
        val network = NetworkTally()

        var refreshed = 0
        var failed = 0
        val staleIds = repository.stalePinnedIds(
            staleBefore = now - settings.cacheTtl.millis,
            limit = MAX_REFRESHES_PER_RUN,
        )
        for (id in staleIds) {
            if (network.looksDown) {
                failed += 1
                continue
            }
            try {
                repository.refreshUniversity(id)
                network.onResponse()
                refreshed += 1
            } catch (e: CancellationException) {
                throw e
            } catch (e: UniversityNotFoundException) {
                network.onResponse()
                failed += 1
            } catch (e: IOException) {
                network.onFailure(e)
                failed += 1
            } catch (e: Exception) {
                Log.w(TAG, "Failed to refresh $id", e)
                network.onResponse()
                failed += 1
            }
        }

        val homeSearch = SearchParams(query = "", country = settings.homeCountry)
        val homeSearchStale = CachePolicy.isStale(repository.searchFetchedAt(homeSearch), now, settings.cacheTtl)
        if (homeSearchStale && !network.looksDown) {
            try {
                repository.refreshSearch(homeSearch)
                network.onResponse()
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                network.onFailure(e)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to prefetch the home search", e)
                network.onResponse()
            }
        }

        val pruned = repository.pruneCache(olderThan = now - CachePolicy.RETENTION_MILLIS)

        network.failureIfUnavailable()?.let { throw it }
        settingsRepository.setLastSyncAt(now)
        return SyncReport(refreshed = refreshed, failed = failed, pruned = pruned)
    }

    private class NetworkTally {
        private var responses = 0
        private var consecutiveFailures = 0
        private var lastFailure: IOException? = null

        val looksDown: Boolean
            get() = consecutiveFailures >= DefaultUniversitySyncer.MAX_CONSECUTIVE_NETWORK_FAILURES

        fun onResponse() {
            responses += 1
            consecutiveFailures = 0
        }

        fun onFailure(error: IOException) {
            consecutiveFailures += 1
            lastFailure = error
        }

        fun failureIfUnavailable(): IOException? = lastFailure?.takeIf { responses == 0 }
    }

    companion object {
        const val MAX_REFRESHES_PER_RUN = 30
        const val MAX_CONSECUTIVE_NETWORK_FAILURES = 3
        private const val TAG = "UniversitySyncer"
    }
}
