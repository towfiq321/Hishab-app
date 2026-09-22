package com.hishab.finance.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hishab.finance.data.local.entity.RecurringRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {

    @Insert
    suspend fun insert(rule: RecurringRuleEntity): Long

    @Insert
    suspend fun insertAll(rules: List<RecurringRuleEntity>)

    @Update
    suspend fun update(rule: RecurringRuleEntity)

    @Delete
    suspend fun delete(rule: RecurringRuleEntity)

    @Query("SELECT * FROM recurring_rules ORDER BY isActive DESC, nextDueEpochDay ASC")
    fun observeAll(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE isActive = 1")
    suspend fun active(): List<RecurringRuleEntity>

    @Query("SELECT * FROM recurring_rules")
    suspend fun all(): List<RecurringRuleEntity>

    @Query("SELECT * FROM recurring_rules WHERE id = :id")
    suspend fun byId(id: Long): RecurringRuleEntity?

    @Query("SELECT COUNT(*) FROM recurring_rules")
    suspend fun count(): Int

    @Query("DELETE FROM recurring_rules WHERE categoryId = :categoryId")
    suspend fun removeForCategory(categoryId: Long)

    @Query("DELETE FROM recurring_rules")
    suspend fun clear()
}
