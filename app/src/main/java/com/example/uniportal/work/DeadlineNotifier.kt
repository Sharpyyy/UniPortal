package com.example.uniportal.work

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.uniportal.MainActivity
import com.example.uniportal.R
import com.example.uniportal.domain.model.DueDeadline
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeadlineNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_deadlines),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_deadlines_description)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun notifyDeadlines(deadlines: List<DueDeadline>): Boolean {
        if (deadlines.isEmpty()) return false
        if (!canPostNotifications()) {
            Log.i(TAG, "Deadline reminder skipped: notifications are not allowed")
            return false
        }
        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, buildNotification(deadlines))
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "Failed to post the deadline reminder", e)
            false
        }
    }

    private fun buildNotification(deadlines: List<DueDeadline>): Notification {
        val nearest = deadlines.first()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openApplicationsIntent())
            .setAutoCancel(true)

        if (deadlines.size == 1) {
            builder
                .setContentTitle(nearest.application.university.name)
                .setContentText(context.getString(R.string.notification_deadline_single, whenText(nearest.daysLeft)))
        } else {
            val title = context.resources.getQuantityString(
                R.plurals.notification_deadlines_title,
                deadlines.size,
                deadlines.size,
            )
            val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
            deadlines.take(MAX_LINES).forEach { deadline -> style.addLine(line(deadline)) }
            val hidden = deadlines.size - MAX_LINES
            if (hidden > 0) {
                style.setSummaryText(context.getString(R.string.notification_deadlines_more, hidden))
            }
            builder
                .setContentTitle(title)
                .setContentText(line(nearest))
                .setStyle(style)
                .setNumber(deadlines.size)
        }
        return builder.build()
    }

    private fun line(deadline: DueDeadline): String =
        context.getString(
            R.string.notification_deadline_line,
            deadline.application.university.name,
            whenText(deadline.daysLeft),
        )

    private fun whenText(daysLeft: Int): String = when {
        daysLeft <= 0 -> context.getString(R.string.notification_when_today)
        daysLeft == 1 -> context.getString(R.string.notification_when_tomorrow)
        else -> context.resources.getQuantityString(R.plurals.notification_when_in_days, daysLeft, daysLeft)
    }

    private fun openApplicationsIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_APPLICATIONS, true)
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_APP,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        const val CHANNEL_ID = "deadlines"
        const val NOTIFICATION_ID = 1001

        const val EXTRA_OPEN_APPLICATIONS = "com.example.uniportal.extra.OPEN_APPLICATIONS"

        private const val REQUEST_CODE_OPEN_APP = 0
        private const val MAX_LINES = 5
        private const val TAG = "DeadlineNotifier"
    }
}
