package com.hishab.finance.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hishab.finance.data.local.dao.BudgetDao
import com.hishab.finance.data.local.dao.CategoryDao
import com.hishab.finance.data.local.dao.IncomeSourceDao
import com.hishab.finance.data.local.dao.RecurringDao
import com.hishab.finance.data.local.dao.TransactionDao
import com.hishab.finance.data.local.entity.BudgetEntity
import com.hishab.finance.data.local.entity.CategoryEntity
import com.hishab.finance.data.local.entity.IncomeSourceEntity
import com.hishab.finance.data.local.entity.RecurringRuleEntity
import com.hishab.finance.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        IncomeSourceEntity::class,
        BudgetEntity::class,
        RecurringRuleEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class HishabDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun incomeSourceDao(): IncomeSourceDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringDao(): RecurringDao

    companion object {
        const val NAME = "hishab.db"

        @Volatile private var instance: HishabDatabase? = null

        fun get(context: Context): HishabDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                HishabDatabase::class.java,
                NAME
            )
                // Schema changes in future versions should add real Migration objects here.
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }

        /** Used by restore: close the handle so the file on disk can be swapped. */
        fun closeInstance() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }
    }
}
