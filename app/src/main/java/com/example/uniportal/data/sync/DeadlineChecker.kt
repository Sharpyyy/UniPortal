package com.example.uniportal.data.sync

import com.example.uniportal.data.repository.ApplicationRepository
import com.example.uniportal.data.repository.SettingsRepository
import com.example.uniportal.domain.model.DueDeadline
import com.example.uniportal.domain.rules.ApplicationRules
import com.example.uniportal.util.TimeProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class DeadlineChecker @Inject constructor(
    private val applicationRepository: ApplicationRepository,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
) {
    suspend fun check(): List<DueDeadline> {
        val settings = settingsRepository.settings.first()
        if (!settings.deadlineReminders) return emptyList()
        val today = timeProvider.today()
        val due = applicationRepository.dueForReminder(today = today, daysBefore = settings.reminderDaysBefore)
        return ApplicationRules.dashboardOrder(due, today).mapNotNull { application ->
            application.deadline?.let { deadline ->
                DueDeadline(application = application, daysLeft = ApplicationRules.daysLeft(deadline, today))
            }
        }
    }
}
