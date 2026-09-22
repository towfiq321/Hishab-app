@file:OptIn(ExperimentalMaterial3Api::class)

package com.hishab.finance.ui.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hishab.finance.core.mediumLabel
import com.hishab.finance.core.shortLabel
import com.hishab.finance.core.toCompactTaka
import com.hishab.finance.core.toTaka
import com.hishab.finance.data.local.entity.IncomeSourceEntity
import com.hishab.finance.ui.components.BarGroup
import com.hishab.finance.ui.components.DividerThin
import com.hishab.finance.ui.components.EmptyState
import com.hishab.finance.ui.components.GroupedBarChart
import com.hishab.finance.ui.components.HishabCard
import com.hishab.finance.ui.components.IconBubble
import com.hishab.finance.ui.components.SectionHeader
import com.hishab.finance.ui.components.ShareBar
import com.hishab.finance.ui.components.StatTile
import com.hishab.finance.ui.components.TransactionRow
import com.hishab.finance.ui.rememberViewModel
import com.hishab.finance.ui.theme.HishabTheme

/** Section 7: every person, company or project that pays, and what they have actually paid. */
@Composable
fun SourcesScreen(
    onBack: () -> Unit,
    onOpenSource: (Long) -> Unit,
    onAddSource: () -> Unit
) {
    val vm: SourcesViewModel = rememberViewModel { SourcesViewModel(it) }
    val stats by vm.stats.collectAsStateWithLifecycle()
    val unassigned by vm.unassigned.collectAsStateWithLifecycle()
    val money = HishabTheme.money
    val grandTotal = stats.sumOf { it.total } + unassigned

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Income sources") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddSource,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New source") }
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
                    label = "Income recorded from all sources",
                    value = grandTotal.toTaka(),
                    tint = money.income,
                    caption = "${stats.size} sources" +
                        if (unassigned > 0) " \u00B7 ${unassigned.toCompactTaka()} unassigned" else "",
                    emoji = "\uD83D\uDCBC"
                )
            }

            if (stats.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "\uD83D\uDCBC",
                        title = "No income sources yet",
                        message = "Add the people, companies and projects that pay you, and Hishab " +
                            "will keep a running total for each one.",
                        actionLabel = "Add a source",
                        onAction = onAddSource
                    )
                }
            }

            items(stats, key = { it.source.id }) { line ->
                SourceCard(
                    stats = line,
                    share = if (grandTotal <= 0) 0f else (line.total / grandTotal).toFloat(),
                    onClick = { onOpenSource(line.source.id) }
                )
            }
        }
    }
}

@Composable
private fun SourceCard(stats: SourceStats, share: Float, onClick: () -> Unit) {
    val money = HishabTheme.money
    val color = Color(stats.source.colorArgb)
    HishabCard(onClick = onClick) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(stats.source.icon, color)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stats.source.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${stats.source.sourceType} \u00B7 ${stats.source.payFrequency.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    stats.total.toTaka(),
                    style = MaterialTheme.typography.titleMedium,
                    color = money.income,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(10.dp))
            ShareBar(fraction = share, color = color)
            Spacer(Modifier.height(8.dp))
            Text(
                buildString {
                    append("${stats.count} payments")
                    if (stats.count > 0) append(" \u00B7 avg ${stats.average.toCompactTaka()}")
                    stats.lastDate?.let { append(" \u00B7 last ${it.mediumLabel()}") }
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* ---------------------------------------------------------------------------------------- detail */

@Composable
fun SourceDetailScreen(
    sourceId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenTransaction: (Long) -> Unit
) {
    val vm: SourceDetailViewModel = rememberViewModel(key = "source-$sourceId") {
        SourceDetailViewModel(it, sourceId)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val money = HishabTheme.money
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.source?.name ?: "Income source") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = money.expense)
                    }
                }
            )
        }
    ) { padding ->
        val source = state.source
        val stats = state.stats
        if (source == null || stats == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (!state.loading) {
                    EmptyState(
                        emoji = "\uD83D\uDD0D",
                        title = "Source not found",
                        message = "It may have been deleted."
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(money.heroBrush)
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            "Total income received",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            stats.total.toTaka(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "${stats.count} payments \u00B7 ${stats.thisYear.toCompactTaka()} this year",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        label = "Average payment",
                        value = stats.average.toTaka(),
                        tint = money.income,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Last payment",
                        value = stats.lastAmount?.toTaka() ?: "\u2014",
                        tint = money.forecast,
                        caption = stats.lastDate?.mediumLabel(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (state.monthly.any { it.second > 0 }) {
                item {
                    HishabCard {
                        Column(Modifier.padding(14.dp)) {
                            SectionHeader("Last six months")
                            GroupedBarChart(
                                groups = state.monthly.map { (ym, amount) ->
                                    BarGroup(ym.shortLabel(), amount, 0.0)
                                },
                                incomeColor = money.income,
                                expenseColor = Color.Transparent,
                                height = 150
                            )
                        }
                    }
                }
            }

            item { SourceDetailsCard(source) }

            item {
                SectionHeader(
                    "Payment history",
                    subtitle = "${state.transactions.size} transactions",
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (state.transactions.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "\uD83E\uDDFE",
                        title = "No payments recorded",
                        message = "Add an income transaction and pick this source."
                    )
                }
            } else {
                item {
                    HishabCard {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            state.transactions.forEachIndexed { index, item ->
                                if (index > 0) DividerThin(Modifier.padding(start = 66.dp))
                                TransactionRow(
                                    item = item,
                                    showDate = true,
                                    onClick = { onOpenTransaction(item.tx.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this source?") },
            text = {
                Text(
                    "The income you recorded stays. Those transactions will simply no longer be " +
                        "linked to a source."
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete() }) {
                    Text("Delete", color = HishabTheme.money.expense)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SourceDetailsCard(source: IncomeSourceEntity) {
    HishabCard {
        Column(Modifier.padding(14.dp)) {
            SectionHeader("Details")
            DetailLine("Type", source.sourceType)
            DetailLine("Expected frequency", source.payFrequency.label)
            DetailLine(
                "Typical amount",
                if (source.typicalAmount > 0) source.typicalAmount.toTaka() else null
            )
            DetailLine("Contact", source.contactName.ifBlank { null })
            DetailLine("Phone", source.contactPhone.ifBlank { null })
            DetailLine("Email", source.email.ifBlank { null })
            DetailLine("Address", source.address.ifBlank { null })
            DetailLine("Description", source.description.ifBlank { null })
            DetailLine("Notes", source.notes.ifBlank { null })
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(130.dp)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}
