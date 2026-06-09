package com.stitch.bank.tracker.util

import java.util.Locale

object TransactionParser {

    private val dateTimeRegex = Regex("في\\s*:?\\s*\\d{1,2}/\\d{1,2}/\\d{2,4}\\s+\\d{1,2}:\\d{2}")

    fun isBankMessage(body: String): Boolean {
        val normalized = body.lowercase(Locale.ROOT)

        if (normalized.contains("رمز التفعيل") || normalized.contains("رمز التحقق")
            || normalized.contains("otp") || normalized.contains("الرقم السري")
        ) {
            return false
        }

        if (normalized.contains("http") || normalized.contains("invoice")
            || normalized.contains(".xml") || normalized.contains("zatca")
            || normalized.contains("tax")
        ) {
            return false
        }

        if (!dateTimeRegex.containsMatchIn(body)) {
            return false
        }

        val keywords = listOf(
            "شراء", "سحب", "إيداع", "ايداع", "حوالة", "حواله",
            "خصم", "تحويل", "مبلغ", "شحن"
        )
        return keywords.any { normalized.contains(it) }
    }

    fun extractAmount(body: String): Double {
        val amountLabelRegex = Regex(
            "مبلغ\\s*:?\\s*(?:SAR|ريال)?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:SAR|ريال)?",
            RegexOption.IGNORE_CASE
        )
        amountLabelRegex.find(body)?.let {
            it.groupValues[1].toDoubleOrNull()?.let { v -> return v }
        }

        val baaRegex = Regex("بـ\\s*(\\d+(?:\\.\\d+)?)\\s*(?:SAR|ريال)", RegexOption.IGNORE_CASE)
        baaRegex.find(body)?.let {
            it.groupValues[1].toDoubleOrNull()?.let { v -> return v }
        }

        val currencyRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:SAR|ريال|ر\\.س)", RegexOption.IGNORE_CASE)
        currencyRegex.find(body)?.let {
            it.groupValues[1].toDoubleOrNull()?.let { v -> return v }
        }

        return 0.0
    }

    fun isIncome(body: String): Boolean {
        val normalized = body.lowercase(Locale.ROOT)
        val incomeKeywords = listOf(
            "واردة", "وارده", "إيداع", "ايداع", "استرداد", "ارتجاع", "refund"
        )
        return incomeKeywords.any { normalized.contains(it) }
    }
}
