package com.stitch.bank.tracker.util

import com.stitch.bank.tracker.data.AccountEntity
import com.stitch.bank.tracker.data.AppDatabase
import com.stitch.bank.tracker.data.TransactionEntity
import kotlinx.coroutines.flow.first

/** Shared enrichment logic so manual sync and the live SMS receiver build identical records. */
object SmsTransactionProcessor {

    suspend fun process(db: AppDatabase, sender: String, body: String, date: Long): TransactionEntity? {
        if (!TransactionParser.isBankMessage(body)) return null
        if (db.transactionDao().exists(body, date)) return null

        val isIncome = TransactionParser.isIncome(body)
        val amount = TransactionParser.extractAmount(body)
        val bankName = TransactionParser.resolveBankName(sender)
        val merchant = TransactionParser.extractMerchant(body)
        val balanceAfter = TransactionParser.extractBalanceAfter(body)

        val categories = db.categoryDao().getAllCategories().first()
        val suggestedName = TransactionParser.suggestCategoryName(body, isIncome)
        val categoryId = categories.firstOrNull { it.name == suggestedName }?.id

        db.accountDao().insertIfAbsent(
            AccountEntity(senderKey = sender, displayName = bankName, colorHex = "#00346F")
        )

        val transaction = TransactionEntity(
            sender = sender,
            body = body,
            amount = amount,
            date = date,
            isIncome = isIncome,
            merchant = merchant,
            bankName = bankName,
            balanceAfter = balanceAfter,
            categoryId = categoryId
        )
        db.transactionDao().insertTransaction(transaction)
        return transaction
    }
}
