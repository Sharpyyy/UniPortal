package com.example.uniportal

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.uniportal.di.ApplicationScope
import com.example.uniportal.work.DeadlineNotifier
import com.example.uniportal.work.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

@HiltAndroidApp
class UniPortalApp : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var workScheduler: WorkScheduler

    @Inject
    lateinit var deadlineNotifier: DeadlineNotifier

    @ApplicationScope
    @Inject
    lateinit var applicationScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        deadlineNotifier.createChannel()
        workScheduler.start(applicationScope)
    }
}
