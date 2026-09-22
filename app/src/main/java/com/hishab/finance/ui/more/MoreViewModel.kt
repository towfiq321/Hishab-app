package com.hishab.finance.ui.more

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hishab.finance.AppContainer
import com.hishab.finance.core.startOfMonth
import com.hishab.finance.data.repository.AppSettings
import com.hishab.finance.data.repository.ThemeMode
import com.hishab.finance.domain.ReportEngine
import com.hishab.finance.notification.ReminderWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Work in progress banners: exports and restores can take a moment on a big history. */
data class MoreUiState(
    val busy: Boolean = false,
    val message: String? = null,
    val counts: DataCounts = DataCounts()
)

data class DataCounts(
    val transactions: Int = 0,
    val categories: Int = 0,
    val sources: Int = 0,
    val rules: Int = 0
)

class MoreViewModel(
    private val container: AppContainer,
    private val appContext: Context
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        container.settings.settings
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _ui = MutableStateFlow(MoreUiState())
    val ui: StateFlow<MoreUiState> = _ui.asStateFlow()

    init { refreshCounts() }

    private fun refreshCounts() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                counts = DataCounts(
                    transactions = container.transactions.count(),
                    categories = container.catalog.categories().size,
                    sources = container.catalog.sources().size,
                    rules = container.recurring.rules().size
                )
            )
        }
    }

    /* ----------------------------------------------------------------------------- appearance */

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { container.settings.setThemeMode(mode) }
    }

    /* -------------------------------------------------------------------------- notifications */

    fun setDailyReminder(enabled: Boolean) = updateReminders { container.settings.setDailyReminder(enabled) }
    fun setBillReminders(enabled: Boolean) = updateReminders { container.settings.setBillReminders(enabled) }
    fun setBudgetAlerts(enabled: Boolean) = updateReminders { container.settings.setBudgetAlerts(enabled) }
    fun setMonthlyReport(enabled: Boolean) = updateReminders { container.settings.setMonthlyReport(enabled) }
    fun setBackupReminder(enabled: Boolean) = updateReminders { container.settings.setBackupReminder(enabled) }
    fun setReminderHour(hour: Int) = updateReminders { container.settings.setReminderHour(hour) }

    /**
     * Any notification change re-plans the single daily worker, or cancels it when nothing is
     * switched on, so WorkManager never runs for no reason.
     */
    private fun updateReminders(change: suspend () -> Unit) {
        viewModelScope.launch {
            change()
            val current = container.settings.settings.first()
            val anyOn = current.dailyReminder || current.billReminders || current.budgetAlerts ||
                current.monthlyReport || current.backupReminder
            if (anyOn) ReminderWorker.schedule(appContext, current.reminderHour)
            else ReminderWorker.cancel(appContext)
        }
    }

    /* --------------------------------------------------------------------------------- export */

    fun exportCsv(from: LocalDate, to: LocalDate) {
        run(busyMessage = null) {
            val items = container.transactions.observeItemsBetween(from, to).first()
            if (items.isEmpty()) return@run "Nothing to export in that date range."
            val file = container.exporter.exportCsv(items, from, to)
            container.exporter.share(file, container.exporter.mimeFor(file))
            "Exported ${items.size} transactions to ${file.name}"
        }
    }

    fun exportPdf(from: LocalDate, to: LocalDate) {
        run(busyMessage = null) {
            val items = container.transactions.observeItemsBetween(from, to).first()
            if (items.isEmpty()) return@run "Nothing to export in that date range."
            val summary = ReportEngine.summarise(items, from, to)
            val file = container.exporter.exportPdf(summary, items, "Financial report")
            container.exporter.share(file, container.exporter.mimeFor(file))
            "Report ready: ${file.name}"
        }
    }

    /* --------------------------------------------------------------------------------- backup */

    fun createBackup() {
        run {
            val file = container.backup.createBackup()
            container.exporter.share(file, "application/json")
            "Backup created: ${file.name}"
        }
    }

    fun restore(uri: Uri) {
        run {
            val result = container.backup.restore(uri)
            refreshCounts()
            "Restored ${result.transactions} transactions, ${result.categories} categories, " +
                "${result.sources} sources, ${result.rules} recurring rules and ${result.budgets} budgets."
        }
    }

    /* ------------------------------------------------------------------------------ demo data */

    fun loadDemoData() {
        run {
            container.seeder.seedCatalogueIfEmpty()
            container.seeder.seedDemoData()
            container.settings.setDemoSeeded(true)
            refreshCounts()
            "Demo history added. Explore the reports and forecast."
        }
    }

    fun clearAllData() {
        run {
            container.seeder.clearAllData()
            container.seeder.seedCatalogueIfEmpty()
            container.settings.setDemoSeeded(true)
            refreshCounts()
            "All transactions removed. Default categories and sources kept."
        }
    }

    fun clearMessage() { _ui.value = _ui.value.copy(message = null) }

    /** Runs a long job with a busy flag and turns its result (or failure) into one message. */
    private fun run(busyMessage: String? = null, block: suspend () -> String) {
        if (_ui.value.busy) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true, message = busyMessage)
            val outcome = runCatching { block() }
            _ui.value = _ui.value.copy(
                busy = false,
                message = outcome.getOrElse { "That did not work: ${it.message ?: "unknown error"}" }
            )
        }
    }

    /** Default export window: the start of this month to today. */
    fun defaultRange(): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return today.startOfMonth() to today
    }
}
