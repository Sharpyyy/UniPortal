package com.example.uniportal.data.repository

import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.University
import com.example.uniportal.domain.model.UniversityApplication
import com.example.uniportal.domain.rules.ApplicationValidation
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface ApplicationRepository {
    fun observeApplications(): Flow<List<UniversityApplication>>

    fun observeApplication(universityId: String): Flow<UniversityApplication?>

    fun observeStatuses(): Flow<Map<String, ApplicationStatus>>

    suspend fun save(
        university: University,
        status: ApplicationStatus,
        deadline: LocalDate?,
        priority: Int,
        note: String,
    ): ApplicationValidation

    suspend fun updateStatus(universityId: String, status: ApplicationStatus)

    suspend fun delete(universityId: String)

    suspend fun dueForReminder(today: LocalDate, daysBefore: Int): List<UniversityApplication>
}
