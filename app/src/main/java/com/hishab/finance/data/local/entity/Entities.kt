package com.hishab.finance.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Expense or income. Stored as a string column via [com.hishab.finance.data.local.Converters]. */
enum class TxType { EXPENSE, INCOME }

enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    BANK("Bank"),
    MOBILE_BANKING("Mobile Banking"),
    CARD("Card"),
    OTHER("Other")
}

enum class Recurrence(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
    CUSTOM("Custom (every N days)")
}

/** How often a source is expected to pay. Informational only - income is never forecast. */
enum class PayFrequency(val label: String) {
    ONE_OFF("One-off"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    YEARLY("Yearly"),
    IRREGULAR("Irregular")
}

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name", "type"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TxType,
    /** Emoji glyph - keeps custom icons possible without shipping an icon pack. */
    val icon: String = "\uD83D\uDCB8",
    val colorArgb: Int,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false
)

@Entity(
    tableName = "income_sources",
    indices = [Index(value = ["name"], unique = true)]
)
data class IncomeSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sourceType: String = "Other",
    val description: String = "",
    val contactName: String = "",
    val contactPhone: String = "",
    val email: String = "",
    val address: String = "",
    val payFrequency: PayFrequency = PayFrequency.IRREGULAR,
    val typicalAmount: Double = 0.0,
    val notes: String = "",
    val colorArgb: Int,
    val icon: String = "\uD83D\uDCBC",
    val isArchived: Boolean = false
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["dateEpochDay"]),
        Index(value = ["type"]),
        Index(value = ["categoryId"]),
        Index(value = ["incomeSourceId"]),
        Index(value = ["recurringRuleId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    val amount: Double,
    /** Epoch day, so BETWEEN queries stay correct and cheap. */
    val dateEpochDay: Long,
    /** Minutes from midnight. */
    val timeMinutes: Int,
    val categoryId: Long?,
    val incomeSourceId: Long?,
    val description: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val note: String = "",
    /** Set when the row was auto-posted from a recurring rule; used to avoid double counting in forecasts. */
    val recurringRuleId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["yearMonth", "categoryId"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** `2026-09` */
    val yearMonth: String,
    val categoryId: Long,
    val amount: Double
)

@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val categoryId: Long,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val recurrence: Recurrence = Recurrence.MONTHLY,
    /** Every N periods; for CUSTOM this is a day interval. */
    val interval: Int = 1,
    val startEpochDay: Long,
    val endEpochDay: Long? = null,
    val nextDueEpochDay: Long,
    val lastPostedEpochDay: Long? = null,
    /** When true the app writes the expense automatically once it falls due. */
    val autoPost: Boolean = true,
    val isActive: Boolean = true,
    val note: String = ""
)
