package com.stitch.bank.tracker.util

import com.stitch.bank.tracker.data.CategoryEntity
import com.stitch.bank.tracker.data.TransactionEntity
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale

object CsvExporter {

    fun export(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        outputStream: OutputStream
    ) {
        val categoryNames = categories.associateBy({ it.id }, { it.name })
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        outputStream.bufferedWriter().use { writer ->
            writer.appendLine("التاريخ,البنك,الجهة,النوع,المبلغ,الرصيد بعد العملية,الفئة,نص الرسالة")
            transactions.forEach { tx ->
                val row = listOf(
                    sdf.format(java.util.Date(tx.date)),
                    tx.bankName,
                    tx.merchant ?: "",
                    if (tx.isIncome) "وارد" else "صادر",
                    tx.amount.toString(),
                    tx.balanceAfter?.toString() ?: "",
                    tx.categoryId?.let { categoryNames[it] } ?: "",
                    tx.body
                ).joinToString(",") { escapeCsv(it) }
                writer.appendLine(row)
            }
        }
    }

    private fun escapeCsv(value: String): String {
        val needsQuoting = value.contains(",") || value.contains("\"") || value.contains("\n")
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuoting) "\"$escaped\"" else escaped
    }
}
