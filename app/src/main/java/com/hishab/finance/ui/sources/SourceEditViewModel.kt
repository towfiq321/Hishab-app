package com.hishab.finance.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hishab.finance.AppContainer
import com.hishab.finance.data.local.entity.IncomeSourceEntity
import com.hishab.finance.data.local.entity.PayFrequency
import androidx.compose.ui.graphics.toArgb
import com.hishab.finance.ui.theme.ChartPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The full income-source form from section 7 of the brief. */
data class SourceFormState(
    val loading: Boolean = true,
    val editingId: Long? = null,
    val name: String = "",
    val sourceType: String = "Consulting",
    val description: String = "",
    val contactName: String = "",
    val contactPhone: String = "",
    val email: String = "",
    val address: String = "",
    val payFrequency: PayFrequency = PayFrequency.MONTHLY,
    val typicalAmountText: String = "",
    val notes: String = "",
    val icon: String = "\uD83D\uDCBC",
    val colorArgb: Int = 0xFF10B981.toInt(),
    val error: String? = null,
    val saved: Boolean = false
) {
    val isEditing: Boolean get() = editingId != null
    val typicalAmount: Double get() = typicalAmountText.trim().toDoubleOrNull() ?: 0.0
    val canSave: Boolean get() = name.isNotBlank()
}

class SourceEditViewModel(
    private val container: AppContainer,
    private val sourceId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(SourceFormState())
    val state: StateFlow<SourceFormState> = _state.asStateFlow()

    companion object {
        val SOURCE_TYPES = listOf(
            "Salary", "Consulting", "Business", "Solar Projects", "Meter Sales",
            "Commission", "Freelancing", "Investment", "Rental", "Other"
        )
        val ICONS = listOf(
            "\uD83D\uDCBC", "\uD83D\uDCB0", "\u2600\uFE0F", "\uD83D\uDD0C", "\uD83C\uDFEA",
            "\uD83E\uDDE0", "\uD83D\uDCBB", "\uD83E\uDD1D", "\uD83D\uDCC8", "\uD83C\uDFE0"
        )
        val COLORS: List<Int> = ChartPalette.map { it.toArgb() }
    }

    init {
        viewModelScope.launch {
            val existing = if (sourceId > 0) container.catalog.source(sourceId) else null
            _state.value = if (existing == null) {
                _state.value.copy(loading = false)
            } else {
                SourceFormState(
                    loading = false,
                    editingId = existing.id,
                    name = existing.name,
                    sourceType = existing.sourceType,
                    description = existing.description,
                    contactName = existing.contactName,
                    contactPhone = existing.contactPhone,
                    email = existing.email,
                    address = existing.address,
                    payFrequency = existing.payFrequency,
                    typicalAmountText = if (existing.typicalAmount > 0)
                        existing.typicalAmount.toLong().toString() else "",
                    notes = existing.notes,
                    icon = existing.icon,
                    colorArgb = existing.colorArgb
                )
            }
        }
    }

    fun setName(value: String) { _state.value = _state.value.copy(name = value, error = null) }
    fun setType(value: String) { _state.value = _state.value.copy(sourceType = value) }
    fun setDescription(value: String) { _state.value = _state.value.copy(description = value) }
    fun setContactName(value: String) { _state.value = _state.value.copy(contactName = value) }
    fun setPhone(value: String) { _state.value = _state.value.copy(contactPhone = value) }
    fun setEmail(value: String) { _state.value = _state.value.copy(email = value) }
    fun setAddress(value: String) { _state.value = _state.value.copy(address = value) }
    fun setFrequency(value: PayFrequency) { _state.value = _state.value.copy(payFrequency = value) }
    fun setTypicalAmount(value: String) {
        _state.value = _state.value.copy(typicalAmountText = value.filter { it.isDigit() || it == '.' })
    }
    fun setNotes(value: String) { _state.value = _state.value.copy(notes = value) }
    fun setIcon(value: String) { _state.value = _state.value.copy(icon = value) }
    fun setColor(value: Int) { _state.value = _state.value.copy(colorArgb = value) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.value = s.copy(error = "Give the source a name.")
            return
        }
        viewModelScope.launch {
            val entity = IncomeSourceEntity(
                id = s.editingId ?: 0,
                name = s.name.trim(),
                sourceType = s.sourceType,
                description = s.description.trim(),
                contactName = s.contactName.trim(),
                contactPhone = s.contactPhone.trim(),
                email = s.email.trim(),
                address = s.address.trim(),
                payFrequency = s.payFrequency,
                typicalAmount = s.typicalAmount,
                notes = s.notes.trim(),
                colorArgb = s.colorArgb,
                icon = s.icon
            )
            val result = runCatching {
                if (s.editingId == null) container.catalog.addSource(entity)
                else container.catalog.updateSource(entity)
            }
            _state.value = if (result.isSuccess) {
                _state.value.copy(saved = true)
            } else {
                // The only realistic failure is the unique name index.
                _state.value.copy(error = "A source with that name already exists.")
            }
        }
    }
}
