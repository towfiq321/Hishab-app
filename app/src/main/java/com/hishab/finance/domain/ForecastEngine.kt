package com.hishab.finance.domain

import com.hishab.finance.data.local.entity.CategoryEntity
import com.hishab.finance.data.local.entity.TransactionEntity
import com.hishab.finance.data.local.entity.TxType
import com.hishab.finance.data.repository.CatalogRepository
import com.hishab.finance.data.repository.RecurringRepository
import com.hishab.finance.data.repository.TransactionRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.sqrt

enum class Confidence(val label: String) {
    LOW("Low confidence"),
    MEDIUM("Medium confidence"),
    HIGH("High confidence")
}

data class CategoryForecast(
    val categoryId: Long?,
    val label: String,
    val icon: String,
    val colorArgb: Int,
    val amount: Double,
    val recurringPart: Double
)

/**
 * An expense estimate for one month. Expenses only - the app never predicts income,
 * because income here is irregular and a wrong income estimate is a dangerous number to plan on.
 */
data class ExpenseForecast(
    val month: YearMonth,
    val expected: Double,
    val low: Double,
    val high: Double,
    val categories: List<CategoryForecast>,
    val monthsOfHistory: Int,
    val recurringTotal: Double,
    val confidence: Confidence,
    val basis: String
) {
    val isUsable: Boolean get() = expected > 0
}

/** One backtest point: what the model would have said, against what actually happened. */
data class ForecastAccuracyPoint(
    val month: YearMonth,
    val forecast: Double,
    val actual: Double
) {
    val errorPercent: Double? get() = if (forecast <= 0) null else (actual - forecast) / forecast * 100.0
}

/** Where the current month is heading if the rest of it looks like the part already spent. */
data class MonthProjection(
    val month: YearMonth,
    val spentSoFar: Double,
    val projectedTotal: Double,
    val daysElapsed: Int,
    val daysRemaining: Int
)

/**
 * Forecasting model.
 *
 * Baseline:  recency-weighted average of the last six months of *non-recurring* spending per
 *            category, with an incomplete current month scaled up to a full month.
 * Recurring: auto-posted rules are priced separately by counting their real occurrences in the
 *            target month, which keeps rent and bills exact instead of averaged.
 * Range:     the coefficient of variation of recent monthly totals, clamped to 8-28%.
 */
