@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.hishab.finance.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hishab.finance.core.TAKA
import com.hishab.finance.core.longLabel
import com.hishab.finance.core.toTaka
import com.hishab.finance.data.local.entity.CategoryEntity
import com.hishab.finance.data.repository.BudgetProgress
import com.hishab.finance.data.repository.BudgetState
import com.hishab.finance.ui.components.EmptyState
import com.hishab.finance.ui.components.HishabCard
import com.hishab.finance.ui.components.IconBubble
import com.hishab.finance.ui.components.MonthStepper
import com.hishab.finance.ui.components.SectionHeader
import com.hishab.finance.ui.components.ShareBar
import com.hishab.finance.ui.components.StatTile
import com.hishab.finance.ui.rememberViewModel
import com.hishab.finance.ui.theme.HishabTheme

/** Section 13: monthly category limits with progress and warnings. */
@Composable
fun BudgetScreen(onBack: () -> Unit) {
    val vm: BudgetViewModel = rememberViewModel { BudgetViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val money = HishabTheme.money
    val snackbar = remember { SnackbarHostState() }

    // category being edited, plus the amount currently in the dialog
    var editing by remember { mutableStateOf<Pair<CategoryEntity, Double>?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = vm::copyFromPreviousMonth) { Text("Copy last month") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MonthStepper(
                    month = state.month,
                    label = state.month.longLabel(),
                    onPrevious = vm::previousMonth,
                    onNext = vm::nextMonth
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        label = "Planned",
                        value = state.planned.toTaka(),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Used",
                        value = state.spent.toTaka(),
                        tint = money.expense,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Remaining",
                        value = state.remaining.toTaka(),
                        tint = if (state.remaining >= 0) money.safe else money.expense,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (state.lines.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "\uD83C\uDFAF",
                        title = "No budget for ${state.month.longLabel()}",
                        message = "Pick a category below and set a limit. Hishab warns you at 85% " +
                            "and again when you go over."
                    )
                }
            }

            items(state.lines, key = { it.category.id }) { line ->
                BudgetLineCard(
                    line = line,
                    onEdit = { editing = line.category to line.budget },
                    onRemove = { vm.removeBudget(line.category.id) }
                )
            }

            if (state.unbudgeted.isNotEmpty()) {
                item {
                    HishabCard {
                        Column(Modifier.padding(14.dp)) {
                            SectionHeader(
                                "Add a limit",
                                subtitle = "Categories with no budget this month"
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.unbudgeted.forEach { category ->
                                    SuggestionChip(
                                        onClick = { editing = category to 0.0 },
                                        label = { Text("${category.icon}  ${category.name}") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { (category, current) ->
        BudgetAmountDialog(
            category = category,
            initial = current,
            onDismiss = { editing = null },
            onConfirm = { amount ->
                vm.setBudget(category.id, amount)
                editing = null
            }
        )
    }
}

@Composable
private fun BudgetLineCard(line: BudgetProgress, onEdit: () -> Unit, onRemove: () -> Unit) {
    val money = HishabTheme.money
    val barColor = when (line.state) {
        BudgetState.OVER -> money.expense
        BudgetState.NEAR -> money.warning
        else -> money.safe
    }
    HishabCard(onClick = onEdit) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(line.category.icon, androidx.compose.ui.graphics.Color(line.category.colorArgb))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(line.category.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Limit ${line.budget.toTaka()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    String.format(java.util.Locale.US, "%.0f%%", line.ratio * 100),
                    style = MaterialTheme.typography.titleSmall,
                    color = barColor
                )
            }
            Spacer(Modifier.height(10.dp))
            ShareBar(fraction = line.ratio.coerceIn(0f, 1f), color = barColor)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Used ${line.spent.toTaka()}  \u00B7  " +
                        if (line.remaining >= 0) "Remaining ${line.remaining.toTaka()}"
                        else "Over by ${(-line.remaining).toTaka()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (line.remaining >= 0) MaterialTheme.colorScheme.onSurfaceVariant else money.expense,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onRemove) { Text("Remove") }
            }
            if (line.state == BudgetState.NEAR || line.state == BudgetState.OVER) {
                Text(
                    if (line.state == BudgetState.OVER)
                        "\u26A0 You have passed this limit."
                    else "\u26A0 You are close to this limit.",
                    style = MaterialTheme.typography.labelMedium,
                    color = barColor
                )
            }
        }
    }
}

@Composable
private fun BudgetAmountDialog(
    category: CategoryEntity,
    initial: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var text by remember {
        mutableStateOf(if (initial > 0) initial.toLong().toString() else "")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${category.icon}  ${category.name}") },
        text = {
            Column {
                Text(
                    "How much do you want to allow for this category this month?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { value -> text = value.filter { it.isDigit() || it == '.' } },
                    prefix = { Text(TAKA) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.toDoubleOrNull() ?: 0.0) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
