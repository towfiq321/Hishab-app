package com.hishab.finance.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.hishab.finance.core.DateX
import com.hishab.finance.core.fileTimestampSafe
import com.hishab.finance.core.mediumLabel
import com.hishab.finance.core.toTaka
import com.hishab.finance.data.repository.TransactionItem
import com.hishab.finance.domain.PeriodSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

/**
 * CSV and PDF output. Both write into `filesDir/exports` and are handed to other apps through
 * the FileProvider declared in the manifest, so no storage permission is needed.
 */
class ExportManager(private val context: Context) {

    private fun exportsDir(): File = File(context.filesDir, "exports").apply { mkdirs() }

    /** Excel opens this directly. The BOM keeps the Taka sign and Bangla text readable. */
    suspend fun exportCsv(items: List<TransactionItem>, from: LocalDate, to: LocalDate): File =
        withContext(Dispatchers.IO) {
            val file = File(exportsDir(), "hishab-transactions-${fileTimestampSafe()}.csv")
            file.bufferedWriter(Charsets.UTF_8).use { out ->
                out.write("\uFEFF")
                out.write("Date,Time,Type,Category,Income Source,Description,Amount (BDT),Payment Method,Note\n")
                items.sortedWith(compareBy({ it.tx.dateEpochDay }, { it.tx.timeMinutes })).forEach { item ->
                    val row = listOf(
                        item.date.toString(),
                        DateX.minutesToLabel(item.tx.timeMinutes),
                        if (item.isIncome) "Income" else "Expense",
                        item.category?.name ?: "",
                        item.source?.name ?: "",
                        item.tx.description,
                        String.format(java.util.Locale.US, "%.2f", item.tx.amount),
                        item.tx.paymentMethod.label,
                        item.tx.note
                    ).joinToString(",") { it.csvEscape() }
                    out.write(row)
                    out.write("\n")
                }
                val income = items.filter { it.isIncome }.sumOf { it.tx.amount }
                val expense = items.filter { !it.isIncome }.sumOf { it.tx.amount }
                out.write("\n")
                out.write("Range,${from} to ${to}\n")
                out.write("Total Income,${String.format(java.util.Locale.US, "%.2f", income)}\n")
                out.write("Total Expense,${String.format(java.util.Locale.US, "%.2f", expense)}\n")
                out.write("Net Balance,${String.format(java.util.Locale.US, "%.2f", income - expense)}\n")
            }
            file
        }

    /** A printable report: summary, category breakdown, income sources, then the transaction list. */
    suspend fun exportPdf(
        summary: PeriodSummary,
        items: List<TransactionItem>,
        title: String
    ): File = withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f

        val h1 = paint(20f, Typeface.DEFAULT_BOLD, Color.parseColor("#0E1B33"))
        val h2 = paint(13f, Typeface.DEFAULT_BOLD, Color.parseColor("#0E1B33"))
        val body = paint(10f, Typeface.DEFAULT, Color.parseColor("#1F2937"))
        val muted = paint(9f, Typeface.DEFAULT, Color.parseColor("#6B7280"))
        val income = paint(10f, Typeface.DEFAULT_BOLD, Color.parseColor("#059669"))
        val expense = paint(10f, Typeface.DEFAULT_BOLD, Color.parseColor("#DC2626"))
        val rule = Paint().apply { color = Color.parseColor("#E5E7EB"); strokeWidth = 1f }

        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = margin + 10f

        fun newPage() {
            canvas.drawText("Page $pageNumber", pageWidth - margin - 40f, pageHeight - 20f, muted)
            doc.finishPage(page)
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = margin
        }

        fun ensure(space: Float) {
            if (y + space > pageHeight - margin) newPage()
        }

