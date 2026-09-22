package com.hishab.finance.data.repository

import com.hishab.finance.data.local.dao.BudgetDao
import com.hishab.finance.data.local.dao.CategoryDao
import com.hishab.finance.data.local.dao.IncomeSourceDao
import com.hishab.finance.data.local.dao.RecurringDao
import com.hishab.finance.data.local.dao.TransactionDao
import com.hishab.finance.data.local.entity.CategoryEntity
import com.hishab.finance.data.local.entity.IncomeSourceEntity
import com.hishab.finance.data.local.entity.TxType
import kotlinx.coroutines.flow.Flow

/** Categories and income sources - the two lists the rest of the app picks from. */
class CatalogRepository(
    private val categoryDao: CategoryDao,
    private val sourceDao: IncomeSourceDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val recurringDao: RecurringDao
) {
    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()
    fun observeCategories(type: TxType): Flow<List<CategoryEntity>> = categoryDao.observeByType(type)
    fun observeSources(): Flow<List<IncomeSourceEntity>> = sourceDao.observeAll()
    fun observeSource(id: Long): Flow<IncomeSourceEntity?> = sourceDao.observeById(id)

    suspend fun categories(): List<CategoryEntity> = categoryDao.all()
    suspend fun sources(): List<IncomeSourceEntity> = sourceDao.all()
    suspend fun category(id: Long): CategoryEntity? = categoryDao.byId(id)
    suspend fun source(id: Long): IncomeSourceEntity? = sourceDao.byId(id)

    suspend fun addCategory(category: CategoryEntity): Long = categoryDao.insert(category)
    suspend fun updateCategory(category: CategoryEntity) = categoryDao.update(category)

    /**
     * Deleting a category never deletes money. Transactions that used it are detached and show as
     * "Uncategorised"; budgets and recurring rules that pointed at it are removed.
     */
    suspend fun deleteCategory(category: CategoryEntity) {
        transactionDao.detachCategory(category.id)
        budgetDao.removeForCategory(category.id)
        recurringDao.removeForCategory(category.id)
        categoryDao.delete(category)
    }

    suspend fun categoryUsageCount(id: Long): Int = transactionDao.countForCategory(id)

    suspend fun addSource(source: IncomeSourceEntity): Long = sourceDao.insert(source)
    suspend fun updateSource(source: IncomeSourceEntity) = sourceDao.update(source)

    suspend fun deleteSource(source: IncomeSourceEntity) {
        transactionDao.detachSource(source.id)
        sourceDao.delete(source)
    }

    suspend fun sourceUsageCount(id: Long): Int = transactionDao.countForSource(id)
}
