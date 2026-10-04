package com.example.uniportal.work

import android.app.Application
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.example.uniportal.data.sync.SyncReport
import com.example.uniportal.testutil.FakeUniversitySyncer
import com.example.uniportal.testutil.TestWorkerFactory
import com.example.uniportal.testutil.buildTestWorker
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class SyncWorkerTest {
    private val syncer = FakeUniversitySyncer()
    private val factory = TestWorkerFactory(
        mapOf(
            SyncWorker::class.java.name to { context: Context, params: WorkerParameters -> SyncWorker(context, params, syncer) },
        ),
    )

    private fun buildWorker(runAttemptCount: Int = 0): SyncWorker =
        buildTestWorker(SyncWorker::class.java, factory, runAttemptCount = runAttemptCount)

    @Test
    fun `given no network, when the worker runs on each attempt, then it retries below the attempt limit and fails at it`() =
        runTest {
            syncer.handler = { throw IOException("No route to host") }

            val results = (0..SyncWorker.MAX_ATTEMPTS).map { attempt -> buildWorker(runAttemptCount = attempt).doWork() }

            assertEquals(
                List(SyncWorker.MAX_ATTEMPTS) { ListenableWorker.Result.retry() } + ListenableWorker.Result.failure(),
                results,
            )
            assertEquals("every attempt really syncs", SyncWorker.MAX_ATTEMPTS + 1, syncer.syncCalls)
        }
}
