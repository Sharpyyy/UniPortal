package com.example.uniportal.work

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.uniportal.domain.model.CacheTtl
import com.example.uniportal.domain.model.CountryFilter
import com.example.uniportal.domain.model.ThemeMode
import com.example.uniportal.testutil.FakeSettingsRepository
import com.example.uniportal.testutil.FakeTimeProvider
import com.example.uniportal.work.WorkManagerScheduler.Companion.COUNTRY_TAG_PREFIX
import com.example.uniportal.work.WorkManagerScheduler.Companion.DEADLINE_REMINDERS_WORK
import com.example.uniportal.work.WorkManagerScheduler.Companion.PERIODIC_SYNC_WORK
import com.example.uniportal.work.WorkManagerScheduler.Companion.SYNC_NOW_WORK
import com.example.uniportal.work.WorkManagerScheduler.Companion.TAG_COUNTRY_DOWNLOAD
import com.example.uniportal.work.WorkManagerScheduler.Companion.TAG_SYNC
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Provider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class WorkSchedulerTest {
    private lateinit var workManager: WorkManager
    private lateinit var scheduler: WorkManagerScheduler
    private lateinit var systemTimeZone: TimeZone

    private val settings = FakeSettingsRepository()
    private val timeProvider = FakeTimeProvider()

    @Before
    fun setUp() {
        systemTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .setExecutor(SynchronousExecutor())
                .build(),
        )
        workManager = WorkManager.getInstance(context)
        scheduler = WorkManagerScheduler(Provider { workManager }, settings, timeProvider)
    }

    @After
    fun tearDown() {
        WorkManagerTestInitHelper.closeWorkDatabase()
        TimeZone.setDefault(systemTimeZone)
    }

    private fun uniqueWork(name: String): List<WorkInfo> = workManager.getWorkInfosForUniqueWork(name).get()

    private fun pendingWork(name: String): WorkInfo = uniqueWork(name).filterNot { it.state.isFinished }.single()

    private fun downloads(): List<WorkInfo> = workManager.getWorkInfosByTag(TAG_COUNTRY_DOWNLOAD).get()

    @Test
    fun `given default settings, when the scheduler starts, then the periodic sync is enqueued every 12 hours on any network`() =
        runTest {
            scheduler.start(backgroundScope)
            runCurrent()

            val sync = uniqueWork(PERIODIC_SYNC_WORK).single()
            assertEquals(WorkInfo.State.ENQUEUED, sync.state)
            assertEquals(NetworkType.CONNECTED, sync.constraints.requiredNetworkType)
            assertEquals(
                TimeUnit.HOURS.toMillis(WorkManagerScheduler.SYNC_INTERVAL_HOURS),
                sync.periodicityInfo?.repeatIntervalMillis,
            )
            assertTrue(TAG_SYNC in sync.tags)
        }
}
