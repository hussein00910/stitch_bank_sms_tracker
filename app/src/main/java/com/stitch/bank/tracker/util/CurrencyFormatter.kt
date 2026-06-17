package com.stitch.bank.tracker.util

import java.util.Locale

object CurrencyFormatter {

    val supportedCurrencies = listOf("SAR", "AED", "USD", "EGP", "KWD", "QAR", "BHD")

    private val symbols = mapOf(
        "SAR" to "ر.س",
        "AED" to "د.إ",
        "USD" to "$",
        "EGP" to "ج.م",
        "KWD" to "د.ك",
        "QAR" to "ر.ق",
        "BHD" to "د.ب"
    )

    fun symbolFor(currencyCode: String): String = symbols[currencyCode] ?: currencyCode

    fun format(amount: Double, currencyCode: String, signed: Boolean = false): String {
        val sign = if (signed && amount > 0) "+" else ""
        val formatted = String.format(Locale.US, "%,.2f", amount)
        return "$sign$formatted ${symbolFor(currencyCode)}"
    }
}
