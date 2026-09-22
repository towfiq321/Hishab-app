package com.hishab.finance.data.backup

import android.content.Context
import android.net.Uri
import com.hishab.finance.core.DateX
import com.hishab.finance.data.local.HishabDatabase
import com.hishab.finance.data.local.entity.BudgetEntity
import com.hishab.finance.data.local.entity.CategoryEntity
import com.hishab.finance.data.local.entity.IncomeSourceEntity
import com.hishab.finance.data.local.entity.PayFrequency
import com.hishab.finance.data.local.entity.PaymentMethod
import com.hishab.finance.data.local.entity.Recurrence
import com.hishab.finance.data.local.entity.RecurringRuleEntity
import com.hishab.finance.data.local.entity.TransactionEntity
import com.hishab.finance.data.local.entity.TxType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Whole-database backup as a single readable JSON file. JSON rather than a copy of the .db file
 * so a backup taken today can still be restored after a future schema change.
 */
class BackupManager(
    private val context: Context,
    private val db: HishabDatabase
) {
    companion object {
        const val FORMAT_VERSION = 1
    }

    data class RestoreResult(val transactions: Int, val categories: Int, val sources: Int, val rules: Int, val budgets: Int)

    suspend fun createBackup(): File = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("format", FORMAT_VERSION)
        root.put("app", "Hishab")
        root.put("createdAt", System.currentTimeMillis())

        root.put("categories", JSONArray().apply {
            db.categoryDao().all().forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id); put("name", c.name); put("type", c.type.name)
                    put("icon", c.icon); put("colorArgb", c.colorArgb)
                    put("isDefault", c.isDefault); put("sortOrder", c.sortOrder); put("isArchived", c.isArchived)
                })
            }
        })

        root.put("sources", JSONArray().apply {
            db.incomeSourceDao().all().forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id); put("name", s.name); put("sourceType", s.sourceType)
                    put("description", s.description); put("contactName", s.contactName)
                    put("contactPhone", s.contactPhone); put("email", s.email); put("address", s.address)
                    put("payFrequency", s.payFrequency.name); put("typicalAmount", s.typicalAmount)
                    put("notes", s.notes); put("colorArgb", s.colorArgb); put("icon", s.icon)
                    put("isArchived", s.isArchived)
                })
            }
        })

        root.put("transactions", JSONArray().apply {
            db.transactionDao().allOnce().forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id); put("type", t.type.name); put("amount", t.amount)
                    put("dateEpochDay", t.dateEpochDay); put("timeMinutes", t.timeMinutes)
                    put("categoryId", t.categoryId ?: JSONObject.NULL)
                    put("incomeSourceId", t.incomeSourceId ?: JSONObject.NULL)
                    put("description", t.description); put("paymentMethod", t.paymentMethod.name)
                    put("note", t.note); put("recurringRuleId", t.recurringRuleId ?: JSONObject.NULL)
                    put("createdAt", t.createdAt); put("updatedAt", t.updatedAt)
                })
            }
        })

        root.put("budgets", JSONArray().apply {
            db.budgetDao().all().forEach { b ->
                put(JSONObject().apply {
                    put("id", b.id); put("yearMonth", b.yearMonth)
                    put("categoryId", b.categoryId); put("amount", b.amount)
                })
            }
        })

        root.put("recurring", JSONArray().apply {
            db.recurringDao().all().forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id); put("title", r.title); put("amount", r.amount)
                    put("categoryId", r.categoryId); put("paymentMethod", r.paymentMethod.name)
                    put("recurrence", r.recurrence.name); put("interval", r.interval)
                    put("startEpochDay", r.startEpochDay)
                    put("endEpochDay", r.endEpochDay ?: JSONObject.NULL)
                    put("nextDueEpochDay", r.nextDueEpochDay)
                    put("lastPostedEpochDay", r.lastPostedEpochDay ?: JSONObject.NULL)
                    put("autoPost", r.autoPost); put("isActive", r.isActive); put("note", r.note)
                })
            }
        })

        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val file = File(dir, "hishab-backup-${DateX.fileTimestamp()}.json")
        file.writeText(root.toString(2))
        file
    }

    /** Replaces everything currently in the database with the contents of [uri]. */
    suspend fun restore(uri: Uri): RestoreResult = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Could not open the backup file")
        val root = JSONObject(text)
        require(root.optInt("format", 0) <= FORMAT_VERSION) {
            "This backup was made by a newer version of Hishab"
        }

        db.transactionDao().clear()
        db.budgetDao().clear()
        db.recurringDao().clear()
        db.categoryDao().clear()
        db.incomeSourceDao().clear()

        val categories = root.optJSONArray("categories").toList().map { o ->
            CategoryEntity(
                id = o.optLong("id"), name = o.getString("name"),
                type = TxType.valueOf(o.optString("type", "EXPENSE")),
                icon = o.optString("icon", "\uD83D\uDCCC"), colorArgb = o.optInt("colorArgb"),
                isDefault = o.optBoolean("isDefault"), sortOrder = o.optInt("sortOrder"),
                isArchived = o.optBoolean("isArchived")
            )
        }
        db.categoryDao().insertAll(categories)

        val sources = root.optJSONArray("sources").toList().map { o ->
            IncomeSourceEntity(
                id = o.optLong("id"), name = o.getString("name"),
                sourceType = o.optString("sourceType", "Other"),
                description = o.optString("description"), contactName = o.optString("contactName"),
                contactPhone = o.optString("contactPhone"), email = o.optString("email"),
                address = o.optString("address"),
                payFrequency = runCatching { PayFrequency.valueOf(o.optString("payFrequency")) }
                    .getOrDefault(PayFrequency.IRREGULAR),
                typicalAmount = o.optDouble("typicalAmount", 0.0), notes = o.optString("notes"),
                colorArgb = o.optInt("colorArgb"), icon = o.optString("icon", "\uD83D\uDCBC"),
                isArchived = o.optBoolean("isArchived")
            )
        }
        db.incomeSourceDao().insertAll(sources)

        val transactions = root.optJSONArray("transactions").toList().map { o ->
            TransactionEntity(
                id = o.optLong("id"), type = TxType.valueOf(o.optString("type", "EXPENSE")),
                amount = o.optDouble("amount"), dateEpochDay = o.optLong("dateEpochDay"),
                timeMinutes = o.optInt("timeMinutes"),
                categoryId = o.optLongOrNull("categoryId"), incomeSourceId = o.optLongOrNull("incomeSourceId"),
                description = o.optString("description"),
                paymentMethod = runCatching { PaymentMethod.valueOf(o.optString("paymentMethod")) }
                    .getOrDefault(PaymentMethod.OTHER),
                note = o.optString("note"), recurringRuleId = o.optLongOrNull("recurringRuleId"),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
            )
        }
        db.transactionDao().insertAll(transactions)

        val budgets = root.optJSONArray("budgets").toList().map { o ->
            BudgetEntity(
                id = o.optLong("id"), yearMonth = o.getString("yearMonth"),
                categoryId = o.optLong("categoryId"), amount = o.optDouble("amount")
            )
        }
        db.budgetDao().upsertAll(budgets)

        val rules = root.optJSONArray("recurring").toList().map { o ->
            RecurringRuleEntity(
                id = o.optLong("id"), title = o.optString("title"), amount = o.optDouble("amount"),
                categoryId = o.optLong("categoryId"),
                paymentMethod = runCatching { PaymentMethod.valueOf(o.optString("paymentMethod")) }
                    .getOrDefault(PaymentMethod.OTHER),
                recurrence = runCatching { Recurrence.valueOf(o.optString("recurrence")) }
                    .getOrDefault(Recurrence.MONTHLY),
                interval = o.optInt("interval", 1), startEpochDay = o.optLong("startEpochDay"),
                endEpochDay = o.optLongOrNull("endEpochDay"), nextDueEpochDay = o.optLong("nextDueEpochDay"),
                lastPostedEpochDay = o.optLongOrNull("lastPostedEpochDay"),
                autoPost = o.optBoolean("autoPost", true), isActive = o.optBoolean("isActive", true),
                note = o.optString("note")
            )
        }
        db.recurringDao().insertAll(rules)

        RestoreResult(transactions.size, categories.size, sources.size, rules.size, budgets.size)
    }
}

private fun JSONArray?.toList(): List<JSONObject> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { optJSONObject(it) }
}

private fun JSONObject.optLongOrNull(key: String): Long? =
    if (isNull(key)) null else optLong(key)
