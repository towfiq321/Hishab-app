package com.hishab.finance.data.local

import com.hishab.finance.core.DateX
import com.hishab.finance.data.local.entity.BudgetEntity
import com.hishab.finance.data.local.entity.CategoryEntity
import com.hishab.finance.data.local.entity.IncomeSourceEntity
import com.hishab.finance.data.local.entity.PayFrequency
import com.hishab.finance.data.local.entity.PaymentMethod
import com.hishab.finance.data.local.entity.Recurrence
import com.hishab.finance.data.local.entity.RecurringRuleEntity
import com.hishab.finance.data.local.entity.TransactionEntity
import com.hishab.finance.data.local.entity.TxType
import java.time.LocalDate
import java.time.YearMonth
import kotlin.random.Random

/**
 * First-run data: the default category and income-source catalogue, plus an optional
 * four months of demo transactions so every chart, report and forecast has something to show.
 */
class Seeder(private val db: HishabDatabase) {

    companion object {
        val EXPENSE_CATEGORIES = listOf(
            Triple("Food", "\uD83C\uDF5B", 0xFFEF4444.toInt()),
            Triple("Transport", "\uD83D\uDE96", 0xFF3B82F6.toInt()),
            Triple("Housing/Rent", "\uD83C\uDFE0", 0xFF8B5CF6.toInt()),
            Triple("Utilities", "\uD83D\uDCA1", 0xFFF59E0B.toInt()),
            Triple("Shopping", "\uD83D\uDECD", 0xFFEC4899.toInt()),
            Triple("Medical", "\uD83D\uDC8A", 0xFF14B8A6.toInt()),
            Triple("Education", "\uD83D\uDCDA", 0xFF6366F1.toInt()),
            Triple("Entertainment", "\uD83C\uDFAC", 0xFFF97316.toInt()),
            Triple("Mobile/Internet", "\uD83D\uDCF1", 0xFF06B6D4.toInt()),
            Triple("Family", "\uD83D\uDC6A", 0xFFD946EF.toInt()),
            Triple("Business", "\uD83D\uDCBC", 0xFF0EA5E9.toInt()),
            Triple("Travel", "\u2708\uFE0F", 0xFF22C55E.toInt()),
            Triple("Other", "\uD83D\uDCCC", 0xFF94A3B8.toInt())
        )

        val INCOME_CATEGORIES = listOf(
            Triple("Salary", "\uD83D\uDCB0", 0xFF10B981.toInt()),
            Triple("Business", "\uD83C\uDFEA", 0xFF0EA5E9.toInt()),
            Triple("Consulting", "\uD83E\uDDE0", 0xFF8B5CF6.toInt()),
            Triple("Commission", "\uD83E\uDD1D", 0xFFF59E0B.toInt()),
            Triple("Freelancing", "\uD83D\uDCBB", 0xFF6366F1.toInt()),
            Triple("Investment", "\uD83D\uDCC8", 0xFF14B8A6.toInt()),
            Triple("Project Payment", "\uD83D\uDD27", 0xFFEAB308.toInt()),
            Triple("Other", "\u2795", 0xFF94A3B8.toInt())
        )
    }

    /** Inserts the default catalogue if the tables are empty. Safe to call on every launch. */
    suspend fun seedCatalogueIfEmpty() {
        if (db.categoryDao().count() == 0) {
            val expense = EXPENSE_CATEGORIES.mapIndexed { i, (name, icon, color) ->
                CategoryEntity(
                    name = name, type = TxType.EXPENSE, icon = icon,
                    colorArgb = color, isDefault = true, sortOrder = i
                )
            }
            val income = INCOME_CATEGORIES.mapIndexed { i, (name, icon, color) ->
                CategoryEntity(
                    name = name, type = TxType.INCOME, icon = icon,
                    colorArgb = color, isDefault = true, sortOrder = i
                )
            }
            db.categoryDao().insertAll(expense + income)
        }
        if (db.incomeSourceDao().count() == 0) {
            db.incomeSourceDao().insertAll(defaultSources())
        }
    }

