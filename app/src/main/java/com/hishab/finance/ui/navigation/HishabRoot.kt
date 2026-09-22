@file:OptIn(ExperimentalMaterial3Api::class)

package com.hishab.finance.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hishab.finance.data.local.entity.TxType
import com.hishab.finance.ui.budget.BudgetScreen
import com.hishab.finance.ui.categories.CategoriesScreen
import com.hishab.finance.ui.components.IconBubble
import com.hishab.finance.ui.dashboard.DashboardScreen
import com.hishab.finance.ui.entry.EntryScreen
import com.hishab.finance.ui.more.MoreScreen
import com.hishab.finance.ui.recurring.RecurringScreen
import com.hishab.finance.ui.reports.ReportsScreen
import com.hishab.finance.ui.sources.SourceDetailScreen
import com.hishab.finance.ui.sources.SourceEditScreen
import com.hishab.finance.ui.sources.SourcesScreen
import com.hishab.finance.ui.theme.HishabTheme

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.DASHBOARD, "Dashboard", Icons.Default.Dashboard),
    Tab(Routes.TRANSACTIONS, "Transactions", Icons.AutoMirrored.Filled.List),
    Tab(Routes.REPORTS, "Reports", Icons.Default.BarChart),
    Tab(Routes.MORE, "More", Icons.Default.MoreHoriz)
)

@Composable
fun HishabRoot() {
    val nav = rememberNavController()
    var showAddSheet by remember { mutableStateOf(false) }
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in TABS.map { it.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(visible = showBottomBar) {
                BottomBar(
                    currentRoute = currentRoute,
                    onSelect = { route -> nav.switchTab(route) },
                    onAdd = { showAddSheet = true }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onAddExpense = { nav.navigate(Routes.entry(TxType.EXPENSE.name)) },
                    onAddIncome = { nav.navigate(Routes.entry(TxType.INCOME.name)) },
                    onOpenTransaction = { id, type -> nav.navigate(Routes.entry(type.name, id)) },
                    onSeeAll = { nav.switchTab(Routes.TRANSACTIONS) },
                    onOpenReports = { nav.switchTab(Routes.REPORTS) },
                    onOpenBudgets = { nav.navigate(Routes.BUDGETS) },
                    onOpenRecurring = { nav.navigate(Routes.RECURRING) }
                )
            }
            composable(Routes.TRANSACTIONS) {
                com.hishab.finance.ui.transactions.TransactionsScreen(
                    onOpenTransaction = { id, type -> nav.navigate(Routes.entry(type.name, id)) }
                )
            }
            composable(Routes.REPORTS) {
                ReportsScreen(onOpenBudgets = { nav.navigate(Routes.BUDGETS) })
            }
            composable(Routes.MORE) {
                MoreScreen(
                    onOpenSources = { nav.navigate(Routes.SOURCES) },
                    onOpenCategories = { nav.navigate(Routes.CATEGORIES) },
                    onOpenBudgets = { nav.navigate(Routes.BUDGETS) },
                    onOpenRecurring = { nav.navigate(Routes.RECURRING) }
                )
            }
            composable(
                route = Routes.ENTRY,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType; defaultValue = TxType.EXPENSE.name },
                    navArgument("id") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { entry ->
                val type = runCatching {
                    TxType.valueOf(entry.arguments?.getString("type") ?: TxType.EXPENSE.name)
                }.getOrDefault(TxType.EXPENSE)
                EntryScreen(
                    initialType = type,
                    transactionId = entry.arguments?.getLong("id") ?: -1L,
                    onDone = { nav.popBackStack() },
                    onManageSources = { nav.navigate(Routes.SOURCES) }
                )
            }
            composable(Routes.SOURCES) {
                SourcesScreen(
                    onBack = { nav.popBackStack() },
                    onOpenSource = { nav.navigate(Routes.sourceDetail(it)) },
                    onAddSource = { nav.navigate(Routes.sourceEdit()) }
                )
            }
            composable(
                route = Routes.SOURCE_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: -1L
                SourceDetailScreen(
                    sourceId = id,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.sourceEdit(id)) },
                    onOpenTransaction = { txId -> nav.navigate(Routes.entry(TxType.INCOME.name, txId)) }
                )
            }
            composable(
                route = Routes.SOURCE_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                SourceEditScreen(
                    sourceId = entry.arguments?.getLong("id") ?: -1L,
                    onDone = { nav.popBackStack() }
                )
            }
            composable(Routes.BUDGETS) { BudgetScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.RECURRING) { RecurringScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.CATEGORIES) { CategoriesScreen(onBack = { nav.popBackStack() }) }
        }
    }

    if (showAddSheet) {
        AddSheet(
            onDismiss = { showAddSheet = false },
            onAddExpense = { showAddSheet = false; nav.navigate(Routes.entry(TxType.EXPENSE.name)) },
            onAddIncome = { showAddSheet = false; nav.navigate(Routes.entry(TxType.INCOME.name)) },
            onAddRecurring = { showAddSheet = false; nav.navigate(Routes.RECURRING) },
            onAddSource = { showAddSheet = false; nav.navigate(Routes.sourceEdit()) }
        )
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomBar(currentRoute: String?, onSelect: (String) -> Unit, onAdd: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationBar(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                TABS.take(2).forEach { tab ->
                    TabItem(tab, currentRoute, onSelect)
                }
            }
            AddButton(onAdd)
            NavigationBar(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                TABS.drop(2).forEach { tab ->
                    TabItem(tab, currentRoute, onSelect)
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabItem(
    tab: Tab,
    currentRoute: String?,
    onSelect: (String) -> Unit
) {
    NavigationBarItem(
        selected = currentRoute == tab.route,
        onClick = { onSelect(tab.route) },
        icon = { Icon(tab.icon, contentDescription = tab.label) },
        label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

@Composable
private fun AddButton(onAdd: () -> Unit) {
    Box(
        Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(HishabTheme.money.heroBrush)
            .clickable(onClick = onAdd),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Add a transaction",
            tint = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun AddSheet(
    onDismiss: () -> Unit,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onAddRecurring: () -> Unit,
    onAddSource: () -> Unit
) {
    val state = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("What are you adding?", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(14.dp))
            AddOption("\uD83D\uDCB8", "Expense", "Money you spent", HishabTheme.money.expense, onAddExpense)
            AddOption("\uD83D\uDCB0", "Income", "Money you received", HishabTheme.money.income, onAddIncome)
            AddOption("\uD83D\uDD01", "Recurring expense", "Rent, bills, subscriptions", HishabTheme.money.forecast, onAddRecurring)
            AddOption("\uD83D\uDCBC", "Income source", "A person, company or project that pays you", MaterialTheme.colorScheme.primary, onAddSource)
        }
    }
}

@Composable
private fun AddOption(
    emoji: String,
    title: String,
    subtitle: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        IconBubble(emoji, tint, size = 44)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
