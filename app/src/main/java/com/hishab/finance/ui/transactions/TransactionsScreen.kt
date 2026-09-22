@file:OptIn(ExperimentalMaterial3Api::class)

package com.hishab.finance.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hishab.finance.core.mediumLabel
import com.hishab.finance.core.relativeLabel
import com.hishab.finance.core.toTaka
import com.hishab.finance.data.local.entity.PaymentMethod
import com.hishab.finance.data.local.entity.TxType
import com.hishab.finance.ui.components.DividerThin
import com.hishab.finance.ui.components.EmptyState
import com.hishab.finance.ui.components.HishabCard
import com.hishab.finance.ui.components.DatePickerSheet
import com.hishab.finance.ui.components.TransactionRow
import com.hishab.finance.ui.rememberViewModel
import com.hishab.finance.ui.theme.HishabTheme
import java.time.LocalDate

@Composable
fun TransactionsScreen(onOpenTransaction: (Long, TxType) -> Unit) {
    val vm: TransactionsViewModel = rememberViewModel { TransactionsViewModel(it) }
    val items by vm.items.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val sources by vm.sources.collectAsStateWithLifecycle()
    val money = HishabTheme.money
    var showFilters by remember { mutableStateOf(false) }

    val income = items.filter { it.isIncome }.sumOf { it.tx.amount }
    val expense = items.filter { !it.isIncome }.sumOf { it.tx.amount }
    val grouped = items.groupBy { it.date }.toSortedMap(compareByDescending { it })

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("Transactions", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = filter.query,
                onValueChange = vm::setQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search description, category or source") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (filter.query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = filter.type == null,
                    onClick = { vm.setType(null) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = filter.type == TxType.EXPENSE,
                    onClick = { vm.setType(TxType.EXPENSE) },
                    label = { Text("Expense") }
                )
                FilterChip(
                    selected = filter.type == TxType.INCOME,
                    onClick = { vm.setType(TxType.INCOME) },
                    label = { Text("Income") }
                )
                AssistChip(
                    onClick = { showFilters = true },
                    label = { Text(if (filter.isActive) "Filters on" else "Filters") },
                    leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) }
                )
                if (filter.isActive) {
                    TextButton(onClick = vm::clearFilters) { Text("Reset") }
                }
            }
        }

        Box(Modifier.padding(horizontal = 16.dp)) {
            HishabCard {
                Row(Modifier.fillMaxWidth().padding(14.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Income",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(income.toTaka(), style = MaterialTheme.typography.titleMedium, color = money.income)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Expense",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(expense.toTaka(), style = MaterialTheme.typography.titleMedium, color = money.expense)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text((income - expense).toTaka(), style = MaterialTheme.typography.titleMedium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Entries",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("${items.size}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        if (items.isEmpty()) {
            EmptyState(
                emoji = "\uD83D\uDD0E",
                title = "Nothing matches",
                message = if (filter.isActive) "Try widening the filters or clearing the search."
                else "Add your first transaction with the + button.",
                actionLabel = if (filter.isActive) "Clear filters" else null,
                onAction = if (filter.isActive) vm::clearFilters else null
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 14.dp, bottom = 30.dp)
            ) {
                grouped.forEach { (date, dayItems) ->
                    item(key = "header-$date") {
                        val dayExpense = dayItems.filter { !it.isIncome }.sumOf { it.tx.amount }
                        val dayIncome = dayItems.filter { it.isIncome }.sumOf { it.tx.amount }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                date.relativeLabel(),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            if (dayIncome > 0) {
                                Text(
                                    "+${dayIncome.toTaka()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = money.income
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                            Text(
                                "-${dayExpense.toTaka()}",
                                style = MaterialTheme.typography.labelMedium,
                                color = money.expense
                            )
                        }
                    }
                    items(dayItems, key = { it.tx.id }) { item ->
                        HishabCard(
                            modifier = Modifier.padding(vertical = 3.dp),
                            onClick = { onOpenTransaction(item.tx.id, item.tx.type) }
                        ) {
                            TransactionRow(item = item)
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        FilterSheet(
            vm = vm,
            onDismiss = { showFilters = false },
            categories = categories.map { it.id to "${it.icon} ${it.name}" },
            sources = sources.map { it.id to "${it.icon} ${it.name}" }
        )
    }
}

@Composable
private fun FilterSheet(
    vm: TransactionsViewModel,
    onDismiss: () -> Unit,
    categories: List<Pair<Long, String>>,
    sources: List<Pair<Long, String>>
) {
    val filter by vm.filter.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var minText by remember { mutableStateOf(filter.minAmount?.toInt()?.toString() ?: "") }
    var maxText by remember { mutableStateOf(filter.maxAmount?.toInt()?.toString() ?: "") }
    var pickingFrom by remember { mutableStateOf(false) }
    var pickingTo by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 30.dp)
        ) {
            Text("Filter transactions", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            Text("Date range", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AssistChip(
                    onClick = { pickingFrom = true },
                    label = { Text(filter.from?.mediumLabel() ?: "From any date") }
                )
                AssistChip(
                    onClick = { pickingTo = true },
                    label = { Text(filter.to?.mediumLabel() ?: "To any date") }
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val today = LocalDate.now()
                TextButton(onClick = { vm.setRange(today, today) }) { Text("Today") }
                TextButton(onClick = { vm.setRange(today.minusDays(6), today) }) { Text("Last 7 days") }
                TextButton(onClick = { vm.setRange(today.withDayOfMonth(1), today) }) { Text("This month") }
            }

            Spacer(Modifier.height(14.dp))
            Text("Amount range (\u09F3)", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = minText,
                    onValueChange = {
                        minText = it.filter { c -> c.isDigit() }
                        vm.setAmountRange(minText.toDoubleOrNull(), maxText.toDoubleOrNull())
                    },
                    label = { Text("Minimum") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = maxText,
                    onValueChange = {
                        maxText = it.filter { c -> c.isDigit() }
                        vm.setAmountRange(minText.toDoubleOrNull(), maxText.toDoubleOrNull())
                    },
                    label = { Text("Maximum") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("Payment method", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMethod.entries.forEach { method ->
                    FilterChip(
                        selected = method in filter.methods,
                        onClick = { vm.toggleMethod(method) },
                        label = { Text(method.label) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Category", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            ChipFlow(categories, filter.categoryIds, vm::toggleCategory)

            Spacer(Modifier.height(16.dp))
            Text("Income source", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            ChipFlow(sources, filter.sourceIds, vm::toggleSource)

            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = { vm.clearFilters(); minText = ""; maxText = "" }) { Text("Clear all") }
                Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Show results") }
            }
        }
    }

    if (pickingFrom) {
        DatePickerSheet(
            initial = filter.from ?: LocalDate.now(),
            onDismiss = { pickingFrom = false },
            onPicked = { vm.setRange(it, filter.to) }
        )
    }
    if (pickingTo) {
        DatePickerSheet(
            initial = filter.to ?: LocalDate.now(),
            onDismiss = { pickingTo = false },
            onPicked = { vm.setRange(filter.from, it) }
        )
    }
}

@Composable
private fun ChipFlow(
    options: List<Pair<Long, String>>,
    selected: Set<Long>,
    onToggle: (Long) -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (id, label) ->
            FilterChip(
                selected = id in selected,
                onClick = { onToggle(id) },
                label = { Text(label) }
            )
        }
    }
}
