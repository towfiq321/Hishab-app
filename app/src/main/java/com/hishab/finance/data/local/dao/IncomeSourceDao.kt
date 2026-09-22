package com.hishab.finance.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hishab.finance.data.local.entity.IncomeSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeSourceDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(source: IncomeSourceEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(sources: List<IncomeSourceEntity>)

    @Update
    suspend fun update(source: IncomeSourceEntity)

    @Delete
    suspend fun delete(source: IncomeSourceEntity)

    @Query("SELECT * FROM income_sources ORDER BY isArchived ASC, name ASC")
    fun observeAll(): Flow<List<IncomeSourceEntity>>

    @Query("SELECT * FROM income_sources WHERE id = :id")
    fun observeById(id: Long): Flow<IncomeSourceEntity?>

    @Query("SELECT * FROM income_sources WHERE id = :id")
    suspend fun byId(id: Long): IncomeSourceEntity?

    @Query("SELECT * FROM income_sources")
    suspend fun all(): List<IncomeSourceEntity>

    @Query("SELECT COUNT(*) FROM income_sources")
    suspend fun count(): Int

    @Query("DELETE FROM income_sources")
    suspend fun clear()
}
