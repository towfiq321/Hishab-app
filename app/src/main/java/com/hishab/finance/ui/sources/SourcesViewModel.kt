package com.hishab.finance.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hishab.finance.AppContainer
import com.hishab.finance.core.toYearMonth
import com.hishab.finance.data.local.entity.IncomeSourceEntity
import com.hishab.finance.data.repository.TransactionItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * Lifetime figures for one income source. This is the heart of the Income Sources section:
 * how much a person, company or project has actually paid, and when they last did.
 */
data class SourceStats(
    val source: IncomeSourceEntity,
    val total: Double = 0.0,
    val count: Int = 0,
    val lastAmount: Double? = null,
    val lastDate: LocalDate? = null,
    val thisYear: Double = 0.0
) {
    val average: Double get() = if (count == 0) 0.0 else total / count
}

class SourcesViewModel(private val container: AppContainer) : ViewModel() {

    /** Sources ordered by how much they have actually brought in. */
    val stats: StateFlow<List<SourceStats>> =
        combine(
            container.catalog.observeSources(),
            container.transactions.observeItems()
        ) { sources, items ->
            val income = items.filter { it.isIncome }
            val bySource = income.groupBy { it.tx.incomeSourceId }
            sources.map { source -> statsFor(source, bySource[source.id].orEmpty()) }
                .sortedByDescending { it.total }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Income that has no source attached, so nothing goes missing from the totals. */
    val unassigned: StateFlow<Double> =
        container.transactions.observeItems()
            .map { items -> items.filter { it.isIncome && it.tx.incomeSourceId == null }.sumOf { it.tx.amount } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    fun delete(source: IncomeSourceEntity) {
        viewModelScope.launch { container.catalog.deleteSource(source) }
    }
}

data class SourceDetailState(
    val loading: Boolean = true,
    val source: IncomeSourceEntity? = null,
    val stats: SourceStats? = null,
    val transactions: List<TransactionItem> = emptyList(),
    val monthly: List<Pair<YearMonth, Double>> = emptyList(),
    val deleted: Boolean = false
)

class SourceDetailViewModel(
    private val container: AppContainer,
    private val sourceId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(SourceDetailState())
    val state: StateFlow<SourceDetailState> = _state.asStateFlow()

    init {
        combine(
            container.catalog.observeSource(sourceId),
            container.transactions.observeForSource(sourceId)
        ) { source, items -> source to items }
            .onEach { (source, items) ->
                val sorted = items.sortedWith(
                    compareByDescending<TransactionItem> { it.tx.dateEpochDay }
                        .thenByDescending { it.tx.timeMinutes }
                )
                _state.value = _state.value.copy(
                    loading = false,
                    source = source,
                    stats = source?.let { statsFor(it, items) },
                    transactions = sorted,
                    monthly = monthlyTotals(items)
                )
            }
            .launchIn(viewModelScope)
    }

    fun delete() {
        val source = _state.value.source ?: return
        viewModelScope.launch {
            container.catalog.deleteSource(source)
            _state.value = _state.value.copy(deleted = true)
        }
    }

    /** Last six months of receipts from this source, oldest first. */
    private fun monthlyTotals(items: List<TransactionItem>): List<Pair<YearMonth, Double>> {
        val current = LocalDate.now().toYearMonth()
        val byMonth = items.groupBy { it.date.toYearMonth() }
        return (5 downTo 0).map { back ->
            val ym = current.minusMonths(back.toLong())
            ym to (byMonth[ym]?.sumOf { it.tx.amount } ?: 0.0)
        }
    }
}

/** Shared by the list and the detail screen so both show identical numbers. */
internal fun statsFor(source: IncomeSourceEntity, items: List<TransactionItem>): SourceStats {
    val income = items.filter { it.isIncome }
    val latest = income.maxByOrNull { it.tx.dateEpochDay * 1440L + it.tx.timeMinutes }
    val thisYear = LocalDate.now().year
    return SourceStats(
        source = source,
        total = income.sumOf { it.tx.amount },
        count = income.size,
        lastAmount = latest?.tx?.amount,
        lastDate = latest?.date,
        thisYear = income.filter { it.date.year == thisYear }.sumOf { it.tx.amount }
    )
}
