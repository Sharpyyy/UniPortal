package com.example.uniportal.work

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.example.uniportal.MainActivity
import com.example.uniportal.R
import com.example.uniportal.data.local.UniPortalDatabase
import com.example.uniportal.data.repository.RoomApplicationRepository
import com.example.uniportal.data.sync.DeadlineChecker
import com.example.uniportal.domain.model.AppSettings
import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.University
import com.example.uniportal.testutil.FakeSettingsRepository
import com.example.uniportal.testutil.FakeTimeProvider
import com.example.uniportal.testutil.TestWorkerFactory
import com.example.uniportal.testutil.buildTestWorker
import com.example.uniportal.testutil.inMemoryDatabase
import com.example.uniportal.testutil.university
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class DeadlineReminderWorkerTest {
    private lateinit var context: Application
    private lateinit var database: UniPortalDatabase
    private lateinit var applications: RoomApplicationRepository
    private lateinit var factory: TestWorkerFactory

    private val timeProvider = FakeTimeProvider()
    private val today: LocalDate = FakeTimeProvider.START_DATE
    private val settings = FakeSettingsRepository(AppSettings(deadlineReminders = true, reminderDaysBefore = 3))

    private val mtsu = university(id = "mtsu.edu", name = "Middle Tennessee State University")
    private val kfu = university(
        id = "kpfu.ru",
        name = "Kazan Federal University",
        country = "Russian Federation",
        countryCode = "RU",
    )
    private val tokyo = university(id = "u-tokyo.ac.jp", name = "University of Tokyo", country = "Japan", countryCode = "JP")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = inMemoryDatabase()
        applications = RoomApplicationRepository(database.applicationDao(), timeProvider)
        val checker = DeadlineChecker(applications, settings, timeProvider)
        val notifier = DeadlineNotifier(context).also { it.createChannel() }
        factory = TestWorkerFactory(
            mapOf(
                DeadlineReminderWorker::class.java.name to { appContext: Context, params: WorkerParameters ->
                    DeadlineReminderWorker(appContext, params, checker, notifier)
                },
            ),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun grantNotificationPermission() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    private suspend fun saveApplication(
        university: University,
        deadline: LocalDate?,
        status: ApplicationStatus = ApplicationStatus.PREPARING,
    ) {
        applications.save(university, status, deadline = deadline, priority = 2, note = "")
    }

    private suspend fun runWorker(): ListenableWorker.Result =
        buildTestWorker(DeadlineReminderWorker::class.java, factory).doWork()

    private fun postedNotifications(): List<Notification> =
        shadowOf(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).allNotifications

    private fun Notification.title(): String? = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()

    private fun Notification.text(): String? = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

    @Test
    fun `given one application due tomorrow, when the worker runs, then one notification names it and opens the applications`() =
        runTest {
            grantNotificationPermission()
            saveApplication(mtsu, deadline = today.plusDays(1))
            saveApplication(kfu, deadline = today.plusDays(10))
            saveApplication(tokyo, deadline = today.plusDays(1), status = ApplicationStatus.SUBMITTED)

            val result = runWorker()

            assertEquals(ListenableWorker.Result.success(), result)
            val notification = postedNotifications().single()
            assertEquals(mtsu.name, notification.title())
            assertEquals(
                context.getString(R.string.notification_deadline_single, context.getString(R.string.notification_when_tomorrow)),
                notification.text(),
            )
            assertEquals(DeadlineNotifier.CHANNEL_ID, notification.channelId)
            val tapIntent = shadowOf(notification.contentIntent).savedIntent
            assertEquals(MainActivity::class.java.name, tapIntent.component?.className)
            assertTrue(tapIntent.getBooleanExtra(DeadlineNotifier.EXTRA_OPEN_APPLICATIONS, false))
        }
}
