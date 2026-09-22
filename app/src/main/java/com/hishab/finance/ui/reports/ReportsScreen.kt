@file:OptIn(ExperimentalMaterial3Api::class)

package com.hishab.finance.ui.reports

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hishab.finance.core.longLabel
import com.hishab.finance.core.mediumLabel
import com.hishab.finance.core.percentChangeFrom
import com.hishab.finance.core.rangeLabel
import com.hishab.finance.core.shortLabel
import com.hishab.finance.core.toCompactTaka
import com.hishab.finance.core.toTaka
import com.hishab.finance.core.toYearMonth
import com.hishab.finance.core.weekdayLabel
import com.hishab.finance.domain.BreakdownSlice
import com.hishab.finance.domain.Confidence
import com.hishab.finance.domain.ExpenseForecast
import com.hishab.finance.domain.PeriodSummary
import com.hishab.finance.ui.components.BarGroup
import com.hishab.finance.ui.components.BreakdownRow
import com.hishab.finance.ui.components.ChartSlice
import com.hishab.finance.ui.components.DividerThin
import com.hishab.finance.ui.components.DonutChart
import com.hishab.finance.ui.components.EmptyState
import com.hishab.finance.ui.components.ForecastAccuracyChart
import com.hishab.finance.ui.components.GroupedBarChart
import com.hishab.finance.ui.components.HishabCard
import com.hishab.finance.ui.components.LegendDot
import com.hishab.finance.ui.components.MonthStepper
import com.hishab.finance.ui.components.SectionHeader
import com.hishab.finance.ui.components.ShareBar
import com.hishab.finance.ui.components.StatTile
import com.hishab.finance.ui.components.TrendLineChart
import com.hishab.finance.ui.rememberViewModel
import com.hishab.finance.ui.theme.HishabTheme

private val TAB_TITLES = listOf("Weekly", "Monthly", "Forecast")

@Composable
fun ReportsScreen(onOpenBudgets: () -> Unit) {
    val vm: ReportsViewModel = rememberViewModel { ReportsViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("Reports", style = MaterialTheme.typography.headlineSmall)
        }
        TabRow(selectedTabIndex = tab) {
            TAB_TITLES.forEachIndexed { index, title ->
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    text = { Text(title) }
                )
            }
        }
        when (tab) {
            0 -> WeeklyReport(state, vm)
            1 -> MonthlyReport(state, vm, onOpenBudgets)
            else -> ForecastReport(state)
        }
    }
}

/* --------------------------------------------------------------------------------------- weekly */

@Composable
private fun WeeklyReport(state: ReportsState, vm: ReportsViewModel) {
    val week = state.week
    val money = HishabTheme.money

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            MonthStepper(
                month = state.weekStart.toYearMonth(),
                label = rangeLabel(state.weekStart, state.weekStart.plusDays(6)),
                onPrevious = vm::previousWeek,
                onNext = vm::nextWeek,
                nextEnabled = !state.isCurrentWeek
            )
        }

        if (week == null || week.isEmpty) {
            item {
                EmptyState(
                    emoji = "\uD83D\uDCC5",
                    title = "Nothing recorded this week",
                    message = "Add a few transactions and the weekly report fills in automatically."
                )
            }
            return@LazyColumn
        }

        item { TotalsRow(week) }
        item {
            ComparisonCard(
                title = "Compared with last week",
                current = week,
                previous = state.previousWeek
            )
        }
        item { AveragesCard(week, "day") }

        item {
            HishabCard {
                Column(Modifier.padding(14.dp)) {
                    SectionHeader("Income vs expense", subtitle = "Day by day")
                    GroupedBarChart(
                        groups = week.days.map {
                            BarGroup(it.date.weekdayLabel(), it.income, it.expense)
                        },
                        incomeColor = money.income,
                        expenseColor = money.expense
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendDot(money.income, "Income")
                        LegendDot(money.expense, "Expense")
                    }
                }
            }
        }

        item {
            HishabCard {
                Column(Modifier.padding(14.dp)) {
                    SectionHeader("Daily spending trend")
                    TrendLineChart(
                        values = week.days.map { it.expense },
                        labels = week.days.map { it.date.weekdayLabel() },
                        lineColor = money.expense
                    )
                }
            }
        }

        item { HighlightsCard(week) }

        if (week.expenseByCategory.isNotEmpty()) {
            item { BreakdownCard("Where the money went", week.expenseByCategory, week.expense, "expense") }
        }
        if (week.incomeBySource.isNotEmpty()) {
            item { BreakdownCard("Income by source", week.incomeBySource, week.income, "income") }
        }
    }
}

