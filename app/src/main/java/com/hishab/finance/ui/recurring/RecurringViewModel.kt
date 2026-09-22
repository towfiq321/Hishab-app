package com.hishab.finance.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hishab.finance.AppContainer
import com.hishab.finance.core.DateX
import com.hishab.finance.data.local.entity.CategoryEntity
import com.hishab.finance.data.local.entity.PaymentMethod
import com.hishab.finance.data.local.entity.Recurrence
import com.hishab.finance.data.local.entity.RecurringRuleEntity
import com.hishab.finance.data.local.entity.TxType
import com.hishab.finance.data.repository.RecurringRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** The add/edit form for one recurring expense. */
data class RuleFormState(
    val editingId: Long? = null,
    val title: String = "",
    val amountText: String = "",
    val categoryId: Long? = null,
    val method: PaymentMethod = PaymentMethod.CASH,
    val recurrence: Recurrence = Recurrence.MONTHLY,
    val intervalText: String = "1",
    val startDate: LocalDate = DateX.today(),
    val endDate: LocalDate? = null,
    val autoPost: Boolean = true,
    val note: String = "",
    val error: String? = null
) {
    val isEditing: Boolean get() = editingId != null
    val amount: Double get() = amountText.trim().toDoubleOrNull() ?: 0.0
    val interval: Int get() = intervalText.trim().toIntOrNull()?.coerceAtLeast(1) ?: 1
    val canSave: Boolean get() = title.isNotBlank() && amount > 0.0 && categoryId != null
}

class RecurringViewModel(private val container: AppContainer) : ViewModel() {

    val rules: StateFlow<List<RecurringRuleEntity>> =
        container.recurring.observeRules()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> =
        container.catalog.observeCategories(TxType.EXPENSE)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _form = MutableStateFlow<RuleFormState?>(null)
    val form: StateFlow<RuleFormState?> = _form.asStateFlow()

    fun startNew() {
        viewModelScope.launch {
            val first = container.catalog.categories().firstOrNull { it.type == TxType.EXPENSE }
            _form.value = RuleFormState(categoryId = first?.id)
        }
    }

    fun startEdit(rule: RecurringRuleEntity) {
        _form.value = RuleFormState(
            editingId = rule.id,
            title = rule.title,
            amountText = if (rule.amount % 1.0 == 0.0) rule.amount.toLong().toString() else rule.amount.toString(),
            categoryId = rule.categoryId,
            method = rule.paymentMethod,
            recurrence = rule.recurrence,
            intervalText = rule.interval.toString(),
            startDate = LocalDate.ofEpochDay(rule.startEpochDay),
            endDate = rule.endEpochDay?.let { LocalDate.ofEpochDay(it) },
            autoPost = rule.autoPost,
            note = rule.note
        )
    }

    fun cancelEdit() { _form.value = null }

    fun update(block: (RuleFormState) -> RuleFormState) {
        _form.value = _form.value?.let(block)
    }

    fun save() {
        val form = _form.value ?: return
        val categoryId = form.categoryId
        if (!form.canSave || categoryId == null) {
            _form.value = form.copy(error = "Give it a name, an amount and a category.")
            return
        }
        viewModelScope.launch {
            if (form.isEditing) {
                val existing = container.recurring.byId(form.editingId!!) ?: return@launch
                // Keep the posting cursor unless the start date moved forward past it.
                val nextDue = if (form.startDate.toEpochDay() > existing.nextDueEpochDay)
                    form.startDate.toEpochDay() else existing.nextDueEpochDay
                container.recurring.update(
                    existing.copy(
                        title = form.title.trim(),
                        amount = form.amount,
                        categoryId = categoryId,
                        paymentMethod = form.method,
                        recurrence = form.recurrence,
                        interval = form.interval,
                        startEpochDay = form.startDate.toEpochDay(),
                        endEpochDay = form.endDate?.toEpochDay(),
                        nextDueEpochDay = nextDue,
                        autoPost = form.autoPost,
                        note = form.note.trim()
                    )
                )
            } else {
                container.recurring.add(
                    RecurringRuleEntity(
                        title = form.title.trim(),
                        amount = form.amount,
                        categoryId = categoryId,
                        paymentMethod = form.method,
                        recurrence = form.recurrence,
                        interval = form.interval,
                        startEpochDay = form.startDate.toEpochDay(),
                        endEpochDay = form.endDate?.toEpochDay(),
                        nextDueEpochDay = form.startDate.toEpochDay(),
                        autoPost = form.autoPost,
                        note = form.note.trim()
                    )
                )
            }
            _form.value = null
            // Anything already due is written straight away so the numbers agree with the list.
            container.recurring.postDue()
        }
    }

    fun setActive(rule: RecurringRuleEntity, active: Boolean) {
        viewModelScope.launch { container.recurring.setActive(rule, active) }
    }

    fun delete(rule: RecurringRuleEntity) {
        viewModelScope.launch { container.recurring.delete(rule) }
    }

    fun monthlyEquivalent(rule: RecurringRuleEntity): Double =
        RecurringRepository.monthlyEquivalent(rule)
}
