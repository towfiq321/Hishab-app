package com.hishab.finance

import android.app.Application
import com.hishab.finance.notification.Notifications
import com.hishab.finance.notification.ReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HishabApp : Application() {

    lateinit var container: AppContainer
        private set

    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)

        startupScope.launch {
            // Default categories and sources always exist.
            container.seeder.seedCatalogueIfEmpty()

            // Demo history on the very first launch only, so the app is never an empty shell.
            val settings = container.settings.settings.first()
            if (!settings.demoSeeded && container.seeder.isEmpty()) {
                runCatching { container.seeder.seedDemoData() }
                container.settings.setDemoSeeded(true)
            }

            // Catch up any recurring expense that fell due while the app was closed.
            runCatching { container.recurring.postDue() }

            if (settings.dailyReminder || settings.billReminders || settings.budgetAlerts || settings.monthlyReport) {
                ReminderWorker.schedule(this@HishabApp, settings.reminderHour)
            }
        }
    }
}
