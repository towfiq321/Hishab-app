package com.hishab.finance.domain

import com.hishab.finance.data.local.entity.TxType
import com.hishab.finance.data.repository.TransactionItem
import java.time.LocalDate

/** One calendar day of totals - the unit every trend chart is built from. */
data class DayPoint(
    val date: LocalDate,
    val income: Double,
    val expense: Double
) {
    val net: Double get() = income - expense
    val hasActivity: Boolean get() = income > 0 || expense > 0
}

/** One slice of a breakdown: a category, an income source or a payment method. */
data class BreakdownSlice(
    val id: Long?,
    val label: String,
    val icon: String,
    val colorArgb: Int,
    val amount: Double,
    val count: Int,
    val share: Float
)

/**
 * Totals and breakdowns for any date range. Daily, weekly and monthly reports are all the
 * same calculation over a different window, so there is one implementation.
 */
data class PeriodSummary(
    val from: LocalDate,
    val to: LocalDate,
    val income: Double,
    val expense: Double,
    val incomeCount: Int,
    val expenseCount: Int,
    val days: List<DayPoint>,
    val expenseByCategory: List<BreakdownSlice>,
    val incomeBySource: List<BreakdownSlice>,
    val expenseByMethod: List<BreakdownSlice>
) {
    val net: Double get() = income - expense
    val dayCount: Int get() = days.size
    /** Averaged over elapsed days only, so a half-finished month is not understated. */
    val elapsedDays: Int
        get() = days.count { !it.date.isAfter(LocalDate.now()) }.coerceAtLeast(1)
    val avgDailyExpense: Double get() = expense / elapsedDays
    val avgDailyIncome: Double get() = income / elapsedDays
    val topExpenseDay: DayPoint? get() = days.filter { it.expense > 0 }.maxByOrNull { it.expense }
    val topIncomeDay: DayPoint? get() = days.filter { it.income > 0 }.maxByOrNull { it.income }
    val lowestExpenseDay: DayPoint? get() = days.filter { it.expense > 0 }.minByOrNull { it.expense }
    val topExpenseCategory: BreakdownSlice? get() = expenseByCategory.firstOrNull()
    val topIncomeSource: BreakdownSlice? get() = incomeBySource.firstOrNull()
    val savingsRate: Double get() = if (income <= 0) 0.0 else (net / income) * 100.0
    val isEmpty: Boolean get() = incomeCount == 0 && expenseCount == 0
}

object ReportEngine {

    /** Builds a summary from already-joined items. [items] may contain rows outside the window. */
    fun summarise(items: List<TransactionItem>, from: LocalDate, to: LocalDate): PeriodSummary {
        val window = items.filter { it.date >= from && it.date <= to }
        val expenses = window.filter { !it.isIncome }
        val incomes = window.filter { it.isIncome }

        val byDate = window.groupBy { it.date }
        val days = buildList {
            var d = from
            while (!d.isAfter(to)) {
                val rows = byDate[d].orEmpty()
                add(
                    DayPoint(
                        date = d,
                        income = rows.filter { it.isIncome }.sumOf { it.tx.amount },
                        expense = rows.filter { !it.isIncome }.sumOf { it.tx.amount }
                    )
                )
                d = d.plusDays(1)
            }
        }

        val expenseTotal = expenses.sumOf { it.tx.amount }
        val incomeTotal = incomes.sumOf { it.tx.amount }

        return PeriodSummary(
            from = from,
            to = to,
            income = incomeTotal,
            expense = expenseTotal,
            incomeCount = incomes.size,
            expenseCount = expenses.size,
            days = days,
            expenseByCategory = slices(expenses, expenseTotal) { item ->
                Triple(
                    item.tx.categoryId,
                    item.category?.name ?: "Uncategorised",
                    (item.category?.icon ?: "\uD83D\uDCCC") to (item.category?.colorArgb ?: 0xFF94A3B8.toInt())
                )
            },
            incomeBySource = slices(incomes, incomeTotal) { item ->
                Triple(
                    item.tx.incomeSourceId,
                    item.source?.name ?: item.category?.name ?: "Other income",
                    (item.source?.icon ?: "\uD83D\uDCB0") to (item.source?.colorArgb ?: 0xFF10B981.toInt())
                )
            },
            expenseByMethod = slices(expenses, expenseTotal) { item ->
                Triple(
                    item.tx.paymentMethod.ordinal.toLong(),
                    item.tx.paymentMethod.label,
                    methodIcon(item.tx.paymentMethod.ordinal) to methodColor(item.tx.paymentMethod.ordinal)
                )
            }
        )
    }

    /** Daily view: one day is just a one-day window. */
    fun day(items: List<TransactionItem>, date: LocalDate): PeriodSummary = summarise(items, date, date)

    private inline fun slices(
        rows: List<TransactionItem>,
        total: Double,
        keyOf: (TransactionItem) -> Triple<Long?, String, Pair<String, Int>>
    ): List<BreakdownSlice> {
        if (rows.isEmpty()) return emptyList()
        return rows.groupBy { keyOf(it) }
            .map { (key, group) ->
                val amount = group.sumOf { it.tx.amount }
                BreakdownSlice(
                    id = key.first,
                    label = key.second,
                    icon = key.third.first,
                    colorArgb = key.third.second,
                    amount = amount,
                    count = group.size,
                    share = if (total <= 0) 0f else (amount / total).toFloat()
                )
            }
            .sortedByDescending { it.amount }
    }

    private fun methodIcon(ordinal: Int) = when (ordinal) {
        0 -> "\uD83D\uDCB5"
        1 -> "\uD83C\uDFE6"
        2 -> "\uD83D\uDCF2"
        3 -> "\uD83D\uDCB3"
        else -> "\uD83D\uDD04"
    }

    private fun methodColor(ordinal: Int) = when (ordinal) {
        0 -> 0xFF22C55E.toInt()
        1 -> 0xFF3B82F6.toInt()
        2 -> 0xFFF97316.toInt()
        3 -> 0xFF8B5CF6.toInt()
        else -> 0xFF94A3B8.toInt()
    }

    /** Income minus expense, kept as a named function because the spec calls it out explicitly. */
    fun balance(income: Double, expense: Double): Double = income - expense

    fun countOf(items: List<TransactionItem>, type: TxType): Int = items.count { it.tx.type == type }
}
