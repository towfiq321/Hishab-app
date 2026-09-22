@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.hishab.finance.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hishab.finance.AppContainer
import com.hishab.finance.data.local.entity.CategoryEntity
import com.hishab.finance.data.local.entity.TxType
import com.hishab.finance.data.repository.BudgetProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.YearMonth

data class BudgetScreenState(
    val loading: Boolean = true,
    val month: YearMonth = YearMonth.now(),
    val lines: List<BudgetProgress> = emptyList(),
    /** Expense categories that have no limit set for this month yet. */
    val unbudgeted: List<CategoryEntity> = emptyList(),
    val message: String? = null
) {
    val planned: Double get() = lines.sumOf { it.budget }
    val spent: Double get() = lines.sumOf { it.spent }
    val remaining: Double get() = planned - spent
    val isCurrentMonth: Boolean get() = month == YearMonth.now()
}

class BudgetViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(BudgetScreenState())
    val state: StateFlow<BudgetScreenState> = _state.asStateFlow()

    private val month = MutableStateFlow(YearMonth.now())

    init {
        month
            .flatMapLatest { ym ->
                combine(
                    container.budgets.observeMonth(ym),
                    container.catalog.observeCategories(TxType.EXPENSE)
                ) { lines, categories -> Triple(ym, lines, categories) }
            }
            .onEach { (ym, lines, categories) ->
                val budgeted = lines.map { it.category.id }.toSet()
                _state.value = _state.value.copy(
                    loading = false,
                    month = ym,
                    lines = lines,
                    unbudgeted = categories.filter { it.id !in budgeted && !it.isArchived }
                )
            }
            .launchIn(viewModelScope)
    }

    fun previousMonth() { month.value = month.value.minusMonths(1) }
    fun nextMonth() { month.value = month.value.plusMonths(1) }

    fun setBudget(categoryId: Long, amount: Double) {
        viewModelScope.launch { container.budgets.setBudget(month.value, categoryId, amount) }
    }

    fun removeBudget(categoryId: Long) {
        viewModelScope.launch { container.budgets.removeBudget(month.value, categoryId) }
    }

    fun copyFromPreviousMonth() {
        viewModelScope.launch {
            val copied = container.budgets.copyFromPreviousMonth(month.value)
            _state.value = _state.value.copy(
                message = if (copied == 0) "Nothing to copy from last month."
                else "Copied $copied budget${if (copied == 1) "" else "s"} from last month."
            )
        }
    }

    fun clearMessage() { _state.value = _state.value.copy(message = null) }
}
