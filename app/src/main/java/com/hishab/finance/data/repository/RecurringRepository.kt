package com.hishab.finance.data.repository

import com.hishab.finance.data.local.dao.RecurringDao
import com.hishab.finance.data.local.dao.TransactionDao
import com.hishab.finance.data.local.entity.Recurrence
import com.hishab.finance.data.local.entity.RecurringRuleEntity
import com.hishab.finance.data.local.entity.TransactionEntity
import com.hishab.finance.data.local.entity.TxType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

class RecurringRepository(
    private val dao: RecurringDao,
    private val transactionDao: TransactionDao
) {

    fun observeRules(): Flow<List<RecurringRuleEntity>> = dao.observeAll()

    suspend fun rules(): List<RecurringRuleEntity> = dao.all()
    suspend fun byId(id: Long): RecurringRuleEntity? = dao.byId(id)
    suspend fun add(rule: RecurringRuleEntity): Long = dao.insert(rule)
    suspend fun update(rule: RecurringRuleEntity) = dao.update(rule)
    suspend fun delete(rule: RecurringRuleEntity) = dao.delete(rule)
    suspend fun setActive(rule: RecurringRuleEntity, active: Boolean) = dao.update(rule.copy(isActive = active))

    /**
     * Writes any expense that has fallen due since the last launch and moves the rule forward.
     * Called on app start, so a rule can never silently skip a period.
     * Returns the number of transactions created.
     */
    suspend fun postDue(today: LocalDate = LocalDate.now()): Int {
        var created = 0
        dao.active().forEach { rule ->
            if (!rule.autoPost) return@forEach
            var due = LocalDate.ofEpochDay(rule.nextDueEpochDay)
            var guard = 0
            var last = rule.lastPostedEpochDay
            while (!due.isAfter(today) && guard < 400) {
                val endsBefore = rule.endEpochDay?.let { due.toEpochDay() > it } ?: false
                if (endsBefore) break
                transactionDao.insert(
                    TransactionEntity(
                        type = TxType.EXPENSE,
                        amount = rule.amount,
                        dateEpochDay = due.toEpochDay(),
                        timeMinutes = 9 * 60,
                        categoryId = rule.categoryId,
                        incomeSourceId = null,
                        description = rule.title,
                        paymentMethod = rule.paymentMethod,
                        note = if (rule.note.isBlank()) "Auto-posted recurring expense" else rule.note,
                        recurringRuleId = rule.id
                    )
                )
                created++
                last = due.toEpochDay()
                due = nextDate(due, rule.recurrence, rule.interval)
                guard++
            }
            if (guard > 0 || last != rule.lastPostedEpochDay) {
                dao.update(rule.copy(nextDueEpochDay = due.toEpochDay(), lastPostedEpochDay = last))
            }
        }
        return created
    }

    /** Rules due within the next [days] days, soonest first. Used by the dashboard and reminders. */
    suspend fun upcoming(days: Int = 7, today: LocalDate = LocalDate.now()): List<RecurringRuleEntity> =
        dao.active()
            .filter { it.nextDueEpochDay in today.toEpochDay()..today.plusDays(days.toLong()).toEpochDay() }
            .sortedBy { it.nextDueEpochDay }

    /**
     * Expected cost per category for [month], counting how many times each rule actually falls
     * inside that month. This is what the forecast adds on top of the historical baseline.
     */
    suspend fun expectedByCategory(month: YearMonth): Map<Long, Double> {
        val result = mutableMapOf<Long, Double>()
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()
        // Only auto-posted rules are added on top of history. A rule the user pays manually is
        // already present in past transactions, so adding it again would double count.
        dao.active().filter { it.autoPost }.forEach { rule ->
            val occurrences = occurrencesIn(rule, monthStart, monthEnd)
            if (occurrences > 0) {
                result[rule.categoryId] = (result[rule.categoryId] ?: 0.0) + occurrences * rule.amount
            }
        }
        return result
    }

    private fun occurrencesIn(rule: RecurringRuleEntity, from: LocalDate, to: LocalDate): Int {
        var date = LocalDate.ofEpochDay(rule.startEpochDay)
        val end = rule.endEpochDay?.let { LocalDate.ofEpochDay(it) }
        var count = 0
        var guard = 0
        // Fast-forward cheaply for long daily histories.
        if (rule.recurrence == Recurrence.DAILY || rule.recurrence == Recurrence.CUSTOM) {
            val step = rule.interval.coerceAtLeast(1)
            if (date.isBefore(from)) {
                val gap = from.toEpochDay() - date.toEpochDay()
                val jumps = gap / step
                date = date.plusDays(jumps * step)
            }
        }
        while (!date.isAfter(to) && guard < 2000) {
            if (!date.isBefore(from) && (end == null || !date.isAfter(end))) count++
            date = nextDate(date, rule.recurrence, rule.interval)
            guard++
        }
        return count
    }

    companion object {
        fun nextDate(from: LocalDate, recurrence: Recurrence, interval: Int): LocalDate {
            val n = interval.coerceAtLeast(1).toLong()
            return when (recurrence) {
                Recurrence.DAILY -> from.plusDays(n)
                Recurrence.WEEKLY -> from.plusWeeks(n)
                Recurrence.MONTHLY -> from.plusMonths(n)
                Recurrence.YEARLY -> from.plusYears(n)
                Recurrence.CUSTOM -> from.plusDays(n)
            }
        }

        /** Roughly what a rule costs per 30 days - used for the "per month" label on the rules list. */
        fun monthlyEquivalent(rule: RecurringRuleEntity): Double {
            val n = rule.interval.coerceAtLeast(1)
            return when (rule.recurrence) {
                Recurrence.DAILY, Recurrence.CUSTOM -> rule.amount * (30.0 / n)
                Recurrence.WEEKLY -> rule.amount * (52.0 / 12.0) / n
                Recurrence.MONTHLY -> rule.amount / n
                Recurrence.YEARLY -> rule.amount / (12.0 * n)
            }
        }
    }
}

fun nextDate(from: LocalDate, recurrence: Recurrence, interval: Int): LocalDate =
    RecurringRepository.nextDate(from, recurrence, interval)
