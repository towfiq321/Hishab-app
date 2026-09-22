@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.hishab.finance.ui.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hishab.finance.core.TAKA
import com.hishab.finance.core.mediumLabel
import com.hishab.finance.core.minutesLabel
import com.hishab.finance.data.local.entity.PaymentMethod
import com.hishab.finance.data.local.entity.TxType
import com.hishab.finance.ui.components.DatePickerSheet
import com.hishab.finance.ui.components.HishabCard
import com.hishab.finance.ui.components.SectionHeader
import com.hishab.finance.ui.components.TimePickerDialogSheet
import com.hishab.finance.ui.rememberViewModel
import com.hishab.finance.ui.theme.HishabTheme

/**
 * One form for both kinds of transaction. Expense shows categories; income shows categories and
 * the income source it came from.
 */
@Composable
fun EntryScreen(
    initialType: TxType,
    transactionId: Long,
    onDone: () -> Unit,
    onManageSources: () -> Unit
) {
    val vm: EntryViewModel = rememberViewModel(key = "entry-$transactionId-${initialType.name}") {
        EntryViewModel(it, transactionId, initialType)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val sources by vm.sources.collectAsStateWithLifecycle()
    val money = HishabTheme.money

    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onDone()
    }

    val isIncome = state.type == TxType.INCOME
    val accent = money.directional(isIncome)
    val typedCategories = categories.filter { it.type == state.type && !it.isArchived }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopAppBar(
            title = {
                Text(
                    when {
                        state.isEditing && isIncome -> "Edit income"
                        state.isEditing -> "Edit expense"
                        isIncome -> "Add income"
                        else -> "Add expense"
                    }
                )
            },
            navigationIcon = {
                IconButton(onClick = onDone) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (state.isEditing) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = money.expense)
                    }
                }
            }
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Kind switch. Editing keeps the original kind to avoid orphaning a source link.
            if (!state.isEditing) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isIncome,
                        onClick = { vm.setType(TxType.EXPENSE) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Expense") }
                    SegmentedButton(
                        selected = isIncome,
                        onClick = { vm.setType(TxType.INCOME) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Income") }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Amount, given the space it deserves.
            HishabCard {
                Column(
                    Modifier.padding(vertical = 18.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (isIncome) "Amount received" else "Amount spent",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(TAKA, style = MaterialTheme.typography.headlineMedium, color = accent)
                        Spacer(Modifier.width(6.dp))
                        OutlinedTextField(
                            value = state.amountText,
                            onValueChange = vm::setAmount,
                            placeholder = {
                                Text(
                                    "0",
                                    style = MaterialTheme.typography.headlineMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                textAlign = TextAlign.Center,
                                color = accent
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            isError = state.error != null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (state.error != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            state.error ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // When
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { showDate = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(state.date.mediumLabel(), maxLines = 1)
                }
                OutlinedButton(
                    onClick = { showTime = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(minutesLabel(state.timeMinutes), maxLines = 1)
                }
            }

            Spacer(Modifier.height(18.dp))

            SectionHeader(if (isIncome) "Income category" else "Category")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                typedCategories.forEach { category ->
                    FilterChip(
                        selected = state.categoryId == category.id,
                        onClick = { vm.setCategory(category.id) },
                        label = { Text("${category.icon}  ${category.name}") }
                    )
                }
            }

            if (isIncome) {
                Spacer(Modifier.height(18.dp))
                SectionHeader(
                    "Income source",
                    subtitle = "Which person, company or project paid you",
                    trailing = { TextButton(onClick = onManageSources) { Text("Manage") } }
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.sourceId == null,
                        onClick = { vm.setSource(null) },
                        label = { Text("None") }
                    )
                    sources.filter { !it.isArchived }.forEach { source ->
                        FilterChip(
                            selected = state.sourceId == source.id,
                            onClick = { vm.setSource(source.id) },
                            label = { Text("${source.icon}  ${source.name}") }
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            SectionHeader(if (isIncome) "Received in" else "Paid with")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaymentMethod.entries.forEach { method ->
                    FilterChip(
                        selected = state.method == method,
                        onClick = { vm.setMethod(method) },
                        label = { Text(method.label) }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = state.description,
                onValueChange = vm::setDescription,
                label = { Text("Details") },
                placeholder = { Text(if (isIncome) "September salary" else "Lunch at office") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.note,
                onValueChange = vm::setNote,
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(Modifier.height(24.dp))
        }

        // Save bar pinned to the bottom so it is reachable one-handed.
        Box(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = vm::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White)
            ) {
                Text(
                    if (state.isEditing) "Save changes" else if (isIncome) "Add income" else "Add expense",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    if (showDate) {
        DatePickerSheet(
            initial = state.date,
            onDismiss = { showDate = false },
            onPicked = vm::setDate
        )
    }
    if (showTime) {
        TimePickerDialogSheet(
            initialMinutes = state.timeMinutes,
            onDismiss = { showTime = false },
            onPicked = vm::setTime
        )
    }
    if (confirmDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this transaction?") },
            text = { Text("It will be removed from every report and forecast. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete() }) {
                    Text("Delete", color = money.expense)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}
