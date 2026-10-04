package com.example.uniportal.domain.rules

import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.UniversityApplication
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class ApplicationError { NOTE_TOO_LONG, PRIORITY_OUT_OF_RANGE }

sealed interface ApplicationValidation {
    data object Valid : ApplicationValidation
    data class Invalid(val errors: Set<ApplicationError>) : ApplicationValidation
}

object ApplicationRules {
    const val NOTE_MAX_LENGTH = 1000
    const val MIN_PRIORITY = 1
    const val MAX_PRIORITY = 3
    const val DEFAULT_PRIORITY = 2
    val PRIORITIES: IntRange = MIN_PRIORITY..MAX_PRIORITY

    fun daysLeft(deadline: LocalDate, today: LocalDate): Int =
        ChronoUnit.DAYS.between(today, deadline).toInt()

    fun daysLeft(application: UniversityApplication, today: LocalDate): Int? =
        application.deadline?.let { daysLeft(it, today) }

    fun isOverdue(application: UniversityApplication, today: LocalDate): Boolean {
        val deadline = application.deadline ?: return false
        return application.status.isActive && deadline.isBefore(today)
    }

    fun needsReminder(application: UniversityApplication, today: LocalDate, daysBefore: Int): Boolean {
        val deadline = application.deadline ?: return false
        if (!application.status.isActive || daysBefore < 0) return false
        return !deadline.isBefore(today) && !deadline.isAfter(today.plusDays(daysBefore.toLong()))
    }

    fun validate(note: String, priority: Int): ApplicationValidation {
        val errors = buildSet {
            if (note.length > NOTE_MAX_LENGTH) add(ApplicationError.NOTE_TOO_LONG)
            if (priority !in PRIORITIES) add(ApplicationError.PRIORITY_OUT_OF_RANGE)
        }
        return if (errors.isEmpty()) ApplicationValidation.Valid else ApplicationValidation.Invalid(errors)
    }

    fun dashboardComparator(today: LocalDate): Comparator<UniversityApplication> =
        compareByDescending<UniversityApplication> { isOverdue(it, today) }
            .thenBy(nullsLast<Long>()) { it.deadline?.toEpochDay() }
            .thenByDescending { it.priority }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.university.name }
            .thenBy { it.university.id }

    fun dashboardOrder(applications: List<UniversityApplication>, today: LocalDate): List<UniversityApplication> =
        applications.sortedWith(dashboardComparator(today))

    fun groupByStatus(
        applications: List<UniversityApplication>,
        today: LocalDate,
    ): Map<ApplicationStatus, List<UniversityApplication>> {
        val ordered = dashboardOrder(applications, today)
        return ApplicationStatus.entries
            .associateWith { status -> ordered.filter { it.status == status } }
            .filterValues { it.isNotEmpty() }
    }

    fun countByStatus(applications: List<UniversityApplication>): Map<ApplicationStatus, Int> =
        ApplicationStatus.entries.associateWith { status -> applications.count { it.status == status } }

    fun nearestDeadline(applications: List<UniversityApplication>, today: LocalDate): UniversityApplication? =
        applications
            .filter { app -> app.status.isActive && app.deadline?.let { !it.isBefore(today) } == true }
            .minWithOrNull(dashboardComparator(today))

    fun nextStep(status: ApplicationStatus): ApplicationStatus? = when (status) {
        ApplicationStatus.INTERESTED -> ApplicationStatus.PREPARING
        ApplicationStatus.PREPARING -> ApplicationStatus.SUBMITTED
        ApplicationStatus.SUBMITTED,
        ApplicationStatus.WAITLISTED,
        ApplicationStatus.ACCEPTED,
        ApplicationStatus.REJECTED,
        -> null
    }
}
