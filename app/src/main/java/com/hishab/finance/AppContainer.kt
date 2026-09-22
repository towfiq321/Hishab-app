package com.hishab.finance

import android.content.Context
import com.hishab.finance.data.backup.BackupManager
import com.hishab.finance.data.export.ExportManager
import com.hishab.finance.data.local.HishabDatabase
import com.hishab.finance.data.local.Seeder
import com.hishab.finance.data.repository.BudgetRepository
import com.hishab.finance.data.repository.CatalogRepository
import com.hishab.finance.data.repository.RecurringRepository
import com.hishab.finance.data.repository.SettingsStore
import com.hishab.finance.data.repository.TransactionRepository
import com.hishab.finance.domain.ForecastEngine

/**
 * Manual dependency container. The graph is small and fully synchronous, so a plain container
 * keeps the build simple; swapping in Hilt later only means replacing this class.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    val database: HishabDatabase = HishabDatabase.get(appContext)

    val transactions = TransactionRepository(
        dao = database.transactionDao(),
        categoryDao = database.categoryDao(),
        sourceDao = database.incomeSourceDao()
    )

    val catalog = CatalogRepository(
        categoryDao = database.categoryDao(),
        sourceDao = database.incomeSourceDao(),
        transactionDao = database.transactionDao(),
        budgetDao = database.budgetDao(),
        recurringDao = database.recurringDao()
    )

    val budgets = BudgetRepository(
        budgetDao = database.budgetDao(),
        categoryDao = database.categoryDao(),
        transactionDao = database.transactionDao()
    )

    val recurring = RecurringRepository(
        dao = database.recurringDao(),
        transactionDao = database.transactionDao()
    )

    val forecast = ForecastEngine(transactions, recurring, catalog)
    val settings = SettingsStore(appContext)
    val seeder = Seeder(database)
    val backup = BackupManager(appContext, database)
    val exporter = ExportManager(appContext)
}