    private fun defaultSources() = listOf(
        IncomeSourceEntity(
            name = "Salary", sourceType = "Employment",
            description = "Monthly salary from primary job",
            payFrequency = PayFrequency.MONTHLY, typicalAmount = 60000.0,
            colorArgb = 0xFF10B981.toInt(), icon = "\uD83D\uDCB0"
        ),
        IncomeSourceEntity(
            name = "Consulting", sourceType = "Services",
            description = "Technical consulting engagements",
            payFrequency = PayFrequency.IRREGULAR, typicalAmount = 15000.0,
            colorArgb = 0xFF8B5CF6.toInt(), icon = "\uD83E\uDDE0"
        ),
        IncomeSourceEntity(
            name = "Business", sourceType = "Business",
            description = "Shop and trading income",
            payFrequency = PayFrequency.MONTHLY, typicalAmount = 8000.0,
            colorArgb = 0xFF0EA5E9.toInt(), icon = "\uD83C\uDFEA"
        ),
        IncomeSourceEntity(
            name = "Solar Projects", sourceType = "Project",
            description = "Solar installation and maintenance projects",
            payFrequency = PayFrequency.IRREGULAR, typicalAmount = 20000.0,
            colorArgb = 0xFFF59E0B.toInt(), icon = "\u2600\uFE0F"
        ),
        IncomeSourceEntity(
            name = "Meter Sales", sourceType = "Sales",
            description = "Prepaid and digital meter sales",
            payFrequency = PayFrequency.MONTHLY, typicalAmount = 8000.0,
            colorArgb = 0xFF22C55E.toInt(), icon = "\u26A1"
        )
    )

    /** True when the database has no transactions at all. */
    suspend fun isEmpty(): Boolean = db.transactionDao().count() == 0

