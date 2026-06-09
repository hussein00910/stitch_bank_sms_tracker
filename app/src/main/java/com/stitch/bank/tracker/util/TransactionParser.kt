package com.stitch.bank.tracker.util

import java.util.Locale

object TransactionParser {
    /**
     * Determines whether a message is bank-related based on standard keywords.
     */
    fun isBankMessage(body: String): Boolean {
        val keywords = listOf(
            "تم شراء", "سحب", "إيداع", "bank", "purchase", "amount", 
            "ر.س", "خصم", "مبلغ", "حوالة", "تحويل", "مدى", "visa", 
            "mastercard", "pay", "شحن"
        )
        val normalizedBody = body.lowercase(Locale.ROOT)
        return keywords.any { normalizedBody.contains(it) }
    }

    /**
     * Extracts the transaction amount from the message body, prioritizing currency match
     * and preceding labels over random numbers (e.g., dates or card digits).
     */
    fun extractAmount(body: String): Double {
        val normalizedBody = body.lowercase(Locale.ROOT)

        // 1. Pattern: number followed by currency (e.g., "150.50 ر.س", "100.00 SAR", "50 ريال")
        val currencyRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:ر\\.س|SAR|AED|USD|ريال|ريالاً|SR|sr|dh|egp|جم)", RegexOption.IGNORE_CASE)
        val currencyMatch = currencyRegex.find(normalizedBody)
        if (currencyMatch != null) {
            val amountStr = currencyMatch.groupValues[1]
            amountStr.toDoubleOrNull()?.let { return it }
        }

        // 2. Pattern: number preceded by amount keyword (e.g., "مبلغ 150.50", "بقيمة 100", "amount 50")
        val keywordRegex = Regex("(?:مبلغ|بقيمة|بقيمه|amount|value|purchase of|debit of)\\s*(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)
        val keywordMatch = keywordRegex.find(normalizedBody)
        if (keywordMatch != null) {
            val amountStr = keywordMatch.groupValues[1]
            amountStr.toDoubleOrNull()?.let { return it }
        }

        // 3. Fallback: first decimal number that looks like an amount, avoiding long numbers (like card numbers > 4 digits)
        val fallbackRegex = Regex("\\b\\d+(?:\\.\\d+)?\\b")
        val matches = fallbackRegex.findAll(normalizedBody)
        for (m in matches) {
            val value = m.value
            if (value.length <= 4 || value.contains(".")) {
                value.toDoubleOrNull()?.let { return it }
            }
        }

        // Ultimate fallback: first number
        val firstNumRegex = Regex("(\\d+\\.\\d+|\\d+)")
        val match = firstNumRegex.find(normalizedBody)
        return match?.value?.toDoubleOrNull() ?: 0.0
    }

    /**
     * Determines whether the transaction is an income (deposit, incoming transfer, refund)
     * rather than an expense.
     */
    fun isIncome(body: String): Boolean {
        val normalizedBody = body.lowercase(Locale.ROOT)
        val incomeKeywords = listOf(
            "إيداع", "deposit", "received", "وارد", "حوالة واردة", "تحويل وارد", 
            "إضافة", "اضافة", "ارتجاع", "استرداد", "refund", "تمت إضافة", "تغذية"
        )
        return incomeKeywords.any { normalizedBody.contains(it) }
    }
}
