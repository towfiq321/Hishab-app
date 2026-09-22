@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.hishab.finance.ui.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hishab.finance.core.TAKA
import com.hishab.finance.core.mediumLabel
import com.hishab.finance.core.relativeLabel
import com.hishab.finance.core.toTaka
import com.hishab.finance.data.local.entity.CategoryEntity
import com.hishab.finance.data.local.entity.PaymentMethod
import com.hishab.finance.data.local.entity.Recurrence
import com.hishab.finance.data.local.entity.RecurringRuleEntity
import com.hishab.finance.ui.components.DatePickerSheet
import com.hishab.finance.ui.components.EmptyState
import com.hishab.finance.ui.components.HishabCard
import com.hishab.finance.ui.components.IconBubble
import com.hishab.finance.ui.components.SectionHeader
import com.hishab.finance.ui.components.StatTile
import com.hishab.finance.ui.rememberViewModel
import com.hishab.finance.ui.theme.HishabTheme
import java.time.LocalDate

/** Section 12: rent, bills and subscriptions that repeat, and feed the forecast. */
@Composable
fun RecurringScreen(onBack: () -> Unit) {
    val vm: RecurringViewModel = rememberViewModel { RecurringViewModel(it) }
    val rules by vm.rules.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val form by vm.form.collectAsStateWithLifecycle()
    val money = HishabTheme.money
    var confirmDelete by remember { mutableStateOf<RecurringRuleEntity?>(null) }

    val monthlyTotal = rules.filter { it.isActive }.sumOf { vm.monthlyEquivalent(it) }
    val byId = categories.associateBy { it.id }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurring expenses") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = vm::startNew,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New rule") }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                StatTile(
                    label = "Recurring cost per month",
                    value = monthlyTotal.toTaka(),
                    tint = money.forecast,
                    caption = "${rules.count { it.isActive }} active rules \u00B7 included in your forecast",
                    emoji = "\uD83D\uDD01"
                )
            }

            if (rules.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "\uD83D\uDD01",
                        title = "No recurring expenses yet",
                        message = "Add rent, electricity, internet or a subscription. Hishab can post " +
                            "them automatically and price them exactly in the forecast.",
                        actionLabel = "Add one",
                        onAction = vm::startNew
                    )
                }
            }

            items(rules, key = { it.id }) { rule ->
                RuleCard(
                    rule = rule,
                    category = byId[rule.categoryId],
                    monthly = vm.monthlyEquivalent(rule),
                    onEdit = { vm.startEdit(rule) },
                    onToggle = { active -> vm.setActive(rule, active) },
                    onDelete = { confirmDelete = rule }
                )
            }
        }
    }

    form?.let { current ->
        RuleEditorSheet(
            form = current,
            categories = categories,
            onDismiss = vm::cancelEdit,
            onChange = vm::update,
            onSave = vm::save
        )
    }

    confirmDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete \"${rule.title}\"?") },
            text = { Text("Expenses already posted from this rule stay in your history.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(rule); confirmDelete = null }) {
                    Text("Delete", color = HishabTheme.money.expense)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun RuleCard(
    rule: RecurringRuleEntity,
    category: CategoryEntity?,
    monthly: Double,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val money = HishabTheme.money
    val due = LocalDate.ofEpochDay(rule.nextDueEpochDay)
    HishabCard(onClick = onEdit) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(
                    category?.icon ?: "\uD83D\uDD01",
                    category?.colorArgb?.let { Color(it) } ?: money.forecast
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(rule.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${category?.name ?: "Uncategorised"} \u00B7 " +
                            if (rule.interval > 1) "every ${rule.interval} \u00D7 ${rule.recurrence.label}"
                            else rule.recurrence.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    rule.amount.toTaka(),
                    style = MaterialTheme.typography.titleMedium,
                    color = money.expense
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (rule.isActive) "Next due ${due.relativeLabel()} \u00B7 ${due.mediumLabel()}"
                        else "Paused",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (rule.isActive) MaterialTheme.colorScheme.onSurfaceVariant else money.muted
                    )
                    Text(
                        "About ${monthly.toTaka()} per month" +
                            if (rule.autoPost) " \u00B7 posted automatically" else " \u00B7 reminder only",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = rule.isActive, onCheckedChange = onToggle)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = money.expense)
                }
            }
        }
    }
}

@Composable
private fun RuleEditorSheet(
    form: RuleFormState,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onChange: ((RuleFormState) -> RuleFormState) -> Unit,
    onSave: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showStart by remember { mutableStateOf(false) }
    var showEnd by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                if (form.isEditing) "Edit recurring expense" else "New recurring expense",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = form.title,
                onValueChange = { value -> onChange { it.copy(title = value, error = null) } },
                label = { Text("What is it?") },
                placeholder = { Text("House rent") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = form.amountText,
                onValueChange = { value ->
                    onChange { it.copy(amountText = value.filter { c -> c.isDigit() || c == '.' }, error = null) }
                },
                label = { Text("Amount") },
                prefix = { Text(TAKA) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            SectionHeader("Category")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { category ->
                    FilterChip(
                        selected = form.categoryId == category.id,
                        onClick = { onChange { it.copy(categoryId = category.id) } },
                        label = { Text("${category.icon}  ${category.name}") }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionHeader("How often")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Recurrence.entries.forEach { recurrence ->
                    FilterChip(
                        selected = form.recurrence == recurrence,
                        onClick = { onChange { it.copy(recurrence = recurrence) } },
                        label = { Text(recurrence.label) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = form.intervalText,
                onValueChange = { value ->
                    onChange { it.copy(intervalText = value.filter { c -> c.isDigit() }) }
                },
                label = {
                    Text(
                        if (form.recurrence == Recurrence.CUSTOM) "Every N days"
                        else "Repeat every N ${form.recurrence.label.lowercase()} periods"
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            SectionHeader("Paid with")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaymentMethod.entries.forEach { method ->
                    FilterChip(
                        selected = form.method == method,
                        onClick = { onChange { it.copy(method = method) } },
                        label = { Text(method.label) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { showStart = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("From ${form.startDate.mediumLabel()}", maxLines = 1)
                }
                OutlinedButton(
                    onClick = { showEnd = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(form.endDate?.let { "Until ${it.mediumLabel()}" } ?: "No end date", maxLines = 1)
                }
            }
            if (form.endDate != null) {
                TextButton(onClick = { onChange { it.copy(endDate = null) } }) { Text("Clear end date") }
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Post automatically", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Write the expense on its due date. Turn this off if you prefer to enter it " +
                            "yourself and only want a reminder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = form.autoPost,
                    onCheckedChange = { value -> onChange { it.copy(autoPost = value) } }
                )
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = form.note,
                onValueChange = { value -> onChange { it.copy(note = value) } },
                label = { Text("Note (optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            if (form.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    form.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onSave,
                enabled = form.canSave,
                modifier = Modifier.fillMaxWidth().height(50.dp).navigationBarsPadding(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (form.isEditing) "Save rule" else "Add rule")
            }
        }
    }

    if (showStart) {
        DatePickerSheet(
            initial = form.startDate,
            onDismiss = { showStart = false },
            onPicked = { picked -> onChange { it.copy(startDate = picked) } }
        )
    }
    if (showEnd) {
        DatePickerSheet(
            initial = form.endDate ?: form.startDate.plusMonths(12),
            onDismiss = { showEnd = false },
            onPicked = { picked -> onChange { it.copy(endDate = picked) } }
        )
    }
}
