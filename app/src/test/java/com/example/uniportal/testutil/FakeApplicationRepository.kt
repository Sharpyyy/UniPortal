package com.example.uniportal.testutil

import com.example.uniportal.data.repository.ApplicationRepository
import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.University
import com.example.uniportal.domain.model.UniversityApplication
import com.example.uniportal.domain.rules.ApplicationRules
import com.example.uniportal.domain.rules.ApplicationValidation
import com.example.uniportal.util.TimeProvider
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeApplicationRepository(
    initial: List<UniversityApplication> = emptyList(),
    private val timeProvider: TimeProvider = FakeTimeProvider(),
) : ApplicationRepository {
    data class SaveCall(
        val university: University,
        val status: ApplicationStatus,
        val deadline: LocalDate?,
        val priority: Int,
        val note: String,
    )

    data class StatusCall(val universityId: String, val status: ApplicationStatus)
    data class ReminderCall(val today: LocalDate, val daysBefore: Int)

    private val stored = MutableStateFlow(normalized(initial))

    val applications: List<UniversityApplication>
        get() = stored.value

    val activeObservers: Int
        get() = stored.subscriptionCount.value

    var observeCalls = 0
        private set
    val saveCalls = mutableListOf<SaveCall>()
    val updateStatusCalls = mutableListOf<StatusCall>()
    val deleteCalls = mutableListOf<String>()
    val dueForReminderCalls = mutableListOf<ReminderCall>()

    var readError: Throwable? = null
    var writeError: Throwable? = null
    var writeGate: CompletableDeferred<Unit>? = null

    fun application(universityId: String): UniversityApplication? =
        stored.value.firstOrNull { it.university.id == universityId }

    fun replaceAll(applications: List<UniversityApplication>) {
        stored.value = normalized(applications)
    }

    override fun observeApplications(): Flow<List<UniversityApplication>> = observe { list -> list }

    override fun observeApplication(universityId: String): Flow<UniversityApplication?> =
        observe { list -> list.firstOrNull { it.university.id == universityId } }

    override fun observeStatuses(): Flow<Map<String, ApplicationStatus>> =
        observe { list -> list.associate { it.university.id to it.status } }

    override suspend fun save(
        university: University,
        status: ApplicationStatus,
        deadline: LocalDate?,
        priority: Int,
        note: String,
    ): ApplicationValidation {
        saveCalls += SaveCall(university, status, deadline, priority, note)
        beforeWrite()
        val cleanNote = note.trim()
        val validation = ApplicationRules.validate(note = cleanNote, priority = priority)
        if (validation is ApplicationValidation.Invalid) return validation

        val now = timeProvider.now()
        stored.update { list ->
            val existing = list.firstOrNull { it.university.id == university.id }
            val saved = UniversityApplication(
                university = university,
                status = status,
                deadline = deadline,
                priority = priority,
                note = cleanNote,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
            put(list, saved)
        }
        return validation
    }

    override suspend fun updateStatus(universityId: String, status: ApplicationStatus) {
        updateStatusCalls += StatusCall(universityId = universityId, status = status)
        beforeWrite()
        val now = timeProvider.now()
        stored.update { list ->
            val existing = list.firstOrNull { it.university.id == universityId }
            if (existing == null) list else put(list, existing.copy(status = status, updatedAt = now))
        }
    }

    override suspend fun delete(universityId: String) {
        deleteCalls += universityId
        beforeWrite()
        stored.update { list -> list.filterNot { it.university.id == universityId } }
    }

    override suspend fun dueForReminder(today: LocalDate, daysBefore: Int): List<UniversityApplication> {
        dueForReminderCalls += ReminderCall(today = today, daysBefore = daysBefore)
        readError?.let { throw it }
        if (daysBefore < 0) return emptyList()
        return stored.value.filter { ApplicationRules.needsReminder(it, today, daysBefore) }
    }

    private suspend fun beforeWrite() {
        val currentGate = writeGate
        currentGate?.await()
        writeError?.let { throw it }
    }

    private fun <T> observe(transform: (List<UniversityApplication>) -> T): Flow<T> = flow {
        observeCalls++
        readError?.let { throw it }
        emitAll(stored.map { list -> transform(list) })
    }.distinctUntilChanged()

    private fun put(list: List<UniversityApplication>, application: UniversityApplication): List<UniversityApplication> =
        normalized(listOf(application) + list.filterNot { it.university.id == application.university.id })

    private fun normalized(list: List<UniversityApplication>): List<UniversityApplication> =
        list.sortedByDescending { it.updatedAt }.distinctBy { it.university.id }
}
