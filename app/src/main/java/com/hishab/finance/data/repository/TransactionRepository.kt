package com.hishab.finance.data.repository

import com.hishab.finance.data.local.dao.CategoryDao
import com.hishab.finance.data.local.dao.IncomeSourceDao
import com.hishab.finance.data.local.dao.TransactionDao
import com.hishab.finance.data.local.entity.CategoryEntity
import com.hishab.finance.data.local.entity.IncomeSourceEntity
import com.hishab.finance.data.local.entity.PaymentMethod
import com.hishab.finance.data.local.entity.TransactionEntity
import com.hishab.finance.data.local.entity.TxType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** A transaction joined with the names/colours the UI needs to draw it. */
data class TransactionItem(
    val tx: TransactionEntity,
    val category: CategoryEntity?,
    val source: IncomeSourceEntity?
) {
    val isIncome: Boolean get() = tx.type == TxType.INCOME
    val date: LocalDate get() = LocalDate.ofEpochDay(tx.dateEpochDay)
    val label: String
        get() = tx.description.ifBlank {
            if (isIncome) source?.name ?: category?.name ?: "Income"
            else category?.name ?: "Expense"
        }
    val bucketName: String get() = if (isIncome) (source?.name ?: category?.name ?: "Other") else (category?.name ?: "Uncategorised")
    val icon: String get() = if (isIncome) (source?.icon ?: category?.icon ?: "\u2795") else (category?.icon ?: "\uD83D\uDCCC")
    val color: Int get() = if (isIncome) (source?.colorArgb ?: category?.colorArgb ?: 0xFF10B981.toInt()) else (category?.colorArgb ?: 0xFF94A3B8.toInt())
}

/** Everything the Transactions screen can narrow by. */
data class TransactionFilter(
    val query: String = "",
    val type: TxType? = null,
    val categoryIds: Set<Long> = emptySet(),
    val sourceIds: Set<Long> = emptySet(),
    val methods: Set<PaymentMethod> = emptySet(),
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null
) {
    val isActive: Boolean
        get() = query.isNotBlank() || type != null || categoryIds.isNotEmpty() || sourceIds.isNotEmpty() ||
            methods.isNotEmpty() || from != null || to != null || minAmount != null || maxAmount != null
}

class TransactionRepository(
    private val dao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val sourceDao: IncomeSourceDao
) {

    /** Every transaction, already joined with its category and source. */
    fun observeItems(): Flow<List<TransactionItem>> =
        combine(dao.observeAll(), categoryDao.observeAll(), sourceDao.observeAll()) { txs, cats, srcs ->
            join(txs, cats, srcs)
        }

    fun observeItemsBetween(from: LocalDate, to: LocalDate): Flow<List<TransactionItem>> =
        combine(
            dao.observeBetween(from.toEpochDay(), to.toEpochDay()),
            categoryDao.observeAll(),
            sourceDao.observeAll()
        ) { txs, cats, srcs -> join(txs, cats, srcs) }

    fun observeForSource(sourceId: Long): Flow<List<TransactionItem>> =
        combine(dao.observeBySource(sourceId), categoryDao.observeAll(), sourceDao.observeAll()) { txs, cats, srcs ->
            join(txs, cats, srcs)
        }

    fun observeTotal(type: TxType, from: LocalDate, to: LocalDate): Flow<Double> =
        dao.observeTotal(type, from.toEpochDay(), to.toEpochDay())

    /** Applies [filter] client-side. Personal finance volumes stay small enough for this to be instant. */
    fun observeFiltered(filterFlow: Flow<TransactionFilter>): Flow<List<TransactionItem>> =
        combine(observeItems(), filterFlow) { items, filter -> items.filter { it.matches(filter) } }

    suspend fun between(from: LocalDate, to: LocalDate): List<TransactionEntity> =
        dao.between(from.toEpochDay(), to.toEpochDay())

    suspend fun expensesUntil(date: LocalDate): List<TransactionEntity> =
        dao.ofTypeUntil(TxType.EXPENSE, date.toEpochDay())

    suspend fun byId(id: Long): TransactionEntity? = dao.byId(id)

    suspend fun add(tx: TransactionEntity): Long = dao.insert(tx)

    suspend fun update(tx: TransactionEntity) = dao.update(tx.copy(updatedAt = System.currentTimeMillis()))

    suspend fun delete(tx: TransactionEntity) = dao.delete(tx)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun earliestDate(): LocalDate? = dao.earliestDay()?.let { LocalDate.ofEpochDay(it) }

    suspend fun count(): Int = dao.count()

    suspend fun totalBetween(type: TxType, from: LocalDate, to: LocalDate): Double =
        dao.total(type, from.toEpochDay(), to.toEpochDay())

    suspend fun spentOnCategory(categoryId: Long, from: LocalDate, to: LocalDate): Double =
        dao.expenseForCategory(categoryId, from.toEpochDay(), to.toEpochDay())

    private fun join(
        txs: List<TransactionEntity>,
        cats: List<CategoryEntity>,
        srcs: List<IncomeSourceEntity>
    ): List<TransactionItem> {
        val catMap = cats.associateBy { it.id }
        val srcMap = srcs.associateBy { it.id }
        return txs.map { TransactionItem(it, catMap[it.categoryId], srcMap[it.incomeSourceId]) }
    }
}

private fun TransactionItem.matches(f: TransactionFilter): Boolean {
    if (f.type != null && tx.type != f.type) return false
    if (f.categoryIds.isNotEmpty() && tx.categoryId !in f.categoryIds) return false
    if (f.sourceIds.isNotEmpty() && tx.incomeSourceId !in f.sourceIds) return false
    if (f.methods.isNotEmpty() && tx.paymentMethod !in f.methods) return false
    f.from?.let { if (date.isBefore(it)) return false }
    f.to?.let { if (date.isAfter(it)) return false }
    f.minAmount?.let { if (tx.amount < it) return false }
    f.maxAmount?.let { if (tx.amount > it) return false }
    if (f.query.isNotBlank()) {
        val q = f.query.trim().lowercase()
        val haystack = buildString {
            append(tx.description.lowercase()); append(' ')
            append(tx.note.lowercase()); append(' ')
            append(category?.name?.lowercase() ?: ""); append(' ')
            append(source?.name?.lowercase() ?: ""); append(' ')
            append(tx.paymentMethod.label.lowercase()); append(' ')
            append(tx.amount.toLong().toString())
        }
        if (!haystack.contains(q)) return false
    }
    return true
}
