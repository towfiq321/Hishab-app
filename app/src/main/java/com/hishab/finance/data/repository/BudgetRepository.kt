package com.hishab.finance.data.repository

import com.hishab.finance.core.key
import com.hishab.finance.data.local.dao.BudgetDao
import com.hishab.finance.data.local.dao.CategoryDao
import com.hishab.finance.data.local.dao.TransactionDao
import com.hishab.finance.data.local.entity.BudgetEntity
import com.hishab.finance.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth

/** A budget line with the spending already applied. */
data class BudgetProgress(
    val category: CategoryEntity,
    val budget: Double,
    val spent: Double
) {
    val remaining: Double get() = budget - spent
    val ratio: Float get() = if (budget <= 0) 0f else (spent / budget).toFloat()
    val state: BudgetState
        get() = when {
            budget <= 0 -> BudgetState.UNSET
            spent > budget -> BudgetState.OVER
            ratio >= 0.85f -> BudgetState.NEAR
            else -> BudgetState.OK
        }
}

enum class BudgetState { OK, NEAR, OVER, UNSET }

class BudgetRepository(
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) {

    fun observeMonth(month: YearMonth): Flow<List<BudgetProgress>> =
        combine(
            budgetDao.observeForMonth(month.key()),
            categoryDao.observeAll(),
            transactionDao.observeBetween(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay())
        ) { budgets, categories, txs ->
            val byCategory = categories.associateBy { it.id }
            val spentByCategory = txs
                .filter { it.type == com.hishab.finance.data.local.entity.TxType.EXPENSE }
                .groupBy { it.categoryId }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            budgets.mapNotNull { budget ->
                byCategory[budget.categoryId]?.let { category ->
                    BudgetProgress(category, budget.amount, spentByCategory[category.id] ?: 0.0)
                }
            }.sortedByDescending { it.ratio }
        }

    suspend fun setBudget(month: YearMonth, categoryId: Long, amount: Double) {
        if (amount <= 0.0) {
            budgetDao.remove(month.key(), categoryId)
        } else {
            budgetDao.upsert(BudgetEntity(yearMonth = month.key(), categoryId = categoryId, amount = amount))
        }
    }

    suspend fun removeBudget(month: YearMonth, categoryId: Long) = budgetDao.remove(month.key(), categoryId)

    /** Convenience for a new month: copy last month's plan forward. */
    suspend fun copyFromPreviousMonth(month: YearMonth): Int {
        val previous = budgetDao.forMonth(month.minusMonths(1).key())
        if (previous.isEmpty()) return 0
        budgetDao.upsertAll(previous.map { BudgetEntity(yearMonth = month.key(), categoryId = it.categoryId, amount = it.amount) })
        return previous.size
    }

    suspend fun monthTotals(month: YearMonth): Pair<Double, Double> {
        val budgets = budgetDao.forMonth(month.key())
        val planned = budgets.sumOf { it.amount }
        val from = month.atDay(1).toEpochDay()
        val to = month.atEndOfMonth().toEpochDay()
        var spent = 0.0
        budgets.forEach { spent += transactionDao.expenseForCategory(it.categoryId, from, to) }
        return planned to spent
    }

    /** Lines that are at or past their limit for the month containing [date]. */
    suspend fun breaches(date: LocalDate = LocalDate.now()): List<BudgetProgress> {
        val month = YearMonth.from(date)
        val budgets = budgetDao.forMonth(month.key())
        val categories = categoryDao.all().associateBy { it.id }
        val from = month.atDay(1).toEpochDay()
        val to = month.atEndOfMonth().toEpochDay()
        return budgets.mapNotNull { budget ->
            val category = categories[budget.categoryId] ?: return@mapNotNull null
            val spent = transactionDao.expenseForCategory(budget.categoryId, from, to)
            BudgetProgress(category, budget.amount, spent).takeIf { it.ratio >= 0.85f }
        }
    }
}
