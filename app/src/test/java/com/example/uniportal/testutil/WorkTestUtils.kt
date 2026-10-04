package com.example.uniportal.testutil

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.ProgressUpdater
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.util.concurrent.ListenableFuture
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class TestWorkerFactory(
    private val creators: Map<String, (Context, WorkerParameters) -> ListenableWorker>,
) : WorkerFactory() {
    val createdWorkers = mutableListOf<String>()
    val createdParameters = mutableListOf<WorkerParameters>()

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? {
        val creator = creators[workerClassName] ?: return null
        createdWorkers += workerClassName
        createdParameters += workerParameters
        return creator(appContext, workerParameters)
    }
}

class RecordingProgressUpdater : ProgressUpdater {
    val updates = mutableListOf<Data>()

    override fun updateProgress(context: Context, id: UUID, data: Data): ListenableFuture<Void> {
        updates += data
        return CompletedVoidFuture
    }
}

private object CompletedVoidFuture : ListenableFuture<Void> {
    override fun addListener(listener: Runnable, executor: Executor) {
        executor.execute(listener)
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false

    override fun isCancelled(): Boolean = false

    override fun isDone(): Boolean = true

    override fun get(): Void? = null

    override fun get(timeout: Long, unit: TimeUnit): Void? = null
}

fun <W : ListenableWorker> buildTestWorker(
    workerClass: Class<W>,
    factory: WorkerFactory,
    inputData: Data = Data.EMPTY,
    runAttemptCount: Int = 0,
    progressUpdater: ProgressUpdater = RecordingProgressUpdater(),
): W =
    TestListenableWorkerBuilder.from(ApplicationProvider.getApplicationContext<Context>(), workerClass)
        .setWorkerFactory(factory)
        .setInputData(inputData)
        .setRunAttemptCount(runAttemptCount)
        .setProgressUpdater(progressUpdater)
        .build()
