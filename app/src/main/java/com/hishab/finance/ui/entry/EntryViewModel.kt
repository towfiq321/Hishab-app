package com.hishab.finance.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hishab.finance.AppContainer
import com.hishab.finance.core.DateX
import com.hishab.finance.data.local.entity.CategoryEntity
import com.hishab.finance.data.local.entity.IncomeSourceEntity
import com.hishab.finance.data.local.entity.PaymentMethod
import com.hishab.finance.data.local.entity.TransactionEntity
import com.hishab.finance.data.local.entity.TxType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Everything the entry form holds. Amount stays a string while typing so the field behaves
 * naturally; it is parsed once on save.
 */
data class EntryState(
    val loading: Boolean = true,
    val editingId: Long? = null,
    val type: TxType = TxType.EXPENSE,
    val amountText: String = "",
    val date: LocalDate = DateX.today(),
    val timeMinutes: Int = DateX.nowMinutes(),
    val categoryId: Long? = null,
    val sourceId: Long? = null,
    val description: String = "",
    val method: PaymentMethod = PaymentMethod.CASH,
    val note: String = "",
    val error: String? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false
) {
    val isEditing: Boolean get() = editingId != null
    val amount: Double get() = amountText.trim().replace(",", "").toDoubleOrNull() ?: 0.0
    val canSave: Boolean get() = amount > 0.0
}

/**
 * Backs the add/edit form for both expenses and income. The same screen handles both because the
 * fields only differ by one row (category vs income source).
 */
class EntryViewModel(
    private val container: AppContainer,
    private val transactionId: Long,
    initialType: TxType
) : ViewModel() {

    private val _state = MutableStateFlow(EntryState(type = initialType))
    val state: StateFlow<EntryState> = _state.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> =
        container.catalog.observeCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sources: StateFlow<List<IncomeSourceEntity>> =
        container.catalog.observeSources()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val existing = if (transactionId > 0) container.transactions.byId(transactionId) else null
            _state.value = if (existing != null) {
                EntryState(
                    loading = false,
                    editingId = existing.id,
                    type = existing.type,
                    amountText = trimZeros(existing.amount),
                    date = LocalDate.ofEpochDay(existing.dateEpochDay),
                    timeMinutes = existing.timeMinutes,
                    categoryId = existing.categoryId,
                    sourceId = existing.incomeSourceId,
                    description = existing.description,
                    method = existing.paymentMethod,
                    note = existing.note
                )
            } else {
                val defaults = container.catalog.categories().filter { it.type == initialType }
                _state.value.copy(
                    loading = false,
                    categoryId = defaults.firstOrNull()?.id
                )
            }
        }
    }

    fun setType(type: TxType) {
        if (_state.value.type == type) return
        viewModelScope.launch {
            // Categories are typed, so switching kind has to pick a valid one.
            val first = container.catalog.categories().firstOrNull { it.type == type }
            _state.value = _state.value.copy(type = type, categoryId = first?.id, error = null)
        }
    }

    fun setAmount(value: String) {
        val cleaned = value.filter { it.isDigit() || it == '.' }
        _state.value = _state.value.copy(amountText = cleaned, error = null)
    }

    fun setDate(date: LocalDate) { _state.value = _state.value.copy(date = date) }
    fun setTime(minutes: Int) { _state.value = _state.value.copy(timeMinutes = minutes) }
    fun setCategory(id: Long?) { _state.value = _state.value.copy(categoryId = id) }
    fun setSource(id: Long?) { _state.value = _state.value.copy(sourceId = id) }
    fun setDescription(value: String) { _state.value = _state.value.copy(description = value) }
    fun setMethod(method: PaymentMethod) { _state.value = _state.value.copy(method = method) }
    fun setNote(value: String) { _state.value = _state.value.copy(note = value) }

    fun save() {
        val s = _state.value
        if (s.amount <= 0.0) {
            _state.value = s.copy(error = "Enter an amount greater than zero.")
            return
        }
        viewModelScope.launch {
            val entity = TransactionEntity(
                id = s.editingId ?: 0,
                type = s.type,
                amount = s.amount,
                dateEpochDay = s.date.toEpochDay(),
                timeMinutes = s.timeMinutes,
                categoryId = s.categoryId,
                incomeSourceId = if (s.type == TxType.INCOME) s.sourceId else null,
                description = s.description.trim(),
                paymentMethod = s.method,
                note = s.note.trim()
            )
            if (s.editingId == null) {
                container.transactions.add(entity)
            } else {
                // Keep the link to a recurring rule if this row was auto-posted.
                val original = container.transactions.byId(s.editingId)
                container.transactions.update(
                    entity.copy(
                        recurringRuleId = original?.recurringRuleId,
                        createdAt = original?.createdAt ?: System.currentTimeMillis()
                    )
                )
            }
            _state.value = _state.value.copy(saved = true)
        }
    }

    fun delete() {
        val id = _state.value.editingId ?: return
        viewModelScope.launch {
            container.transactions.deleteById(id)
            _state.value = _state.value.copy(deleted = true)
        }
    }

    private fun trimZeros(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
