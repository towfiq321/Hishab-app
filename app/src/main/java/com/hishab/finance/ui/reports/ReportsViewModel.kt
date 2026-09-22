@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.hishab.finance.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hishab.finance.AppContainer
import com.hishab.finance.core.endOfWeek
import com.hishab.finance.core.startOfWeek
import com.hishab.finance.core.toYearMonth
import com.hishab.finance.data.repository.BudgetProgress
import com.hishab.finance.domain.ExpenseForecast
import com.hishab.finance.domain.ForecastAccuracyPoint
import com.hishab.finance.domain.MonthProjection
import com.hishab.finance.domain.PeriodSummary
import com.hishab.finance.domain.ReportEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** One month of totals, used for the six-month trend bars. */
data class MonthTotals(val month: YearMonth, val income: Double, val expense: Double)

data class ReportsState(
    val loading: Boolean = true,
    val weekStart: LocalDate = LocalDate.now().startOfWeek(),
    val week: PeriodSummary? = null,
    val previousWeek: PeriodSummary? = null,
    val month: YearMonth = YearMonth.now(),
    val monthSummary: PeriodSummary? = null,
    val previousMonth: PeriodSummary? = null,
    val monthTrend: List<MonthTotals> = emptyList(),
    val budgets: List<BudgetProgress> = emptyList(),
    val forecast: ExpenseForecast? = null,
    val accuracy: List<ForecastAccuracyPoint> = emptyList(),
    val projection: MonthProjection? = null
) {
    val isCurrentWeek: Boolean get() = weekStart == LocalDate.now().startOfWeek()
    val isCurrentMonth: Boolean get() = month == YearMonth.now()
}

/**
 * Drives the Reports tab. Weekly and monthly reports are the same summary over different
 * windows; the forecast tab adds the estimate plus a backtest of it.
 */
class ReportsViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    private val weekStart = MutableStateFlow(LocalDate.now().startOfWeek())
    private val month = MutableStateFlow(YearMonth.now())

    init {
        // A single window wide enough for every view on screen: six months back to the end of
        // whichever period the user has stepped to.
        combine(weekStart, month) { w, m -> w to m }
            .flatMapLatest { (w, m) ->
                val from = minOf(w, m.atDay(1)).minusMonths(6).withDayOfMonth(1)
                val to = maxOf(w.endOfWeek(), m.atEndOfMonth())
                combine(
                    container.transactions.observeItemsBetween(from, to),
                    container.budgets.observeMonth(m)
                ) { items, budgets -> Triple(items, budgets, w to m) }
            }
            .onEach { (items, budgets, period) ->
                val (w, m) = period
                val previousWeekStart = w.minusWeeks(1)
                val previousMonth = m.minusMonths(1)
                val trend = (5 downTo 0).map { back ->
                    val ym = m.minusMonths(back.toLong())
                    val summary = ReportEngine.summarise(items, ym.atDay(1), ym.atEndOfMonth())
                    MonthTotals(ym, summary.income, summary.expense)
                }
                _state.value = _state.value.copy(
                    loading = false,
                    weekStart = w,
                    week = ReportEngine.summarise(items, w, w.endOfWeek()),
                    previousWeek = ReportEngine.summarise(items, previousWeekStart, previousWeekStart.endOfWeek()),
                    month = m,
                    monthSummary = ReportEngine.summarise(items, m.atDay(1), m.atEndOfMonth()),
                    previousMonth = ReportEngine.summarise(items, previousMonth.atDay(1), previousMonth.atEndOfMonth()),
                    monthTrend = trend,
                    budgets = budgets
                )
                refreshForecast()
            }
            .launchIn(viewModelScope)
    }

    private fun refreshForecast() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val forecast = runCatching { container.forecast.nextMonth(today) }.getOrNull()
            val accuracy = runCatching { container.forecast.accuracyHistory(5, today) }.getOrDefault(emptyList())
            val projection = runCatching { container.forecast.projectCurrentMonth(today) }.getOrNull()
            _state.value = _state.value.copy(
                forecast = forecast,
                accuracy = accuracy,
                projection = projection
            )
        }
    }

    fun previousWeek() { weekStart.value = weekStart.value.minusWeeks(1) }

    fun nextWeek() {
        val candidate = weekStart.value.plusWeeks(1)
        if (!candidate.isAfter(LocalDate.now().startOfWeek())) weekStart.value = candidate
    }

    fun previousMonth() { month.value = month.value.minusMonths(1) }

    fun nextMonth() {
        val candidate = month.value.plusMonths(1)
        if (!candidate.isAfter(LocalDate.now().toYearMonth())) month.value = candidate
    }
}
