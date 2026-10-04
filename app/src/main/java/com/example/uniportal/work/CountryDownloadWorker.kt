package com.example.uniportal.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.uniportal.data.sync.CountryDownloader
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import kotlinx.coroutines.CancellationException

@HiltWorker
class CountryDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val downloader: CountryDownloader,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val countryName = inputData.getString(KEY_COUNTRY_NAME)
        if (countryName.isNullOrBlank()) return Result.failure()
        val countryCode = inputData.getString(KEY_COUNTRY_CODE).orEmpty()

        return try {
            setProgress(workDataOf(KEY_DOWNLOADED to 0))
            val downloaded = downloader.download(countryName, countryCode) { count ->
                setProgress(workDataOf(KEY_DOWNLOADED to count))
            }
            Result.success(workDataOf(KEY_DOWNLOADED to downloaded))
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.w(TAG, "Download of $countryName failed, attempt ${runAttemptCount + 1}", e)
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "Download of $countryName failed", e)
            Result.failure()
        }
    }

    companion object {
        const val KEY_COUNTRY_NAME = "country_name"
        const val KEY_COUNTRY_CODE = "country_code"
        const val KEY_DOWNLOADED = "downloaded"
        const val MAX_ATTEMPTS = 3
        private const val TAG = "CountryDownloadWorker"
    }
}
