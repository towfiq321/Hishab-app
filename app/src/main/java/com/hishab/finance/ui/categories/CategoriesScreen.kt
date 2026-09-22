@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.hishab.finance.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hishab.finance.data.local.entity.CategoryEntity
import com.hishab.finance.data.local.entity.TxType
import com.hishab.finance.ui.components.HishabCard
import com.hishab.finance.ui.components.IconBubble
import com.hishab.finance.ui.components.SectionHeader
import com.hishab.finance.ui.rememberViewModel

/** Sections 9 and 10: customisable expense and income categories. */
@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val vm: CategoriesViewModel = rememberViewModel { CategoriesViewModel(it) }
    val categories by vm.categories.collectAsStateWithLifecycle()
    val form by vm.form.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var tab by remember { mutableIntStateOf(0) }
    var confirmDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    val type = if (tab == 0) TxType.EXPENSE else TxType.INCOME
    val shown = categories.filter { it.type == type }.sortedBy { it.sortOrder }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.startNew(type) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (type == TxType.EXPENSE) "New expense category" else "New income category") }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Expense") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Income") })
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(shown, key = { it.id }) { category ->
                    HishabCard(onClick = { vm.startEdit(category) }) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconBubble(category.icon, Color(category.colorArgb))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(category.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    if (category.isDefault) "Built in" else "Custom",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { confirmDelete = category }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    form?.let { current ->
        CategoryEditorSheet(
            form = current,
            onDismiss = vm::cancelEdit,
            onChange = vm::update,
            onSave = vm::save
        )
    }

    confirmDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete \"${category.name}\"?") },
            text = {
                Text(
                    "Your transactions are never deleted. Any that used this category become " +
                        "uncategorised, and its budgets and recurring rules are removed."
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.delete(category); confirmDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CategoryEditorSheet(
    form: CategoryFormState,
    onDismiss: () -> Unit,
    onChange: ((CategoryFormState) -> CategoryFormState) -> Unit,
    onSave: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                if (form.isEditing) "Edit category" else "New category",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (form.type == TxType.EXPENSE) "Expense category" else "Income category",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = form.name,
                onValueChange = { value -> onChange { it.copy(name = value, error = null) } },
                label = { Text("Name") },
                singleLine = true,
                isError = form.error != null,
                supportingText = form.error?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))
            SectionHeader("Icon")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CategoriesViewModel.ICONS.forEach { icon ->
                    val selected = form.icon == icon
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) Color(form.colorArgb).copy(alpha = 0.22f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onChange { it.copy(icon = icon) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(icon, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionHeader("Colour")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CategoriesViewModel.COLORS.forEach { argb ->
                    val selected = form.colorArgb == argb
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape
                            )
                            .clickable { onChange { it.copy(colorArgb = argb) } }
                    )
                }
            }

            if (form.isEditing && form.usageCount > 0) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Used by ${form.usageCount} transaction${if (form.usageCount == 1) "" else "s"}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onSave,
                enabled = form.canSave,
                modifier = Modifier.fillMaxWidth().height(50.dp).navigationBarsPadding(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (form.isEditing) "Save changes" else "Add category")
            }
        }
    }
}
