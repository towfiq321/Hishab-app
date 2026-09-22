package com.hishab.finance.ui.categories

import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hishab.finance.AppContainer
import com.hishab.finance.data.local.entity.CategoryEntity
import com.hishab.finance.data.local.entity.TxType
import com.hishab.finance.ui.theme.ChartPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryFormState(
    val editingId: Long? = null,
    val name: String = "",
    val type: TxType = TxType.EXPENSE,
    val icon: String = "\uD83D\uDCCC",
    val colorArgb: Int = 0xFF6366F1.toInt(),
    val usageCount: Int = 0,
    val error: String? = null
) {
    val isEditing: Boolean get() = editingId != null
    val canSave: Boolean get() = name.isNotBlank()
}

class CategoriesViewModel(private val container: AppContainer) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> =
        container.catalog.observeCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _form = MutableStateFlow<CategoryFormState?>(null)
    val form: StateFlow<CategoryFormState?> = _form.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    companion object {
        /** A broad emoji set covering the default categories and common custom ones. */
        val ICONS = listOf(
            "\uD83C\uDF5B", "\uD83C\uDF54", "\u2615", "\uD83D\uDE96", "\uD83D\uDE8C",
            "\u26FD", "\uD83C\uDFE0", "\uD83D\uDCA1", "\uD83D\uDCA7", "\uD83D\uDECD",
            "\uD83D\uDC8A", "\uD83C\uDFE5", "\uD83D\uDCDA", "\uD83C\uDFAC", "\uD83C\uDFAE",
            "\uD83D\uDCF1", "\uD83D\uDCBB", "\uD83D\uDC6A", "\uD83D\uDC76", "\uD83D\uDCBC",
            "\u2708\uFE0F", "\uD83C\uDF81", "\uD83D\uDC55", "\uD83D\uDC8D", "\uD83D\uDCB0",
            "\uD83C\uDFEA", "\uD83E\uDDE0", "\uD83E\uDD1D", "\uD83D\uDCC8", "\uD83D\uDD27",
            "\u2795", "\uD83D\uDCCC"
        )
        val COLORS: List<Int> = ChartPalette.map { it.toArgb() }
    }

    fun startNew(type: TxType) {
        _form.value = CategoryFormState(type = type)
    }

    fun startEdit(category: CategoryEntity) {
        _form.value = CategoryFormState(
            editingId = category.id,
            name = category.name,
            type = category.type,
            icon = category.icon,
            colorArgb = category.colorArgb
        )
        viewModelScope.launch {
            val used = container.catalog.categoryUsageCount(category.id)
            _form.value = _form.value?.copy(usageCount = used)
        }
    }

    fun cancelEdit() { _form.value = null }

    fun update(block: (CategoryFormState) -> CategoryFormState) {
        _form.value = _form.value?.let(block)
    }

    fun save() {
        val form = _form.value ?: return
        if (!form.canSave) {
            _form.value = form.copy(error = "Give the category a name.")
            return
        }
        viewModelScope.launch {
            val entity = CategoryEntity(
                id = form.editingId ?: 0,
                name = form.name.trim(),
                type = form.type,
                icon = form.icon,
                colorArgb = form.colorArgb,
                isDefault = false
            )
            val result = runCatching {
                if (form.isEditing) {
                    // Preserve the default flag and sort order of a built-in category.
                    val existing = container.catalog.category(form.editingId!!)
                    container.catalog.updateCategory(
                        entity.copy(
                            isDefault = existing?.isDefault ?: false,
                            sortOrder = existing?.sortOrder ?: 0
                        )
                    )
                } else {
                    container.catalog.addCategory(entity)
                }
            }
            if (result.isSuccess) {
                _form.value = null
            } else {
                _form.value = _form.value?.copy(error = "A category with that name already exists.")
            }
        }
    }

    fun delete(category: CategoryEntity) {
        viewModelScope.launch {
            val used = container.catalog.categoryUsageCount(category.id)
            container.catalog.deleteCategory(category)
            _form.value = null
            _message.value = if (used == 0) "Category deleted."
            else "Category deleted. $used transaction${if (used == 1) "" else "s"} kept and marked uncategorised."
        }
    }

    fun clearMessage() { _message.value = null }
}
