package com.example.uniportal.work

import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.uniportal.data.repository.SettingsRepository
import com.example.uniportal.domain.model.CountryFilter
import com.example.uniportal.util.TimeProvider
import com.example.uniportal.util.runCatchingCancellable
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

interface WorkScheduler {
    fun start(scope: CoroutineScope)

    fun syncNow()

    fun downloadCountry(country: CountryFilter)

    fun cancelDownload(country: CountryFilter)

    fun observeSyncState(): Flow<SyncState>

    fun observeDownloads(): Flow<Map<String, DownloadState>>
}

@Singleton
class WorkManagerScheduler @Inject constructor(
    private val workManager: Provider<WorkManager>,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
) : WorkScheduler {
    private val started = AtomicBoolean(false)

    override fun start(scope: CoroutineScope) {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            settingsRepository.settings
                .map { settings -> SyncSchedule(enabled = settings.backgroundSync, wifiOnly = settings.syncWifiOnly) }
                .distinctUntilChanged()
                .catch { error -> Log.w(TAG, "Failed to read sync settings", error) }
                .collect { schedule ->
                    runCatchingCancellable { applySyncSchedule(schedule) }
                        .onFailure { error -> Log.w(TAG, "Failed to schedule the sync", error) }
                }
        }
        scope.launch {
            settingsRepository.settings
                .map { settings -> settings.deadlineReminders }
                .distinctUntilChanged()
                .catch { error -> Log.w(TAG, "Failed to read reminder settings", error) }
                .collect { enabled ->
                    runCatchingCancellable { applyReminderSchedule(enabled) }
                        .onFailure { error -> Log.w(TAG, "Failed to schedule deadline reminders", error) }
                }
        }
    }

    override fun syncNow() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraints(NetworkType.CONNECTED))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, ONE_TIME_BACKOFF_SECONDS, TimeUnit.SECONDS)
            .addTag(TAG_SYNC)
            .build()
        workManager.get().enqueueUniqueWork(SYNC_NOW_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    override fun downloadCountry(country: CountryFilter) {
        val countryName = country.apiName ?: return
        val request = OneTimeWorkRequestBuilder<CountryDownloadWorker>()
            .setInputData(
                workDataOf(
                    CountryDownloadWorker.KEY_COUNTRY_NAME to countryName,
                    CountryDownloadWorker.KEY_COUNTRY_CODE to country.code.orEmpty(),
                ),
            )
            .setConstraints(networkConstraints(NetworkType.CONNECTED))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, ONE_TIME_BACKOFF_SECONDS, TimeUnit.SECONDS)
            .addTag(TAG_COUNTRY_DOWNLOAD)
            .addTag(COUNTRY_TAG_PREFIX + countryName)
            .build()
        workManager.get().enqueueUniqueWork(countryDownloadWorkName(country), ExistingWorkPolicy.KEEP, request)
    }

    override fun cancelDownload(country: CountryFilter) {
        if (country.apiName == null) return
        workManager.get().cancelUniqueWork(countryDownloadWorkName(country))
    }

    override fun observeSyncState(): Flow<SyncState> =
        flow { emitAll(workManager.get().getWorkInfosForUniqueWorkFlow(SYNC_NOW_WORK)) }
            .map { infos -> infos.toSyncState() }
            .distinctUntilChanged()

    override fun observeDownloads(): Flow<Map<String, DownloadState>> =
        flow { emitAll(workManager.get().getWorkInfosByTagFlow(TAG_COUNTRY_DOWNLOAD)) }
            .map { infos -> infos.toDownloadStates() }
            .distinctUntilChanged()

    private fun applySyncSchedule(schedule: SyncSchedule) {
        val manager = workManager.get()
        if (!schedule.enabled) {
            manager.cancelUniqueWork(PERIODIC_SYNC_WORK)
            return
        }
        val networkType = if (schedule.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        val request = PeriodicWorkRequestBuilder<SyncWorker>(SYNC_INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(networkConstraints(networkType))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, PERIODIC_BACKOFF_MINUTES, TimeUnit.MINUTES)
            .addTag(TAG_SYNC)
            .build()

        manager.enqueueUniquePeriodicWork(PERIODIC_SYNC_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    private fun applyReminderSchedule(enabled: Boolean) {
        val manager = workManager.get()
        if (!enabled) {
            manager.cancelUniqueWork(DEADLINE_REMINDERS_WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<DeadlineReminderWorker>(REMINDER_INTERVAL_HOURS, TimeUnit.HOURS)
            .setInitialDelay(millisUntilNextReminderTime(), TimeUnit.MILLISECONDS)
            .build()

        manager.enqueueUniquePeriodicWork(DEADLINE_REMINDERS_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun millisUntilNextReminderTime(): Long {
        val zone = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(timeProvider.now()).atZone(zone)
        var next = now.toLocalDate().atTime(REMINDER_HOUR, 0).atZone(zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis()
    }

    private fun networkConstraints(networkType: NetworkType): Constraints =
        Constraints.Builder().setRequiredNetworkType(networkType).build()

    private data class SyncSchedule(val enabled: Boolean, val wifiOnly: Boolean)

    companion object {
        const val PERIODIC_SYNC_WORK = "uniportal_sync"
        const val SYNC_NOW_WORK = "uniportal_sync_now"
        const val DEADLINE_REMINDERS_WORK = "deadline_reminders"
        const val TAG_SYNC = "sync"
        const val TAG_COUNTRY_DOWNLOAD = "country_download"

        const val COUNTRY_TAG_PREFIX = "country_download:"

        const val SYNC_INTERVAL_HOURS = 12L
        const val REMINDER_INTERVAL_HOURS = 24L
        const val REMINDER_HOUR = 10
        private const val PERIODIC_BACKOFF_MINUTES = 5L
        private const val ONE_TIME_BACKOFF_SECONDS = 30L
        private const val TAG = "WorkScheduler"

        fun countryDownloadWorkName(country: CountryFilter): String =
            "country_download_" + (country.code ?: country.name)
    }
}

internal fun List<WorkInfo>.toSyncState(): SyncState {
    val info = firstOrNull { !it.state.isFinished } ?: firstOrNull() ?: return SyncState.Idle
    return when (info.state) {
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> SyncState.Waiting
        WorkInfo.State.RUNNING -> SyncState.Running
        WorkInfo.State.SUCCEEDED -> SyncState.Succeeded(SyncWorker.reportFromData(info.outputData))
        WorkInfo.State.FAILED -> SyncState.Failed
        WorkInfo.State.CANCELLED -> SyncState.Idle
    }
}

internal fun List<WorkInfo>.toDownloadStates(): Map<String, DownloadState> =
    mapNotNull { info ->
        val countryName = info.tags
            .firstOrNull { it.startsWith(WorkManagerScheduler.COUNTRY_TAG_PREFIX) }
            ?.removePrefix(WorkManagerScheduler.COUNTRY_TAG_PREFIX)
        countryName?.let { it to info }
    }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        .mapNotNull { (countryName, infos) ->
            val info = infos.firstOrNull { !it.state.isFinished } ?: infos.first()
            info.toDownloadState()?.let { state -> countryName to state }
        }
        .toMap()

private fun WorkInfo.toDownloadState(): DownloadState? = when (state) {
    WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadState.Waiting
    WorkInfo.State.RUNNING -> DownloadState.Running(progress.getInt(CountryDownloadWorker.KEY_DOWNLOADED, 0))
    WorkInfo.State.SUCCEEDED -> DownloadState.Succeeded(outputData.getInt(CountryDownloadWorker.KEY_DOWNLOADED, 0))
    WorkInfo.State.FAILED -> DownloadState.Failed
    WorkInfo.State.CANCELLED -> null
}
