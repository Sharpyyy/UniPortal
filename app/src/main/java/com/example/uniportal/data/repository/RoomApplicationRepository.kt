package com.example.uniportal.data.repository

import com.example.uniportal.data.local.dao.ApplicationDao
import com.example.uniportal.data.local.entity.ApplicationEntity
import com.example.uniportal.data.mapper.toDomain
import com.example.uniportal.data.mapper.toEntity
import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.University
import com.example.uniportal.domain.model.UniversityApplication
import com.example.uniportal.domain.rules.ApplicationRules
import com.example.uniportal.domain.rules.ApplicationValidation
import com.example.uniportal.util.TimeProvider
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class RoomApplicationRepository @Inject constructor(
    private val dao: ApplicationDao,
    private val timeProvider: TimeProvider,
) : ApplicationRepository {
    override fun observeApplications(): Flow<List<UniversityApplication>> =
        dao.observeAll()
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()

    override fun observeApplication(universityId: String): Flow<UniversityApplication?> =
        dao.observe(universityId)
            .map { row -> row?.toDomain() }
            .distinctUntilChanged()

    override fun observeStatuses(): Flow<Map<String, ApplicationStatus>> =
        dao.observeStatuses()
            .map { rows -> rows.associate { it.universityId to ApplicationStatus.fromName(it.status) } }
            .distinctUntilChanged()

    override suspend fun save(
        university: University,
        status: ApplicationStatus,
        deadline: LocalDate?,
        priority: Int,
        note: String,
    ): ApplicationValidation {
        val cleanNote = note.trim()
        val validation = ApplicationRules.validate(note = cleanNote, priority = priority)
        if (validation is ApplicationValidation.Invalid) return validation

        val now = timeProvider.now()
        dao.save(
            university = university.toEntity(fetchedAt = now),
            application = ApplicationEntity(
                universityId = university.id,
                status = status.name,
                deadlineEpochDay = deadline?.toEpochDay(),
                priority = priority,
                note = cleanNote,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return validation
    }

    override suspend fun updateStatus(universityId: String, status: ApplicationStatus) {
        dao.updateStatus(universityId = universityId, status = status.name, updatedAt = timeProvider.now())
    }

    override suspend fun delete(universityId: String) {
        dao.delete(universityId)
    }

    override suspend fun dueForReminder(today: LocalDate, daysBefore: Int): List<UniversityApplication> {
        if (daysBefore < 0) return emptyList()
        return dao.getWithDeadlineBetween(
            fromEpochDay = today.toEpochDay(),
            toEpochDay = today.plusDays(daysBefore.toLong()).toEpochDay(),
        )
            .map { it.toDomain() }
            .filter { ApplicationRules.needsReminder(it, today, daysBefore) }
    }
}
