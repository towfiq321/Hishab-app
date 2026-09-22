package com.hishab.finance.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dailyReminder: Boolean = false,
    val reminderHour: Int = 21,
    val budgetAlerts: Boolean = true,
    val billReminders: Boolean = true,
    val monthlyReport: Boolean = true,
    val backupReminder: Boolean = false,
    val demoSeeded: Boolean = false
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("hishab_settings")

/** Thin wrapper over DataStore so screens never touch preference keys directly. */
class SettingsStore(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val DAILY_REMINDER = booleanPreferencesKey("daily_reminder")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val BUDGET_ALERTS = booleanPreferencesKey("budget_alerts")
        val BILL_REMINDERS = booleanPreferencesKey("bill_reminders")
        val MONTHLY_REPORT = booleanPreferencesKey("monthly_report")
        val BACKUP_REMINDER = booleanPreferencesKey("backup_reminder")
        val DEMO_SEEDED = booleanPreferencesKey("demo_seeded")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(prefs[Keys.THEME] ?: ThemeMode.SYSTEM.name) }
                .getOrDefault(ThemeMode.SYSTEM),
            dailyReminder = prefs[Keys.DAILY_REMINDER] ?: false,
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: 21,
            budgetAlerts = prefs[Keys.BUDGET_ALERTS] ?: true,
            billReminders = prefs[Keys.BILL_REMINDERS] ?: true,
            monthlyReport = prefs[Keys.MONTHLY_REPORT] ?: true,
            backupReminder = prefs[Keys.BACKUP_REMINDER] ?: false,
            demoSeeded = prefs[Keys.DEMO_SEEDED] ?: false
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME] = mode.name }
    suspend fun setDailyReminder(enabled: Boolean) = edit { it[Keys.DAILY_REMINDER] = enabled }
    suspend fun setReminderHour(hour: Int) = edit { it[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23) }
    suspend fun setBudgetAlerts(enabled: Boolean) = edit { it[Keys.BUDGET_ALERTS] = enabled }
    suspend fun setBillReminders(enabled: Boolean) = edit { it[Keys.BILL_REMINDERS] = enabled }
    suspend fun setMonthlyReport(enabled: Boolean) = edit { it[Keys.MONTHLY_REPORT] = enabled }
    suspend fun setBackupReminder(enabled: Boolean) = edit { it[Keys.BACKUP_REMINDER] = enabled }
    suspend fun setDemoSeeded(seeded: Boolean) = edit { it[Keys.DEMO_SEEDED] = seeded }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
