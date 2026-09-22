@file:OptIn(ExperimentalMaterial3Api::class)

package com.hishab.finance.ui.more

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hishab.finance.core.mediumLabel
import com.hishab.finance.core.minutesLabel
import com.hishab.finance.core.startOfMonth
import com.hishab.finance.data.repository.ThemeMode
import com.hishab.finance.ui.components.DatePickerSheet
import com.hishab.finance.ui.components.DividerThin
import com.hishab.finance.ui.components.HishabCard
import com.hishab.finance.ui.components.SectionHeader
import com.hishab.finance.ui.rememberViewModel
import com.hishab.finance.ui.theme.HishabTheme
import java.time.LocalDate

/** Sections 15-22: appearance, notifications, data management, export and backup. */
@Composable
fun MoreScreen(
    onOpenSources: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenRecurring: () -> Unit
) {
    val context = LocalContext.current
    val vm: MoreViewModel = rememberViewModel { MoreViewModel(it, context) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val money = HishabTheme.money

    var exportFrom by remember { mutableStateOf(LocalDate.now().startOfMonth()) }
    var exportTo by remember { mutableStateOf(LocalDate.now()) }
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    var showHourPicker by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { vm.restore(it) }
    }

    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("More") }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HishabCard {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        MoreRow(Icons.Filled.Wallet, "Income sources", "${ui.counts.sources} sources", onOpenSources)
                        DividerThin(Modifier.padding(start = 60.dp))
                        MoreRow(Icons.Filled.Category, "Categories", "${ui.counts.categories} categories", onOpenCategories)
                        DividerThin(Modifier.padding(start = 60.dp))
                        MoreRow(Icons.Filled.PieChart, "Budgets", "This month's limits", onOpenBudgets)
                        DividerThin(Modifier.padding(start = 60.dp))
                        MoreRow(Icons.Filled.Repeat, "Recurring expenses", "${ui.counts.rules} rules", onOpenRecurring)
                    }
                }
            }

            item {
                HishabCard {
                    Column(Modifier.padding(16.dp)) {
                        SectionHeader("Appearance")
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            ThemeOption(ThemeMode.SYSTEM, "System", Icons.Filled.SettingsBrightness, settings.themeMode, 0, vm::setTheme)
                            ThemeOption(ThemeMode.LIGHT, "Light", Icons.Filled.LightMode, settings.themeMode, 1, vm::setTheme)
                            ThemeOption(ThemeMode.DARK, "Dark", Icons.Filled.DarkMode, settings.themeMode, 2, vm::setTheme)
                        }
                    }
                }
            }

            item {
                HishabCard {
                    Column(Modifier.padding(16.dp)) {
                        SectionHeader("Notifications", subtitle = "One daily check handles all of these")
                        SwitchRow("Daily entry reminder", "A nudge each evening to log today's spending", settings.dailyReminder, vm::setDailyReminder)
                        SwitchRow("Upcoming bill reminders", "A heads-up before a recurring expense is due", settings.billReminders, vm::setBillReminders)
                        SwitchRow("Budget alerts", "Warn me at 85% of a category limit and when I go over", settings.budgetAlerts, vm::setBudgetAlerts)
                        SwitchRow("Monthly report", "A summary notification on the 1st of each month", settings.monthlyReport, vm::setMonthlyReport)
                        SwitchRow("Backup reminder", "Occasional nudge to back up my data", settings.backupReminder, vm::setBackupReminder)
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(onClick = { showHourPicker = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Schedule, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Reminder time: ${minutesLabel(settings.reminderHour * 60)}")
                        }
                    }
                }
            }

            item {
                HishabCard {
                    Column(Modifier.padding(16.dp)) {
                        SectionHeader("Export", subtitle = "CSV opens in Excel; PDF is a printable report")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { showFromPicker = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(exportFrom.mediumLabel(), maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { showToPicker = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("to ${exportTo.mediumLabel()}", maxLines = 1)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { vm.exportCsv(exportFrom, exportTo) },
                                enabled = !ui.busy,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.CloudDownload, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Export CSV")
                            }
                            OutlinedButton(
                                onClick = { vm.exportPdf(exportFrom, exportTo) },
                                enabled = !ui.busy,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.CloudDownload, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Export PDF")
                            }
                        }
                    }
                }
            }

            item {
                HishabCard {
                    Column(Modifier.padding(16.dp)) {
                        SectionHeader("Backup and restore", subtitle = "A JSON file with everything you have recorded")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = vm::createBackup,
                                enabled = !ui.busy,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.CloudUpload, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Back up")
                            }
                            OutlinedButton(
                                onClick = { restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*")) },
                                enabled = !ui.busy,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Restore, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Restore")
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Restoring replaces everything currently in the app.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                HishabCard {
                    Column(Modifier.padding(16.dp)) {
                        SectionHeader(
                            "Your data",
                            subtitle = "${ui.counts.transactions} transactions recorded"
                        )
                        OutlinedButton(onClick = vm::loadDemoData, enabled = !ui.busy, modifier = Modifier.fillMaxWidth()) {
                            Text("Load sample data")
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { confirmClear = true },
                            enabled = !ui.busy,
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = money.expense)
                        ) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Clear all transactions")
                        }
                    }
                }
            }

            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Hishab", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Version 1.0.0 \u00B7 your data stays on this device",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (ui.busy) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Working\u2026", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showFromPicker) {
        DatePickerSheet(
            initial = exportFrom,
            onDismiss = { showFromPicker = false },
            onPicked = { picked -> exportFrom = picked; if (exportTo.isBefore(picked)) exportTo = picked }
        )
    }
    if (showToPicker) {
        DatePickerSheet(
            initial = exportTo,
            onDismiss = { showToPicker = false },
            onPicked = { picked -> exportTo = picked }
        )
    }
    if (showHourPicker) {
        com.hishab.finance.ui.components.TimePickerDialogSheet(
            initialMinutes = settings.reminderHour * 60,
            onDismiss = { showHourPicker = false },
            onPicked = { minutes -> vm.setReminderHour(minutes / 60) }
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all transactions?") },
            text = {
                Text(
                    "Every expense and income record is deleted from this device. Categories, " +
                        "sources, budgets and recurring rules stay. This cannot be undone \u2014 " +
                        "back up first if you might want this data again."
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; vm.clearAllData() }) {
                    Text("Clear everything", color = money.expense)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MoreRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.width(28.dp).height(28.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("\u203A", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SingleChoiceSegmentedButtonRowScope.ThemeOption(
    mode: ThemeMode,
    label: String,
    icon: ImageVector,
    current: ThemeMode,
    index: Int,
    onSelect: (ThemeMode) -> Unit
) {
    SegmentedButton(
        selected = current == mode,
        onClick = { onSelect(mode) },
        shape = SegmentedButtonDefaults.itemShape(index, 3)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.height(18.dp).width(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label)
    }
}
