package com.hishab.finance.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hishab.finance.core.fullLabel
import com.hishab.finance.core.longLabel
import com.hishab.finance.core.mediumLabel
import com.hishab.finance.core.percentChangeFrom
import com.hishab.finance.core.shortLabel
import com.hishab.finance.core.toCompactTaka
import com.hishab.finance.core.toTaka
import com.hishab.finance.core.toYearMonth
import com.hishab.finance.core.weekdayLabel
import com.hishab.finance.data.local.entity.TxType
import com.hishab.finance.ui.components.ChartSlice
import com.hishab.finance.ui.components.DonutChart
import com.hishab.finance.ui.components.DividerThin
import com.hishab.finance.ui.components.EmptyState
import com.hishab.finance.ui.components.HishabCard
import com.hishab.finance.ui.components.IconBubble
import com.hishab.finance.ui.components.LegendDot
import com.hishab.finance.ui.components.SectionHeader
import com.hishab.finance.ui.components.ShareBar
import com.hishab.finance.ui.components.StatTile
import com.hishab.finance.ui.components.TransactionRow
import com.hishab.finance.ui.components.TrendLineChart
import com.hishab.finance.ui.rememberViewModel
import com.hishab.finance.ui.theme.HishabTheme
import java.time.LocalDate

@Composable
fun DashboardScreen(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onOpenTransaction: (Long, TxType) -> Unit,
    onSeeAll: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenRecurring: () -> Unit
) {
    val vm: DashboardViewModel = rememberViewModel { DashboardViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val money = HishabTheme.money

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)
    ) {
        item {
            Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    state.today.fullLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Your money today", style = MaterialTheme.typography.headlineSmall)
            }
        }

        // --- Month hero ---
        item {
            val month = state.month
            Box(Modifier.padding(horizontal = 16.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(money.heroBrush)
                        .padding(20.dp)
                ) {
                    Text(
                        state.today.toYearMonth().longLabel(),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        (month?.net ?: 0.0).toTaka(),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White
                    )
                    Text(
                        "Balance this month",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth()) {
                        HeroFigure("Income", (month?.income ?: 0.0).toTaka(), Modifier.weight(1f))
                        HeroFigure("Expense", (month?.expense ?: 0.0).toTaka(), Modifier.weight(1f))
                        HeroFigure(
                            "Saved",
                            if ((month?.income ?: 0.0) > 0) "${month?.savingsRate?.toInt() ?: 0}%" else "\u2014",
                            Modifier.weight(1f)
                        )
                    }
                    state.previousMonth?.let { previous ->
                        val change = (month?.expense ?: 0.0).percentChangeFrom(previous.expense)
                        if (change != null) {
                            Spacer(Modifier.height(14.dp))
                            Text(
                                buildString {
                                    append("Spending is ")
                                    append(if (change >= 0) "up " else "down ")
                                    append("${kotlin.math.abs(change).toInt()}% ")
                                    append("against last month")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        // --- Today / week tiles ---
        item {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        label = "Today's expense",
                        value = (state.day?.expense ?: 0.0).toTaka(),
                        tint = money.expense,
                        emoji = "\uD83D\uDCB8",
                        caption = "${state.day?.expenseCount ?: 0} entries",
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Today's income",
                        value = (state.day?.income ?: 0.0).toTaka(),
                        tint = money.income,
                        emoji = "\uD83D\uDCB0",
                        caption = "${state.day?.incomeCount ?: 0} entries",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        label = "This week expense",
                        value = (state.week?.expense ?: 0.0).toTaka(),
                        tint = money.expense,
                        emoji = "\uD83D\uDCC5",
                        caption = "Avg ${(state.week?.avgDailyExpense ?: 0.0).toTaka()}/day",
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "This week income",
                        value = (state.week?.income ?: 0.0).toTaka(),
                        tint = money.income,
                        emoji = "\uD83C\uDFE6",
                        caption = "${state.week?.incomeCount ?: 0} payments",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- Forecast ---
        item {
            val forecast = state.forecast
            Box(Modifier.padding(horizontal = 16.dp)) {
                HishabCard {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBubble("\uD83D\uDD2E", money.forecast)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Next month's expenses", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    forecast?.month?.longLabel() ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        if (forecast == null || !forecast.isUsable) {
                            Text(
                                "Record a few weeks of spending and an estimate will appear here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                "${forecast.low.toTaka()} \u2013 ${forecast.high.toTaka()}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = money.forecast
                            )
                            Text(
                                "Most likely ${forecast.expected.toTaka()} \u2022 ${forecast.confidence.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Estimate based on your history, not a guaranteed amount.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = onOpenReports, modifier = Modifier.padding(top = 2.dp)) {
                                Text("See the full forecast")
                            }
                        }
                    }
                }
            }
        }

        // --- Budget warnings ---
        if (state.budgetWarnings.isNotEmpty()) {
            item {
                Box(Modifier.padding(16.dp)) {
                    HishabCard(onClick = onOpenBudgets) {
                        Column(Modifier.padding(16.dp)) {
                            SectionHeader(
                                title = "Budgets needing attention",
                                subtitle = "${state.budgetWarnings.size} of your limits are running hot"
                            )
                            state.budgetWarnings.take(3).forEach { progress ->
                                val over = progress.spent > progress.budget
                                val tint = if (over) money.expense else money.warning
                                Column(Modifier.padding(vertical = 6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${progress.category.icon}  ${progress.category.name}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            if (over) "${(-progress.remaining).toTaka()} over"
                                            else "${progress.remaining.toTaka()} left",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = tint
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    ShareBar(fraction = progress.ratio.coerceAtMost(1f), color = tint)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Upcoming bills ---
        if (state.upcomingBills.isNotEmpty()) {
            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    HishabCard(onClick = onOpenRecurring) {
                        Column(Modifier.padding(16.dp)) {
                            SectionHeader(title = "Coming up", subtitle = "Recurring expenses due soon")
                            state.upcomingBills.take(3).forEach { rule ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconBubble("\uD83D\uDD01", money.forecast, size = 34)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(rule.title, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            LocalDate.ofEpochDay(rule.nextDueEpochDay).mediumLabel(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(rule.amount.toTaka(), style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Spending mix + trend ---
        item {
            val month = state.month
            Box(Modifier.padding(16.dp)) {
                HishabCard {
                    Column(Modifier.padding(16.dp)) {
                        SectionHeader(
                            title = "Where the money went",
                            subtitle = state.today.toYearMonth().longLabel()
                        )
                        if (month == null || month.expenseByCategory.isEmpty()) {
                            EmptyState(
                                emoji = "\uD83E\uDDFE",
                                title = "No expenses yet this month",
                                message = "Add your first expense and the breakdown appears here.",
                                actionLabel = "Add expense",
                                onAction = onAddExpense
                            )
                        } else {
                            val slices = month.expenseByCategory.take(6).map {
                                ChartSlice(it.label, it.amount, Color(it.colorArgb))
                            }
                            DonutChart(
                                slices = slices,
                                centerValue = month.expense.toCompactTaka(),
                                centerLabel = "spent"
                            )
                            Spacer(Modifier.height(12.dp))
                            month.expenseByCategory.take(4).forEach { slice ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LegendDot(Color(slice.colorArgb), slice.label, Modifier.weight(1f))
                                    Text(
                                        "${slice.amount.toTaka()}  \u00b7  ${(slice.share * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            val week = state.week
            if (week != null && week.days.isNotEmpty()) {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    HishabCard {
                        Column(Modifier.padding(16.dp)) {
                            SectionHeader(
                                title = "This week's spending",
                                subtitle = "${week.from.shortLabel()} to ${week.to.shortLabel()}"
                            )
                            TrendLineChart(
                                values = week.days.map { it.expense },
                                labels = week.days.map { it.date.weekdayLabel() },
                                lineColor = money.expense
                            )
                        }
                    }
                }
            }
        }

        // --- Recent activity ---
        item {
            Box(Modifier.padding(16.dp)) {
                HishabCard {
                    Column(Modifier.padding(vertical = 14.dp)) {
                        SectionHeader(
                            title = "Recent activity",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            trailing = { TextButton(onClick = onSeeAll) { Text("See all") } }
                        )
                        if (state.recent.isEmpty()) {
                            EmptyState(
                                emoji = "\u2728",
                                title = "Nothing recorded yet",
                                message = "Your latest expenses and income will show up here.",
                                actionLabel = "Add income",
                                onAction = onAddIncome
                            )
                        } else {
                            state.recent.forEachIndexed { index, item ->
                                TransactionRow(
                                    item = item,
                                    showDate = true,
                                    onClick = { onOpenTransaction(item.tx.id, item.tx.type) }
                                )
                                if (index != state.recent.lastIndex) {
                                    DividerThin(Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroFigure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1
        )
    }
}
