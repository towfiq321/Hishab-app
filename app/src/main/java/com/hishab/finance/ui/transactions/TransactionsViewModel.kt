package com.hishab.finance.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hishab.finance.AppContainer
import com.hishab.finance.data.local.entity.CategoryEntity
import com.hishab.finance.data.local.entity.IncomeSourceEntity
import com.hishab.finance.data.local.entity.PaymentMethod
import com.hishab.finance.data.local.entity.TxType
import com.hishab.finance.data.repository.TransactionFilter
import com.hishab.finance.data.repository.TransactionItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class TransactionsViewModel(private val container: AppContainer) : ViewModel() {

    private val _filter = MutableStateFlow(TransactionFilter())
    val filter: StateFlow<TransactionFilter> = _filter.asStateFlow()

    val items: StateFlow<List<TransactionItem>> =
        container.transactions.observeFiltered(_filter)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> =
        container.catalog.observeCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sources: StateFlow<List<IncomeSourceEntity>> =
        container.catalog.observeSources()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) { _filter.value = _filter.value.copy(query = value) }

    fun setType(type: TxType?) { _filter.value = _filter.value.copy(type = type) }

    fun toggleCategory(id: Long) {
        val current = _filter.value.categoryIds
        _filter.value = _filter.value.copy(
            categoryIds = if (id in current) current - id else current + id
        )
    }

    fun toggleSource(id: Long) {
        val current = _filter.value.sourceIds
        _filter.value = _filter.value.copy(
            sourceIds = if (id in current) current - id else current + id
        )
    }

    fun toggleMethod(method: PaymentMethod) {
        val current = _filter.value.methods
        _filter.value = _filter.value.copy(
            methods = if (method in current) current - method else current + method
        )
    }

    fun setRange(from: LocalDate?, to: LocalDate?) {
        _filter.value = _filter.value.copy(from = from, to = to)
    }

    fun setAmountRange(min: Double?, max: Double?) {
        _filter.value = _filter.value.copy(minAmount = min, maxAmount = max)
    }

    fun clearFilters() { _filter.value = TransactionFilter(query = _filter.value.query) }

    fun delete(item: TransactionItem) {
        viewModelScope.launch { container.transactions.delete(item.tx) }
    }
}
