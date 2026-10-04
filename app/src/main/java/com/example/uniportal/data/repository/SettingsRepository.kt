package com.example.uniportal.data.repository

import com.example.uniportal.domain.model.AppSettings
import com.example.uniportal.domain.model.CacheTtl
import com.example.uniportal.domain.model.CountryFilter
import com.example.uniportal.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setTheme(theme: ThemeMode)

    suspend fun setHomeCountry(country: CountryFilter)

    suspend fun setCacheTtl(ttl: CacheTtl)

    suspend fun setBackgroundSync(enabled: Boolean)

    suspend fun setSyncWifiOnly(enabled: Boolean)

    suspend fun setDeadlineReminders(enabled: Boolean)

    suspend fun setReminderDaysBefore(days: Int)

    suspend fun setHistoryLimit(limit: Int)

    suspend fun setLastSyncAt(timestamp: Long)
}