class ForecastEngine(
    private val transactions: TransactionRepository,
    private val recurring: RecurringRepository,
    private val catalog: CatalogRepository
) {

    suspend fun forecastFor(month: YearMonth, today: LocalDate = LocalDate.now()): ExpenseForecast {
        val history = transactions.expensesUntil(month.atDay(1).minusDays(1))
        val categories = catalog.categories().associateBy { it.id }
        val recurringExpected = recurring.expectedByCategory(month)
        return compute(history, categories, recurringExpected, month, today)
    }

    suspend fun nextMonth(today: LocalDate = LocalDate.now()): ExpenseForecast =
        forecastFor(YearMonth.from(today).plusMonths(1), today)

    /** How the model would have performed over the last [months] completed months. */
    suspend fun accuracyHistory(months: Int = 5, today: LocalDate = LocalDate.now()): List<ForecastAccuracyPoint> {
        val categories = catalog.categories().associateBy { it.id }
        val allExpenses = transactions.expensesUntil(today)
        val result = mutableListOf<ForecastAccuracyPoint>()
        val currentMonth = YearMonth.from(today)
        for (back in months downTo 1) {
            val target = currentMonth.minusMonths(back.toLong())
            val cutoff = target.atDay(1).minusDays(1)
            val priorHistory = allExpenses.filter { it.dateEpochDay <= cutoff.toEpochDay() }
            if (priorHistory.isEmpty()) continue
            val recurringExpected = recurring.expectedByCategory(target)
            val forecast = compute(priorHistory, categories, recurringExpected, target, cutoff)
            val actual = allExpenses
                .filter { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) == target }
                .sumOf { it.amount }
            if (forecast.expected > 0 || actual > 0) {
                result += ForecastAccuracyPoint(target, forecast.expected, actual)
            }
        }
        return result
    }

    suspend fun projectCurrentMonth(today: LocalDate = LocalDate.now()): MonthProjection {
        val month = YearMonth.from(today)
        val spent = transactions.totalBetween(TxType.EXPENSE, month.atDay(1), today)
        val elapsed = today.dayOfMonth
        val total = month.lengthOfMonth()
        val remaining = total - elapsed
        val dailyRate = if (elapsed > 0) spent / elapsed else 0.0
        return MonthProjection(
            month = month,
            spentSoFar = spent,
            projectedTotal = spent + dailyRate * remaining,
            daysElapsed = elapsed,
            daysRemaining = remaining
        )
    }

    companion object {
        private const val WINDOW = 6
        private const val MIN_VOLATILITY = 0.08
        private const val MAX_VOLATILITY = 0.28

        /**
         * Pure calculation, kept free of Android and Room types so it can be unit tested.
         * [history] must contain only expenses dated on or before [cutoff].
         */
        fun compute(
            history: List<TransactionEntity>,
            categories: Map<Long, CategoryEntity>,
            recurringByCategory: Map<Long, Double>,
            target: YearMonth,
            cutoff: LocalDate
        ): ExpenseForecast {
            val expenses = history.filter { it.type == TxType.EXPENSE && it.dateEpochDay <= cutoff.toEpochDay() }
            val recurringTotal = recurringByCategory.values.sum()

            if (expenses.isEmpty()) {
                return emptyForecast(target, recurringByCategory, categories, recurringTotal)
            }

            // Months considered: the six months immediately before the target month.
            val months = (1..WINDOW).map { target.minusMonths(it.toLong()) }.reversed()
            val firstDataMonth = YearMonth.from(LocalDate.ofEpochDay(expenses.minOf { it.dateEpochDay }))
            val usable = months.filter { !it.isBefore(firstDataMonth) }
            if (usable.isEmpty()) {
                return emptyForecast(target, recurringByCategory, categories, recurringTotal)
            }

            // Non-recurring rows only; auto-posted bills are priced from the rules instead.
            val discretionary = expenses.filter { it.recurringRuleId == null }

            data class MonthBucket(val month: YearMonth, val weight: Double, val scale: Double)

            val buckets = usable.mapIndexed { index, ym ->
                // An incomplete month (the current one when cutoff falls inside it) is scaled up
                // to a full month so a mid-month cutoff does not drag the average down.
                val coveredDays = when {
                    YearMonth.from(cutoff) == ym -> cutoff.dayOfMonth
                    else -> ym.lengthOfMonth()
                }
                val scale = ym.lengthOfMonth().toDouble() / coveredDays.coerceAtLeast(1)
                MonthBucket(ym, (index + 1).toDouble(), if (scale > 3.0) 3.0 else scale)
            }
            val weightSum = buckets.sumOf { it.weight }

            val byCategory = mutableMapOf<Long?, Double>()
            val monthlyTotals = mutableListOf<Double>()

            buckets.forEach { bucket ->
                val rows = discretionary.filter { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) == bucket.month }
                val scaledTotal = rows.sumOf { it.amount } * bucket.scale
                monthlyTotals += scaledTotal
                rows.groupBy { it.categoryId }.forEach { (categoryId, group) ->
                    val scaled = group.sumOf { it.amount } * bucket.scale
                    byCategory[categoryId] = (byCategory[categoryId] ?: 0.0) + scaled * bucket.weight / weightSum
                }
            }

            // Merge in the recurring component.
            recurringByCategory.forEach { (categoryId, amount) ->
                byCategory[categoryId] = (byCategory[categoryId] ?: 0.0) + amount
            }

            val lines = byCategory
                .filter { it.value > 0.5 }
                .map { (categoryId, amount) ->
                    val category = categoryId?.let { categories[it] }
                    CategoryForecast(
                        categoryId = categoryId,
                        label = category?.name ?: "Uncategorised",
                        icon = category?.icon ?: "\uD83D\uDCCC",
                        colorArgb = category?.colorArgb ?: 0xFF94A3B8.toInt(),
                        amount = roundTo(amount, 50),
                        recurringPart = recurringByCategory[categoryId] ?: 0.0
                    )
                }
                .sortedByDescending { it.amount }

            val expected = lines.sumOf { it.amount }
            val volatility = volatilityOf(monthlyTotals)
            val confidence = when {
                usable.size >= 3 && volatility <= 0.15 -> Confidence.HIGH
                usable.size >= 2 -> Confidence.MEDIUM
                else -> Confidence.LOW
            }

            return ExpenseForecast(
                month = target,
                expected = expected,
                low = roundTo(expected * (1 - volatility), 500).coerceAtLeast(0.0),
                high = roundTo(expected * (1 + volatility), 500),
                categories = lines,
                monthsOfHistory = usable.size,
                recurringTotal = recurringTotal,
                confidence = confidence,
                basis = buildBasis(usable.size, recurringTotal > 0)
            )
        }

        private fun emptyForecast(
            target: YearMonth,
            recurringByCategory: Map<Long, Double>,
            categories: Map<Long, CategoryEntity>,
            recurringTotal: Double
        ): ExpenseForecast {
            val lines = recurringByCategory.map { (categoryId, amount) ->
                val category = categories[categoryId]
                CategoryForecast(
                    categoryId = categoryId,
                    label = category?.name ?: "Uncategorised",
                    icon = category?.icon ?: "\uD83D\uDCCC",
                    colorArgb = category?.colorArgb ?: 0xFF94A3B8.toInt(),
                    amount = amount,
                    recurringPart = amount
                )
            }.sortedByDescending { it.amount }
            return ExpenseForecast(
                month = target,
                expected = recurringTotal,
                low = roundTo(recurringTotal * 0.9, 500),
                high = roundTo(recurringTotal * 1.2, 500),
                categories = lines,
                monthsOfHistory = 0,
                recurringTotal = recurringTotal,
                confidence = Confidence.LOW,
                basis = if (recurringTotal > 0)
                    "Based on your recurring expenses only. Record a few weeks of spending for a fuller estimate."
                else
                    "Not enough history yet. Add expenses for a few weeks and the estimate will appear here."
            )
        }

        /** Coefficient of variation, clamped so the range is never absurdly tight or wide. */
        private fun volatilityOf(totals: List<Double>): Double {
            val values = totals.filter { it > 0 }
            if (values.size < 2) return MAX_VOLATILITY
            val mean = values.average()
            if (mean <= 0) return MAX_VOLATILITY
            val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
            val cv = sqrt(variance) / mean
            return cv.coerceIn(MIN_VOLATILITY, MAX_VOLATILITY)
        }

        private fun buildBasis(months: Int, hasRecurring: Boolean): String {
            val base = "Estimated from $months month${if (months == 1) "" else "s"} of your spending history"
            return if (hasRecurring) "$base plus your recurring bills." else "$base."
        }

        private fun roundTo(value: Double, step: Int): Double {
            if (step <= 0 || value.isNaN() || value.isInfinite()) return 0.0
            return Math.round(value / step).toDouble() * step
        }

        fun absError(a: Double, b: Double): Double = abs(a - b)
    }
}