/* -------------------------------------------------------------------------------------- monthly */

@Composable
private fun MonthlyReport(state: ReportsState, vm: ReportsViewModel, onOpenBudgets: () -> Unit) {
    val month = state.monthSummary
    val money = HishabTheme.money

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            MonthStepper(
                month = state.month,
                label = state.month.longLabel(),
                onPrevious = vm::previousMonth,
                onNext = vm::nextMonth,
                nextEnabled = !state.isCurrentMonth
            )
        }

        if (month == null || month.isEmpty) {
            item {
                EmptyState(
                    emoji = "\uD83D\uDCCA",
                    title = "No activity in ${state.month.longLabel()}",
                    message = "Record income and expenses to build the monthly picture."
                )
            }
            return@LazyColumn
        }

        item { TotalsRow(month) }
        item {
            ComparisonCard(
                title = "Compared with ${state.month.minusMonths(1).longLabel()}",
                current = month,
                previous = state.previousMonth
            )
        }
        item { AveragesCard(month, "day") }
        item { HighlightsCard(month) }

        item {
            HishabCard {
                Column(Modifier.padding(14.dp)) {
                    SectionHeader("Six month trend", subtitle = "Income beside expense")
                    GroupedBarChart(
                        groups = state.monthTrend.map {
                            BarGroup(it.month.shortLabel(), it.income, it.expense)
                        },
                        incomeColor = money.income,
                        expenseColor = money.expense
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendDot(money.income, "Income")
                        LegendDot(money.expense, "Expense")
                    }
                }
            }
        }

        item {
            HishabCard {
                Column(Modifier.padding(14.dp)) {
                    SectionHeader("Spending through the month")
                    TrendLineChart(
                        values = month.days.map { it.expense },
                        labels = listOf(
                            month.from.mediumLabel(),
                            month.to.mediumLabel()
                        ),
                        lineColor = money.expense
                    )
                }
            }
        }

        if (month.expenseByCategory.isNotEmpty()) {
            item { BreakdownCard("Expense by category", month.expenseByCategory, month.expense, "expense") }
        }
        if (month.incomeBySource.isNotEmpty()) {
            item { BreakdownCard("Income by source", month.incomeBySource, month.income, "income") }
        }
        if (month.expenseByMethod.isNotEmpty()) {
            item {
                HishabCard {
                    Column(Modifier.padding(vertical = 10.dp)) {
                        SectionHeader("Payment methods", modifier = Modifier.padding(horizontal = 14.dp))
                        month.expenseByMethod.forEach { slice ->
                            BreakdownRow(
                                emoji = slice.icon,
                                label = slice.label,
                                amount = slice.amount,
                                share = slice.share,
                                color = Color(slice.colorArgb),
                                caption = "${slice.count} payments"
                            )
                        }
                    }
                }
            }
        }

        if (state.budgets.isNotEmpty()) {
            item {
                HishabCard {
                    Column(Modifier.padding(14.dp)) {
                        SectionHeader(
                            "Budget progress",
                            subtitle = "For ${state.month.longLabel()}",
                            trailing = { TextButton(onClick = onOpenBudgets) { Text("Manage") } }
                        )
                        state.budgets.take(5).forEach { line ->
                            Column(Modifier.padding(vertical = 6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${line.category.icon}  ${line.category.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "${line.spent.toTaka()} / ${line.budget.toTaka()}",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                ShareBar(
                                    fraction = line.ratio.coerceIn(0f, 1f),
                                    color = when {
                                        line.ratio > 1f -> money.expense
                                        line.ratio >= 0.85f -> money.warning
                                        else -> money.safe
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            item {
                HishabCard(onClick = onOpenBudgets) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Set a monthly budget", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Track category limits and get a warning before you go over.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("\u203A", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------------------------- forecast */

@Composable
private fun ForecastReport(state: ReportsState) {
    val money = HishabTheme.money
    val forecast = state.forecast

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (forecast == null) {
            item {
                EmptyState(
                    emoji = "\uD83D\uDD2E",
                    title = "Building your forecast",
                    message = "Record expenses for a few weeks and an estimate will appear here."
                )
            }
            return@LazyColumn
        }

        item { ForecastHeadline(forecast) }

        state.projection?.let { projection ->
            item {
                HishabCard {
                    Column(Modifier.padding(16.dp)) {
                        SectionHeader(
                            "This month so far",
                            subtitle = "${projection.daysElapsed} days in, ${projection.daysRemaining} to go"
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Spent",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    projection.spentSoFar.toTaka(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = money.expense
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "On track to finish at",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    projection.projectedTotal.toTaka(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = money.forecast
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        ShareBar(
                            fraction = if (projection.projectedTotal <= 0) 0f
                            else (projection.spentSoFar / projection.projectedTotal).toFloat(),
                            color = money.forecast
                        )
                    }
                }
            }
        }

        if (forecast.categories.isNotEmpty()) {
            item {
                HishabCard {
                    Column(Modifier.padding(vertical = 12.dp)) {
                        SectionHeader(
                            "Estimated by category",
                            subtitle = "Recurring bills are priced exactly; the rest is your average",
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                        forecast.categories.forEach { line ->
                            BreakdownRow(
                                emoji = line.icon,
                                label = line.label,
                                amount = line.amount,
                                share = if (forecast.expected <= 0) 0f
                                else (line.amount / forecast.expected).toFloat(),
                                color = Color(line.colorArgb),
                                caption = if (line.recurringPart > 0)
                                    "includes ${line.recurringPart.toTaka()} of recurring bills" else null
                            )
                        }
                    }
                }
            }
        }

        if (state.accuracy.isNotEmpty()) {
            item {
                HishabCard {
                    Column(Modifier.padding(14.dp)) {
                        SectionHeader(
                            "Forecast vs actual",
                            subtitle = "How the estimate has held up"
                        )
                        ForecastAccuracyChart(
                            labels = state.accuracy.map { it.month.shortLabel() },
                            forecast = state.accuracy.map { it.forecast },
                            actual = state.accuracy.map { it.actual },
                            forecastColor = money.forecast,
                            actualColor = money.expense
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LegendDot(money.forecast, "Forecast")
                            LegendDot(money.expense, "Actual")
                        }
                        Spacer(Modifier.height(12.dp))
                        DividerThin()
                        state.accuracy.forEach { point ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    point.month.longLabel(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "${point.forecast.toCompactTaka()} \u2192 ${point.actual.toCompactTaka()}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(Modifier.width(10.dp))
                                val error = point.errorPercent
                                Text(
                                    if (error == null) "\u2014" else String.format(
                                        java.util.Locale.US, "%+.0f%%", error
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = when {
                                        error == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                        kotlin.math.abs(error) <= 10 -> money.safe
                                        else -> money.warning
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            HishabCard(background = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.padding(16.dp)) {
                    Text("Why there is no income forecast", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Hishab estimates expenses only. Income here comes from salary, projects and " +
                            "one-off work, and a predicted income figure is a dangerous number to plan " +
                            "against. Expenses repeat; income does not.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ForecastHeadline(forecast: ExpenseForecast) {
    val money = HishabTheme.money
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(money.heroBrush)
            .padding(20.dp)
    ) {
        Column {
            Text(
                "Estimated expense for ${forecast.month.longLabel()}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${forecast.low.toTaka()} \u2013 ${forecast.high.toTaka()}",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Most likely ${forecast.expected.toTaka()}",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.92f)
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        when (forecast.confidence) {
                            Confidence.HIGH -> "\u2714 ${forecast.confidence.label}"
                            Confidence.MEDIUM -> "\u25CF ${forecast.confidence.label}"
                            Confidence.LOW -> "\u26A0 ${forecast.confidence.label}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
                Spacer(Modifier.width(10.dp))
                if (forecast.recurringTotal > 0) {
                    Text(
                        "${forecast.recurringTotal.toCompactTaka()} recurring",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                forecast.basis,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "This is an estimate from your own history, not a guaranteed amount.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}

/* -------------------------------------------------------------------------------- shared pieces */

@Composable
private fun TotalsRow(summary: PeriodSummary) {
    val money = HishabTheme.money
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = "Income",
                value = summary.income.toTaka(),
                tint = money.income,
                caption = "${summary.incomeCount} transactions",
                emoji = "\uD83D\uDCB0",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Expense",
                value = summary.expense.toTaka(),
                tint = money.expense,
                caption = "${summary.expenseCount} transactions",
                emoji = "\uD83D\uDCB8",
                modifier = Modifier.weight(1f)
            )
        }
        StatTile(
            label = "Net balance",
            value = summary.net.toTaka(),
            tint = if (summary.net >= 0) money.safe else money.expense,
            caption = if (summary.income > 0)
                String.format(java.util.Locale.US, "Savings rate %.0f%%", summary.savingsRate)
            else "Income minus expense",
            emoji = "\u2696\uFE0F"
        )
    }
}

@Composable
private fun AveragesCard(summary: PeriodSummary, unit: String) {
    val money = HishabTheme.money
    HishabCard {
        Column(Modifier.padding(14.dp)) {
            SectionHeader("Daily averages", subtitle = "Across ${summary.elapsedDays} ${unit}s so far")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Average income",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        summary.avgDailyIncome.toTaka(),
                        style = MaterialTheme.typography.titleMedium,
                        color = money.income
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Average expense",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        summary.avgDailyExpense.toTaka(),
                        style = MaterialTheme.typography.titleMedium,
                        color = money.expense
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightsCard(summary: PeriodSummary) {
    HishabCard {
        Column(Modifier.padding(14.dp)) {
            SectionHeader("Highlights")
            HighlightRow(
                "Highest spending day",
                summary.topExpenseDay?.let { "${it.date.mediumLabel()} \u00B7 ${it.expense.toTaka()}" }
            )
            HighlightRow(
                "Lowest spending day",
                summary.lowestExpenseDay?.let { "${it.date.mediumLabel()} \u00B7 ${it.expense.toTaka()}" }
            )
            HighlightRow(
                "Highest income day",
                summary.topIncomeDay?.let { "${it.date.mediumLabel()} \u00B7 ${it.income.toTaka()}" }
            )
            HighlightRow(
                "Top spending category",
                summary.topExpenseCategory?.let { "${it.icon} ${it.label} \u00B7 ${it.amount.toTaka()}" }
            )
            HighlightRow(
                "Top income source",
                summary.topIncomeSource?.let { "${it.icon} ${it.label} \u00B7 ${it.amount.toTaka()}" }
            )
        }
    }
}

@Composable
private fun HighlightRow(label: String, value: String?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            value ?: "\u2014",
            style = MaterialTheme.typography.labelLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun ComparisonCard(title: String, current: PeriodSummary, previous: PeriodSummary?) {
    val money = HishabTheme.money
    HishabCard {
        Column(Modifier.padding(14.dp)) {
            SectionHeader(title)
            if (previous == null || previous.isEmpty) {
                Text(
                    "No data for the previous period yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ChangeRow("Income", current.income, previous.income, higherIsBetter = true, money.income)
                ChangeRow("Expense", current.expense, previous.expense, higherIsBetter = false, money.expense)
                ChangeRow("Savings", current.net, previous.net, higherIsBetter = true, money.safe)
            }
        }
    }
}

@Composable
private fun ChangeRow(
    label: String,
    current: Double,
    previous: Double,
    higherIsBetter: Boolean,
    tint: Color
) {
    val money = HishabTheme.money
    val change = current.percentChangeFrom(previous)
    val improved = if (higherIsBetter) current >= previous else current <= previous
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(current.toTaka(), style = MaterialTheme.typography.labelLarge, color = tint)
        Spacer(Modifier.width(10.dp))
        Text(
            when {
                change == null -> "new"
                else -> String.format(java.util.Locale.US, "%+.0f%%", change)
            },
            style = MaterialTheme.typography.labelLarge,
            color = if (improved) money.safe else money.warning
        )
    }
}

@Composable
private fun BreakdownCard(
    title: String,
    slices: List<BreakdownSlice>,
    total: Double,
    kind: String
) {
    HishabCard {
        Column(Modifier.padding(vertical = 12.dp)) {
            SectionHeader(
                title,
                subtitle = "${slices.size} ${if (kind == "income") "sources" else "categories"}",
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            DonutChart(
                slices = slices.take(8).map { ChartSlice(it.label, it.amount, Color(it.colorArgb)) },
                centerLabel = if (kind == "income") "income" else "spent",
                centerValue = total.toCompactTaka(),
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            Spacer(Modifier.height(12.dp))
            slices.take(10).forEach { slice ->
                BreakdownRow(
                    emoji = slice.icon,
                    label = slice.label,
                    amount = slice.amount,
                    share = slice.share,
                    color = Color(slice.colorArgb),
                    caption = String.format(
                        java.util.Locale.US,
                        "%.0f%% \u00B7 %d transactions",
                        slice.share * 100,
                        slice.count
                    )
                )
            }
        }
    }
}