    /**
     * Writes ~4 months of plausible history, three recurring rules and two months of budgets.
     * Everything here is ordinary data the user can edit or delete from the app.
     */
    suspend fun seedDemoData(today: LocalDate = DateX.today()) {
        seedCatalogueIfEmpty()
        val cats = db.categoryDao().all()
        val sources = db.incomeSourceDao().all()
        fun cat(name: String, type: TxType) = cats.first { it.name == name && it.type == type }.id
        fun src(name: String) = sources.first { it.name == name }.id

        val food = cat("Food", TxType.EXPENSE)
        val transport = cat("Transport", TxType.EXPENSE)
        val rent = cat("Housing/Rent", TxType.EXPENSE)
        val utilities = cat("Utilities", TxType.EXPENSE)
        val shopping = cat("Shopping", TxType.EXPENSE)
        val medical = cat("Medical", TxType.EXPENSE)
        val entertainment = cat("Entertainment", TxType.EXPENSE)
        val mobile = cat("Mobile/Internet", TxType.EXPENSE)
        val family = cat("Family", TxType.EXPENSE)

        // Recurring rules first, so the bills they generate can point back at them.
        val start = today.minusMonths(4).withDayOfMonth(1)
        val rentRule = db.recurringDao().insert(
            RecurringRuleEntity(
                title = "House rent", amount = 18000.0, categoryId = rent,
                paymentMethod = PaymentMethod.BANK, recurrence = Recurrence.MONTHLY,
                startEpochDay = start.toEpochDay(),
                nextDueEpochDay = nextMonthlyDue(today, 1),
                note = "Paid to landlord on the 1st"
            )
        )
        val netRule = db.recurringDao().insert(
            RecurringRuleEntity(
                title = "Broadband internet", amount = 1200.0, categoryId = mobile,
                paymentMethod = PaymentMethod.MOBILE_BANKING, recurrence = Recurrence.MONTHLY,
                startEpochDay = start.toEpochDay(),
                nextDueEpochDay = nextMonthlyDue(today, 7)
            )
        )
        val mobileRule = db.recurringDao().insert(
            RecurringRuleEntity(
                title = "Mobile recharge", amount = 500.0, categoryId = mobile,
                paymentMethod = PaymentMethod.MOBILE_BANKING, recurrence = Recurrence.MONTHLY,
                startEpochDay = start.toEpochDay(),
                nextDueEpochDay = nextMonthlyDue(today, 10)
            )
        )

        val rnd = Random(20260921)
        val rows = mutableListOf<TransactionEntity>()
        var day = start
        while (!day.isAfter(today)) {
            val dom = day.dayOfMonth

            // --- Daily living ---
            rows += expense(day, 8 * 60 + rnd.nextInt(0, 60), food, "Breakfast",
                pick(rnd, 60, 90, 120, 150, 180), PaymentMethod.CASH)
            if (rnd.nextFloat() < 0.85f) {
                rows += expense(day, 13 * 60 + rnd.nextInt(0, 90), food, "Lunch",
                    pick(rnd, 150, 180, 220, 250, 300), PaymentMethod.CASH)
            }
            if (rnd.nextFloat() < 0.55f) {
                rows += expense(day, 20 * 60 + rnd.nextInt(0, 90), food, "Dinner / groceries",
                    pick(rnd, 200, 260, 320, 420, 650), if (rnd.nextFloat() < 0.3f) PaymentMethod.CARD else PaymentMethod.CASH)
            }
            repeat(rnd.nextInt(1, 4)) {
                rows += expense(day, 9 * 60 + rnd.nextInt(0, 600), transport,
                    pick(rnd, "Rickshaw", "Bus fare", "CNG", "Ride share"),
                    pick(rnd, 30, 50, 80, 100, 150, 220), PaymentMethod.CASH)
            }
            if (rnd.nextFloat() < 0.18f) {
                rows += expense(day, 18 * 60, shopping,
                    pick(rnd, "Clothes", "Household items", "Electronics", "Gift"),
                    pick(rnd, 600, 900, 1500, 2500, 4000), PaymentMethod.CARD)
            }
            if (rnd.nextFloat() < 0.10f) {
                rows += expense(day, 17 * 60, entertainment,
                    pick(rnd, "Cinema", "Outing", "Streaming"), pick(rnd, 300, 500, 800), PaymentMethod.CARD)
            }
            if (rnd.nextFloat() < 0.07f) {
                rows += expense(day, 11 * 60, medical,
                    pick(rnd, "Medicine", "Doctor visit", "Lab test"), pick(rnd, 350, 700, 1200, 2000), PaymentMethod.CASH)
            }
            if (rnd.nextFloat() < 0.08f) {
                rows += expense(day, 19 * 60, family,
                    pick(rnd, "Family support", "Children expense"), pick(rnd, 1000, 2000, 3000), PaymentMethod.MOBILE_BANKING)
            }

            // --- Bills ---
            if (dom == 1) rows += expense(day, 10 * 60, rent, "House rent", 18000.0, PaymentMethod.BANK, rentRule)
            if (dom == 5) rows += expense(day, 12 * 60, utilities, "Electricity bill",
                pick(rnd, 1800, 2200, 2600, 3100).toDouble(), PaymentMethod.MOBILE_BANKING)
            if (dom == 6) rows += expense(day, 12 * 60, utilities, "Gas and water bill", 1350.0, PaymentMethod.MOBILE_BANKING)
            if (dom == 7) rows += expense(day, 11 * 60, mobile, "Broadband internet", 1200.0, PaymentMethod.MOBILE_BANKING, netRule)
            if (dom == 10) rows += expense(day, 11 * 60, mobile, "Mobile recharge", 500.0, PaymentMethod.MOBILE_BANKING, mobileRule)

            // --- Income ---
            if (dom == 1) rows += income(day, 11 * 60, cat("Salary", TxType.INCOME), src("Salary"),
                day.month.name.lowercase().replaceFirstChar { it.uppercase() } + " salary", 60000.0, PaymentMethod.BANK)
            if (dom == 12 && rnd.nextFloat() < 0.8f) rows += income(day, 15 * 60,
                cat("Business", TxType.INCOME), src("Meter Sales"), "Meter sales settlement",
                pick(rnd, 6000, 8000, 11000).toDouble(), PaymentMethod.MOBILE_BANKING)
            if (dom == 15 && rnd.nextFloat() < 0.7f) rows += income(day, 16 * 60,
                cat("Project Payment", TxType.INCOME), src("Solar Projects"), "Solar project instalment",
                pick(rnd, 15000, 20000, 25000).toDouble(), PaymentMethod.BANK)
            if (dom == 22 && rnd.nextFloat() < 0.6f) rows += income(day, 14 * 60,
                cat("Consulting", TxType.INCOME), src("Consulting"), "Consulting fee",
                pick(rnd, 10000, 15000, 18000).toDouble(), PaymentMethod.BANK)

            day = day.plusDays(1)
        }
        db.transactionDao().insertAll(rows)

        // Budgets for this month and last month.
        val thisMonth = YearMonth.from(today)
        val lastMonth = thisMonth.minusMonths(1)
        val plan = listOf(
            food to 15000.0,
            transport to 6000.0,
            shopping to 10000.0,
            utilities to 6000.0,
            mobile to 2500.0,
            entertainment to 3000.0
        )
        val budgets = mutableListOf<BudgetEntity>()
        listOf(thisMonth, lastMonth).forEach { ym ->
            plan.forEach { (catId, amount) ->
                budgets += BudgetEntity(
                    yearMonth = String.format(java.util.Locale.US, "%04d-%02d", ym.year, ym.monthValue),
                    categoryId = catId,
                    amount = amount
                )
            }
        }
        db.budgetDao().upsertAll(budgets)
    }

