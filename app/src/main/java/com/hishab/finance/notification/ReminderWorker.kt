package com.hishab.finance.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hishab.finance.HishabApp
import com.hishab.finance.core.toTaka
import com.hishab.finance.data.local.entity.TxType
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * One daily pass that handles every reminder the user has switched on:
 * "did you record today's expenses", bills falling due, and budget lines running hot.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as? HishabApp)?.container ?: return Result.success()
        val settings = container.settings.settings.first()
        val today = LocalDate.now()

        if (settings.dailyReminder) {
            val spentToday = container.transactions.totalBetween(TxType.EXPENSE, today, today)
            val text = if (spentToday <= 0.0) {
                "No expenses recorded today. Add them while they are still fresh."
            } else {
                "You have recorded ${spentToday.toTaka()} today. Anything missing?"
            }
            Notifications.post(applicationContext, 1001, Notifications.CHANNEL_REMINDERS, "Today's spending", text)
        }

        if (settings.billReminders) {
            container.recurring.upcoming(days = 3, today = today).forEach { rule ->
                val due = LocalDate.ofEpochDay(rule.nextDueEpochDay)
                val whenText = when (due) {
                    today -> "due today"
                    today.plusDays(1) -> "due tomorrow"
                    else -> "due on ${due.dayOfMonth} ${due.month.name.lowercase().replaceFirstChar { it.uppercase() }}"
                }
                Notifications.post(
                    applicationContext,
                    2000 + rule.id.toInt(),
                    Notifications.CHANNEL_REMINDERS,
                    "${rule.title} $whenText",
                    "${rule.amount.toTaka()} \u2022 ${rule.recurrence.label}"
                )
            }
        }

        if (settings.budgetAlerts) {
            container.budgets.breaches(today).forEach { progress ->
                val over = progress.spent > progress.budget
                val title = if (over) "${progress.category.name} budget passed" else "${progress.category.name} budget is nearly used"
                val text = if (over) {
                    "${progress.spent.toTaka()} spent of ${progress.budget.toTaka()} \u2014 ${(-progress.remaining).toTaka()} over."
                } else {
                    "${progress.spent.toTaka()} of ${progress.budget.toTaka()} used. ${progress.remaining.toTaka()} left this month."
                }
                Notifications.post(
                    applicationContext,
                    3000 + progress.category.id.toInt(),
                    Notifications.CHANNEL_ALERTS,
                    title,
                    text
                )
            }
        }

        if (settings.monthlyReport && today.dayOfMonth == 1) {
            val previous = today.minusMonths(1)
            val from = previous.withDayOfMonth(1)
            val to = previous.withDayOfMonth(previous.lengthOfMonth())
            val income = container.transactions.totalBetween(TxType.INCOME, from, to)
            val expense = container.transactions.totalBetween(TxType.EXPENSE, from, to)
            Notifications.post(
                applicationContext,
                4001,
                Notifications.CHANNEL_REMINDERS,
                "Last month in numbers",
                "Income ${income.toTaka()} \u2022 Expense ${expense.toTaka()} \u2022 Balance ${(income - expense).toTaka()}"
            )
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "hishab-daily-reminders"

        /** Re-enqueued whenever the reminder hour changes; a single worker covers all reminders. */
        fun schedule(context: Context, hour: Int) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(LocalTime.of(hour.coerceIn(0, 23), 0))
            if (!next.isAfter(now)) next = next.plusDays(1)
            val delay = Duration.between(now, next)

            val request = PeriodicWorkRequestBuilder<ReminderWorker>(Duration.ofDays(1))
                .setInitialDelay(delay)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
