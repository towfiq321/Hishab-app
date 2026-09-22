package com.hishab.finance.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hishab.finance.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(budgets: List<BudgetEntity>)

    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth")
    fun observeForMonth(yearMonth: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth")
    suspend fun forMonth(yearMonth: String): List<BudgetEntity>

    @Query("SELECT * FROM budgets")
    suspend fun all(): List<BudgetEntity>

    @Query("DELETE FROM budgets WHERE yearMonth = :yearMonth AND categoryId = :categoryId")
    suspend fun remove(yearMonth: String, categoryId: Long)

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId")
    suspend fun removeForCategory(categoryId: Long)

    @Query("DELETE FROM budgets")
    suspend fun clear()
}
