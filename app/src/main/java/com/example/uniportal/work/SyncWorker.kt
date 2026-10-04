package com.example.uniportal.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.uniportal.data.sync.SyncReport
import com.example.uniportal.data.sync.UniversitySyncer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import kotlinx.coroutines.CancellationException

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncer: UniversitySyncer,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result =
        try {
            Result.success(reportToData(syncer.sync()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.w(TAG, "Sync failed, attempt ${runAttemptCount + 1}", e)
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            Result.failure()
        }

    companion object {
        const val KEY_REFRESHED = "refreshed"
        const val KEY_FAILED = "failed"
        const val KEY_PRUNED = "pruned"

        const val MAX_ATTEMPTS = 3

        private const val TAG = "SyncWorker"

        fun reportToData(report: SyncReport): Data = workDataOf(
            KEY_REFRESHED to report.refreshed,
            KEY_FAILED to report.failed,
            KEY_PRUNED to report.pruned,
        )

        fun reportFromData(data: Data): SyncReport = SyncReport(
            refreshed = data.getInt(KEY_REFRESHED, 0),
            failed = data.getInt(KEY_FAILED, 0),
            pruned = data.getInt(KEY_PRUNED, 0),
        )
    }
}
