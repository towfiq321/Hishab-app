package com.hishab.finance.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hishab.finance.data.local.entity.TransactionEntity
import com.hishab.finance.data.local.entity.TxType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(tx: TransactionEntity): Long

    @Insert
    suspend fun insertAll(list: List<TransactionEntity>)

    @Update
    suspend fun update(tx: TransactionEntity)

    @Delete
    suspend fun delete(tx: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun byId(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY dateEpochDay DESC, timeMinutes DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query(
        "SELECT * FROM transactions WHERE dateEpochDay BETWEEN :from AND :to " +
            "ORDER BY dateEpochDay DESC, timeMinutes DESC, id DESC"
    )
    fun observeBetween(from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE dateEpochDay BETWEEN :from AND :to")
    suspend fun between(from: Long, to: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY dateEpochDay ASC, timeMinutes ASC, id ASC")
    suspend fun allOnce(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE type = :type AND dateEpochDay <= :until ORDER BY dateEpochDay ASC")
    suspend fun ofTypeUntil(type: TxType, until: Long): List<TransactionEntity>

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE type = :type AND dateEpochDay BETWEEN :from AND :to"
    )
    fun observeTotal(type: TxType, from: Long, to: Long): Flow<Double>

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE type = :type AND dateEpochDay BETWEEN :from AND :to"
    )
    suspend fun total(type: TxType, from: Long, to: Long): Double

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE type = 'EXPENSE' AND categoryId = :categoryId AND dateEpochDay BETWEEN :from AND :to"
    )
    suspend fun expenseForCategory(categoryId: Long, from: Long, to: Long): Double

    @Query("SELECT * FROM transactions WHERE incomeSourceId = :sourceId ORDER BY dateEpochDay DESC, id DESC")
    fun observeBySource(sourceId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countForCategory(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE incomeSourceId = :sourceId")
    suspend fun countForSource(sourceId: Long): Int

    @Query("SELECT MIN(dateEpochDay) FROM transactions")
    suspend fun earliestDay(): Long?

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int

    @Query("UPDATE transactions SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun detachCategory(categoryId: Long)

    @Query("UPDATE transactions SET incomeSourceId = NULL WHERE incomeSourceId = :sourceId")
    suspend fun detachSource(sourceId: Long)

    @Query("DELETE FROM transactions")
    suspend fun clear()
}
