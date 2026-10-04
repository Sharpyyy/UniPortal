package com.example.uniportal.testutil

import com.example.uniportal.data.repository.SettingsRepository
import com.example.uniportal.domain.model.AppSettings
import com.example.uniportal.domain.model.CacheTtl
import com.example.uniportal.domain.model.CountryFilter
import com.example.uniportal.domain.model.ThemeMode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

class FakeSettingsRepository(
    initial: AppSettings = AppSettings(),
) : SettingsRepository {
    data class SettingsWrite(val field: String, val value: Any?)

    val current = MutableStateFlow(initial)

    val writes = mutableListOf<SettingsWrite>()

    var readCalls = 0
        private set

    var readGate: CompletableDeferred<Unit>? = null
    var readError: Throwable? = null
    var writeError: Throwable? = null

    override val settings: Flow<AppSettings> = flow {
        readCalls++
        val currentGate = readGate
        currentGate?.await()
        readError?.let { throw it }
        emitAll(current)
    }

    override suspend fun setTheme(theme: ThemeMode) = write("theme", theme) { copy(theme = theme) }

    override suspend fun setHomeCountry(country: CountryFilter) =
        write("homeCountry", country) { copy(homeCountry = country) }

    override suspend fun setCacheTtl(ttl: CacheTtl) = write("cacheTtl", ttl) { copy(cacheTtl = ttl) }

    override suspend fun setBackgroundSync(enabled: Boolean) =
        write("backgroundSync", enabled) { copy(backgroundSync = enabled) }

    override suspend fun setSyncWifiOnly(enabled: Boolean) =
        write("syncWifiOnly", enabled) { copy(syncWifiOnly = enabled) }

    override suspend fun setDeadlineReminders(enabled: Boolean) =
        write("deadlineReminders", enabled) { copy(deadlineReminders = enabled) }

    override suspend fun setReminderDaysBefore(days: Int) =
        write("reminderDaysBefore", days) { copy(reminderDaysBefore = days.coerceAtLeast(1)) }

    override suspend fun setHistoryLimit(limit: Int) =
        write("historyLimit", limit) { copy(historyLimit = limit.coerceAtLeast(1)) }

    override suspend fun setLastSyncAt(timestamp: Long) =
        write("lastSyncAt", timestamp) { copy(lastSyncAt = timestamp) }

    fun update(change: (AppSettings) -> AppSettings) {
        current.update(change)
    }

    fun writesOf(field: String): List<Any?> = writes.filter { it.field == field }.map { it.value }

    private fun write(field: String, value: Any?, change: AppSettings.() -> AppSettings) {
        writes += SettingsWrite(field = field, value = value)
        writeError?.let { throw it }
        current.update { settings -> settings.change() }
    }
}
