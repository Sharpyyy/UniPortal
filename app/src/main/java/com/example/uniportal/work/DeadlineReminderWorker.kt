package com.example.uniportal.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.uniportal.data.sync.DeadlineChecker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class DeadlineReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val checker: DeadlineChecker,
    private val notifier: DeadlineNotifier,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result =
        try {
            val due = checker.check()
            if (due.isNotEmpty()) notifier.notifyDeadlines(due)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Deadline check failed", e)
            Result.failure()
        }

    private companion object {
        const val TAG = "DeadlineReminderWorker"
    }
}
