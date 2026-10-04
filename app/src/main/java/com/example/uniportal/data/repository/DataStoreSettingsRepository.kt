package com.example.uniportal.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.uniportal.domain.model.AppSettings
import com.example.uniportal.domain.model.CacheTtl
import com.example.uniportal.domain.model.CountryFilter
import com.example.uniportal.domain.model.ThemeMode
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->

            if (error is IOException) {
                Log.w(TAG, "Failed to read settings", error)
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences -> preferences.toAppSettings() }
        .distinctUntilChanged()

    override suspend fun setTheme(theme: ThemeMode) = write { it[SettingsKeys.THEME] = theme.name }

    override suspend fun setHomeCountry(country: CountryFilter) = write { it[SettingsKeys.HOME_COUNTRY] = country.name }

    override suspend fun setCacheTtl(ttl: CacheTtl) = write { it[SettingsKeys.CACHE_TTL] = ttl.name }

    override suspend fun setBackgroundSync(enabled: Boolean) = write { it[SettingsKeys.BACKGROUND_SYNC] = enabled }

    override suspend fun setSyncWifiOnly(enabled: Boolean) = write { it[SettingsKeys.SYNC_WIFI_ONLY] = enabled }

    override suspend fun setDeadlineReminders(enabled: Boolean) =
        write { it[SettingsKeys.DEADLINE_REMINDERS] = enabled }

    override suspend fun setReminderDaysBefore(days: Int) =
        write { it[SettingsKeys.REMINDER_DAYS_BEFORE] = days.coerceAtLeast(1) }

    override suspend fun setHistoryLimit(limit: Int) =
        write { it[SettingsKeys.HISTORY_LIMIT] = limit.coerceAtLeast(1) }

    override suspend fun setLastSyncAt(timestamp: Long) = write { it[SettingsKeys.LAST_SYNC_AT] = timestamp }

    private suspend fun write(change: (MutablePreferences) -> Unit) {
        dataStore.edit { preferences -> change(preferences) }
    }

    private fun Preferences.toAppSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            theme = enumOrNull<ThemeMode>(this[SettingsKeys.THEME]) ?: defaults.theme,
            homeCountry = enumOrNull<CountryFilter>(this[SettingsKeys.HOME_COUNTRY]) ?: defaults.homeCountry,
            cacheTtl = enumOrNull<CacheTtl>(this[SettingsKeys.CACHE_TTL]) ?: defaults.cacheTtl,
            backgroundSync = this[SettingsKeys.BACKGROUND_SYNC] ?: defaults.backgroundSync,
            syncWifiOnly = this[SettingsKeys.SYNC_WIFI_ONLY] ?: defaults.syncWifiOnly,
            deadlineReminders = this[SettingsKeys.DEADLINE_REMINDERS] ?: defaults.deadlineReminders,
            reminderDaysBefore = this[SettingsKeys.REMINDER_DAYS_BEFORE]?.takeIf { it > 0 }
                ?: defaults.reminderDaysBefore,
            historyLimit = this[SettingsKeys.HISTORY_LIMIT]?.takeIf { it > 0 } ?: defaults.historyLimit,
            lastSyncAt = this[SettingsKeys.LAST_SYNC_AT],
        )
    }

    private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
        name?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } }

    private companion object {
        const val TAG = "SettingsRepository"
    }
}

object SettingsKeys {
    val THEME = stringPreferencesKey("theme")
    val HOME_COUNTRY = stringPreferencesKey("home_country")
    val CACHE_TTL = stringPreferencesKey("cache_ttl")
    val BACKGROUND_SYNC = booleanPreferencesKey("background_sync")
    val SYNC_WIFI_ONLY = booleanPreferencesKey("sync_wifi_only")
    val DEADLINE_REMINDERS = booleanPreferencesKey("deadline_reminders")
    val REMINDER_DAYS_BEFORE = intPreferencesKey("reminder_days_before")
    val HISTORY_LIMIT = intPreferencesKey("history_limit")
    val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
}
