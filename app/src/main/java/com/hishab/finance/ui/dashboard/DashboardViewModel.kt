package com.hishab.finance.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hishab.finance.AppContainer
import com.hishab.finance.core.endOfMonth
import com.hishab.finance.core.endOfWeek
import com.hishab.finance.core.startOfMonth
import com.hishab.finance.core.startOfWeek
import com.hishab.finance.core.toYearMonth
import com.hishab.finance.data.local.entity.RecurringRuleEntity
import com.hishab.finance.data.repository.BudgetProgress
import com.hishab.finance.data.repository.TransactionItem
import com.hishab.finance.domain.ExpenseForecast
import com.hishab.finance.domain.MonthProjection
import com.hishab.finance.domain.PeriodSummary
import com.hishab.finance.domain.ReportEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val day: PeriodSummary? = null,
    val week: PeriodSummary? = null,
    val month: PeriodSummary? = null,
    val previousMonth: PeriodSummary? = null,
    val recent: List<TransactionItem> = emptyList(),
    val forecast: ExpenseForecast? = null,
    val projection: MonthProjection? = null,
    val budgetWarnings: List<BudgetProgress> = emptyList(),
    val upcomingBills: List<RecurringRuleEntity> = emptyList()
)

class DashboardViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        val today = LocalDate.now()
        val windowStart = today.startOfMonth().minusMonths(1)
        val windowEnd = today.endOfMonth()

        combine(
            container.transactions.observeItemsBetween(windowStart, windowEnd),
            container.budgets.observeMonth(today.toYearMonth())
        ) { items, budgets -> items to budgets }
            .onEach { (items, budgets) ->
                val monthStart = today.startOfMonth()
                val monthEnd = today.endOfMonth()
                val previousStart = monthStart.minusMonths(1)
                val previousEnd = previousStart.endOfMonth()

                _state.value = _state.value.copy(
                    loading = false,
                    today = today,
                    day = ReportEngine.day(items, today),
                    week = ReportEngine.summarise(items, today.startOfWeek(), today.endOfWeek()),
                    month = ReportEngine.summarise(items, monthStart, monthEnd),
                    previousMonth = ReportEngine.summarise(items, previousStart, previousEnd),
                    recent = items.take(6),
                    budgetWarnings = budgets.filter { it.ratio >= 0.85f }
                )
                refreshDerived(today)
            }
            .launchIn(viewModelScope)
    }

    /** Forecast, projection and bills need suspend queries, so they refresh alongside the flows. */
    private fun refreshDerived(today: LocalDate) {
        viewModelScope.launch {
            val forecast = runCatching { container.forecast.nextMonth(today) }.getOrNull()
            val projection = runCatching { container.forecast.projectCurrentMonth(today) }.getOrNull()
            val bills = runCatching { container.recurring.upcoming(days = 10, today = today) }.getOrDefault(emptyList())
            _state.value = _state.value.copy(
                forecast = forecast,
                projection = projection,
                upcomingBills = bills
            )
        }
    }
}
