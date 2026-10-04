package com.example.uniportal.domain.rules

import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.UniversityApplication
import com.example.uniportal.testutil.university
import com.example.uniportal.testutil.universityApplication
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplicationRulesTest {
    private val today: LocalDate = LocalDate.of(2025, 3, 10)

    private fun application(
        name: String,
        status: ApplicationStatus = ApplicationStatus.INTERESTED,
        deadline: LocalDate? = null,
        priority: Int = ApplicationRules.DEFAULT_PRIORITY,
        id: String = name.lowercase().replace(' ', '-') + ".edu",
    ): UniversityApplication = universityApplication(
        university = university(id = id, name = name),
        status = status,
        deadline = deadline,
        priority = priority,
    )

    private fun inDays(days: Long): LocalDate = today.plusDays(days)

    @Test
    fun `given deadlines around today, when days left are counted, then today is 0, tomorrow 1 and a passed one negative`() {
        assertEquals(0, ApplicationRules.daysLeft(today, today))
        assertEquals(1, ApplicationRules.daysLeft(inDays(1), today))
        assertEquals(30, ApplicationRules.daysLeft(inDays(30), today))
        assertEquals(-1, ApplicationRules.daysLeft(inDays(-1), today))
        assertEquals(-45, ApplicationRules.daysLeft(inDays(-45), today))
    }

    @Test
    fun `given a deadline that passed yesterday, when overdue is checked for every status, then only not submitted ones are overdue`() {
        val overdueByStatus = ApplicationStatus.entries.associateWith { status ->
            ApplicationRules.isOverdue(application("Some University", status = status, deadline = inDays(-1)), today)
        }

        assertEquals(
            mapOf(
                ApplicationStatus.INTERESTED to true,
                ApplicationStatus.PREPARING to true,
                ApplicationStatus.SUBMITTED to false,
                ApplicationStatus.WAITLISTED to false,
                ApplicationStatus.ACCEPTED to false,
                ApplicationStatus.REJECTED to false,
            ),
            overdueByStatus,
        )
    }
}