    /** Wipes transactions, budgets and rules but keeps the category / source catalogue. */
    suspend fun clearAllData() {
        db.transactionDao().clear()
        db.budgetDao().clear()
        db.recurringDao().clear()
    }

    private fun nextMonthlyDue(today: LocalDate, dayOfMonth: Int): Long {
        val thisMonth = today.withDayOfMonth(minOf(dayOfMonth, today.lengthOfMonth()))
        val target = if (thisMonth.isAfter(today)) thisMonth else {
            val next = today.plusMonths(1)
            next.withDayOfMonth(minOf(dayOfMonth, next.lengthOfMonth()))
        }
        return target.toEpochDay()
    }

    private fun expense(
        date: LocalDate, minutes: Int, categoryId: Long, description: String,
        amount: Double, method: PaymentMethod, ruleId: Long? = null
    ) = TransactionEntity(
        type = TxType.EXPENSE, amount = amount, dateEpochDay = date.toEpochDay(),
        timeMinutes = minutes, categoryId = categoryId, incomeSourceId = null,
        description = description, paymentMethod = method, recurringRuleId = ruleId
    )

    private fun expense(
        date: LocalDate, minutes: Int, categoryId: Long, description: String,
        amount: Int, method: PaymentMethod
    ) = expense(date, minutes, categoryId, description, amount.toDouble(), method, null)

    private fun income(
        date: LocalDate, minutes: Int, categoryId: Long, sourceId: Long,
        description: String, amount: Double, method: PaymentMethod
    ) = TransactionEntity(
        type = TxType.INCOME, amount = amount, dateEpochDay = date.toEpochDay(),
        timeMinutes = minutes, categoryId = categoryId, incomeSourceId = sourceId,
        description = description, paymentMethod = method
    )

    private fun pick(rnd: Random, vararg values: Int): Int = values[rnd.nextInt(values.size)]
    private fun pick(rnd: Random, vararg values: String): String = values[rnd.nextInt(values.size)]
}