        canvas.drawText("Hishab", margin, y, h1)
        y += 18f
        canvas.drawText(title, margin, y, h2)
        y += 14f
        canvas.drawText(
            "${summary.from.mediumLabel()} to ${summary.to.mediumLabel()}  |  generated ${LocalDate.now().mediumLabel()}",
            margin, y, muted
        )
        y += 16f
        canvas.drawLine(margin, y, pageWidth - margin, y, rule)
        y += 24f

        // Summary block
        canvas.drawText("Summary", margin, y, h2); y += 18f
        val rows = listOf(
            "Total income" to summary.income.toTaka(),
            "Total expense" to summary.expense.toTaka(),
            "Net balance" to summary.net.toTaka(),
            "Income transactions" to summary.incomeCount.toString(),
            "Expense transactions" to summary.expenseCount.toString(),
            "Average daily expense" to summary.avgDailyExpense.toTaka(),
            "Average daily income" to summary.avgDailyIncome.toTaka()
        )
        rows.forEach { (label, value) ->
            ensure(16f)
            canvas.drawText(label, margin, y, body)
            canvas.drawText(value, pageWidth - margin - body.measureText(value), y, body)
            y += 16f
        }
        y += 12f

        if (summary.expenseByCategory.isNotEmpty()) {
            ensure(40f)
            canvas.drawText("Expense by category", margin, y, h2); y += 18f
            summary.expenseByCategory.forEach { slice ->
                ensure(15f)
                canvas.drawText("${slice.label}  (${slice.count})", margin, y, body)
                val text = "${slice.amount.toTaka()}   ${(slice.share * 100).toInt()}%"
                canvas.drawText(text, pageWidth - margin - body.measureText(text), y, expense)
                y += 15f
            }
            y += 12f
        }

        if (summary.incomeBySource.isNotEmpty()) {
            ensure(40f)
            canvas.drawText("Income by source", margin, y, h2); y += 18f
            summary.incomeBySource.forEach { slice ->
                ensure(15f)
                canvas.drawText("${slice.label}  (${slice.count})", margin, y, body)
                val text = "${slice.amount.toTaka()}   ${(slice.share * 100).toInt()}%"
                canvas.drawText(text, pageWidth - margin - body.measureText(text), y, income)
                y += 15f
            }
            y += 12f
        }

        if (items.isNotEmpty()) {
            ensure(40f)
            canvas.drawText("Transactions", margin, y, h2); y += 16f
            canvas.drawLine(margin, y, pageWidth - margin, y, rule); y += 14f
            items.sortedWith(compareBy({ it.tx.dateEpochDay }, { it.tx.timeMinutes })).forEach { item ->
                ensure(14f)
                canvas.drawText(item.date.toString(), margin, y, muted)
                canvas.drawText(item.label.take(34), margin + 70f, y, body)
                canvas.drawText(item.bucketName.take(16), margin + 280f, y, muted)
                val amount = item.tx.amount.toTaka()
                canvas.drawText(
                    (if (item.isIncome) "+" else "-") + amount,
                    pageWidth - margin - body.measureText("+$amount"),
                    y,
                    if (item.isIncome) income else expense
                )
                y += 14f
            }
        }

        canvas.drawText("Page $pageNumber", pageWidth - margin - 40f, pageHeight - 20f, muted)
        doc.finishPage(page)

        val file = File(exportsDir(), "hishab-report-${fileTimestampSafe()}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        file
    }

    /** Opens the system share sheet so the file can go to Drive, email, WhatsApp and so on. */
    fun share(file: File, mime: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share ${file.name}").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun mimeFor(file: File): String = when (file.extension.lowercase()) {
        "csv" -> "text/csv"
        "pdf" -> "application/pdf"
        "json" -> "application/json"
        else -> "*/*"
    }

    private fun paint(size: Float, face: Typeface, colorInt: Int) = Paint().apply {
        textSize = size
        typeface = face
        color = colorInt
        isAntiAlias = true
    }
}

private fun String.csvEscape(): String =
    if (contains(',') || contains('"') || contains('\n')) "\"" + replace("\"", "\"\"") + "\"" else this
