package com.example.uniportal.domain.model

enum class ThemeMode { PORTAL, LIGHT, SYSTEM }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.PORTAL,
    val homeCountry: CountryFilter = CountryFilter.ALL,
    val cacheTtl: CacheTtl = CacheTtl.H24,
    val backgroundSync: Boolean = true,
    val syncWifiOnly: Boolean = false,
    val deadlineReminders: Boolean = true,
    val reminderDaysBefore: Int = DEFAULT_REMINDER_DAYS,
    val historyLimit: Int = DEFAULT_HISTORY_LIMIT,
    val lastSyncAt: Long? = null,
) {
    companion object {
        const val DEFAULT_REMINDER_DAYS = 3
        const val DEFAULT_HISTORY_LIMIT = 50
        val REMINDER_DAYS_OPTIONS: List<Int> = listOf(1, 3, 7)
        val HISTORY_LIMIT_OPTIONS: List<Int> = listOf(20, 50, 100)
    }
}
