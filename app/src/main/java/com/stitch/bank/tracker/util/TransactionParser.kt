package com.stitch.bank.tracker.util

import java.util.Locale

object TransactionParser {

    private val dateTimeRegex = Regex(
        "(?:في|بتاريخ|on)\\s*:?\\s*\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}(?:\\s+\\d{1,2}:\\d{2})?",
        RegexOption.IGNORE_CASE
    )

    private val currencyAmountRegex = Regex(
        "(\\d+(?:[.,]\\d+)?)\\s*(?:SAR|sar|AED|aed|USD|usd|EGP|egp|KWD|kwd|QAR|qar|BHD|bhd|ريال|ر\\.س|درهم|دينار)",
        RegexOption.IGNORE_CASE
    )

    private val transactionKeywords = listOf(
        "شراء", "سحب", "إيداع", "ايداع", "حوالة", "حواله",
        "خصم", "تحويل", "مبلغ", "شحن", "purchase", "withdraw",
        "deposit", "transfer", "debit", "credit", "payment", "pos"
    )

    private val incomeKeywords = listOf(
        "واردة", "وارده", "إيداع", "ايداع", "استرداد", "ارتجاع",
        "راتب", "credit", "refund", "deposit", "incoming"
    )

    private val excludeKeywords = listOf(
        "رمز التفعيل", "رمز التحقق", "otp", "الرقم السري",
        "http", "invoice", ".xml", "zatca", "tax"
    )

    /** Known Saudi/Gulf bank sender IDs mapped to a friendly display name. Extend as needed. */
    private val knownBankSenders = mapOf(
        "alrajhibank" to "مصرف الراجحي",
        "alrajhi" to "مصرف الراجحي",
        "snb" to "البنك الأهلي",
        "ahli" to "البنك الأهلي",
        "riyadbank" to "بنك الرياض",
        "alinma" to "مصرف الإنماء",
        "anb" to "البنك العربي الوطني",
        "samba" to "بنك سامبا",
        "sabb" to "بنك ساب",
        "bsf" to "البنك السعودي الفرنسي",
        "stcpay" to "STC Pay"
    )

    fun isBankMessage(body: String): Boolean {
        val normalized = body.lowercase(Locale.ROOT)

        if (excludeKeywords.any { normalized.contains(it) }) {
            return false
        }

        val hasDate = dateTimeRegex.containsMatchIn(body)
        val hasAmount = currencyAmountRegex.containsMatchIn(body)
        val hasKeyword = transactionKeywords.any { normalized.contains(it) }

        // Accept either a clear "date + keyword" bank template, or an amount + keyword
        // (covers banks that omit an explicit date/time in the SMS body).
        return (hasDate && hasKeyword) || (hasAmount && hasKeyword)
    }

    fun extractAmount(body: String): Double {
        val amountLabelRegex = Regex(
            "مبلغ\\s*:?\\s*(?:SAR|ريال)?\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:SAR|ريال)?",
            RegexOption.IGNORE_CASE
        )
        amountLabelRegex.find(body)?.let {
            it.groupValues[1].replace(",", "").toDoubleOrNull()?.let { v -> return v }
        }

        val baaRegex = Regex("بـ\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:SAR|ريال)", RegexOption.IGNORE_CASE)
        baaRegex.find(body)?.let {
            it.groupValues[1].replace(",", "").toDoubleOrNull()?.let { v -> return v }
        }

        currencyAmountRegex.find(body)?.let {
            it.groupValues[1].replace(",", "").toDoubleOrNull()?.let { v -> return v }
        }

        return 0.0
    }

    fun isIncome(body: String): Boolean {
        val normalized = body.lowercase(Locale.ROOT)
        return incomeKeywords.any { normalized.contains(it) }
    }

    /** Best-effort extraction of the merchant/counterparty name from common bank SMS templates. */
    fun extractMerchant(body: String): String? {
        val patterns = listOf(
            Regex("لدى\\s+([^\\n,.;]{2,40})"),
            Regex("في\\s+متجر\\s+([^\\n,.;]{2,40})"),
            Regex("at\\s+([^\\n,.;]{2,40})", RegexOption.IGNORE_CASE),
            Regex("من\\s+([^\\n,.;]{2,40})")
        )
        for (pattern in patterns) {
            pattern.find(body)?.let { match ->
                val candidate = match.groupValues[1].trim()
                if (candidate.isNotEmpty()) return candidate.take(40)
            }
        }
        return null
    }

    /** Best-effort extraction of the post-transaction available balance, if present. */
    fun extractBalanceAfter(body: String): Double? {
        val patterns = listOf(
            Regex("الرصيد\\s*(?:المتبقي|الحالي|المتاح)?\\s*:?\\s*(\\d+(?:[.,]\\d+)?)", RegexOption.IGNORE_CASE),
            Regex("available\\s*balance\\s*:?\\s*(\\d+(?:[.,]\\d+)?)", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            pattern.find(body)?.let {
                it.groupValues[1].replace(",", "").toDoubleOrNull()?.let { v -> return v }
            }
        }
        return null
    }

    /** Maps a raw SMS sender id to a friendly bank name when recognized, else returns it unchanged. */
    fun resolveBankName(sender: String): String {
        val key = sender.lowercase(Locale.ROOT).replace(Regex("[^a-z]"), "")
        return knownBankSenders.entries.firstOrNull { key.contains(it.key) }?.value ?: sender
    }

    /** Suggests a default category name (matching [DefaultCategories]) based on message content. */
    fun suggestCategoryName(body: String, isIncome: Boolean): String {
        if (isIncome) {
            val normalized = body.lowercase(Locale.ROOT)
            if (normalized.contains("راتب")) return "راتب ودخل"
            if (normalized.contains("حوال") || normalized.contains("تحويل")) return "تحويلات"
            return "راتب ودخل"
        }

        val normalized = body.lowercase(Locale.ROOT)
        return when {
            listOf("سوبر", "بقال", "ماركت", "تموينات").any { normalized.contains(it) } -> "بقالة وسوبر ماركت"
            listOf("مطعم", "كافي", "قهوة", "restaurant", "cafe").any { normalized.contains(it) } -> "مطاعم وكافيهات"
            listOf("فاتورة", "كهرباء", "اتصالات", "stc", "مياه", "اشتراك").any { normalized.contains(it) } -> "فواتير وخدمات"
            listOf("بنزين", "وقود", "محطة", "نقل", "اوبر", "كريم", "uber", "careem").any { normalized.contains(it) } -> "نقل ووقود"
            listOf("صيدلية", "مستشفى", "عيادة", "طبي").any { normalized.contains(it) } -> "صحة وصيدلية"
            listOf("حوال", "تحويل").any { normalized.contains(it) } -> "تحويلات"
            else -> "أخرى"
        }
    }
}
