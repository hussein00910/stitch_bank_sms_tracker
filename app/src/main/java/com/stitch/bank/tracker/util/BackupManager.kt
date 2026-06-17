package com.stitch.bank.tracker.util

import com.stitch.bank.tracker.data.AccountEntity
import com.stitch.bank.tracker.data.AppDatabase
import com.stitch.bank.tracker.data.BudgetEntity
import com.stitch.bank.tracker.data.CategoryEntity
import com.stitch.bank.tracker.data.TransactionEntity
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

/** Full local backup/restore of the database as a single JSON file, so a phone change doesn't lose history. */
object BackupManager {

    suspend fun exportBackup(db: AppDatabase, outputStream: OutputStream) {
        val transactions = db.transactionDao().getAllTransactions().first()
        val categories = db.categoryDao().getAllCategories().first()
        val budgets = db.budgetDao().getAllBudgets().first()
        val accounts = db.accountDao().getAllAccounts().first()

        val root = JSONObject()
        root.put("version", 1)

        root.put("transactions", JSONArray().apply {
            transactions.forEach {
                put(JSONObject().apply {
                    put("id", it.id)
                    put("sender", it.sender)
                    put("body", it.body)
                    put("amount", it.amount)
                    put("date", it.date)
                    put("isIncome", it.isIncome)
                    put("merchant", it.merchant)
                    put("bankName", it.bankName)
                    put("balanceAfter", it.balanceAfter)
                    put("categoryId", it.categoryId)
                })
            }
        })

        root.put("categories", JSONArray().apply {
            categories.forEach {
                put(JSONObject().apply {
                    put("id", it.id)
                    put("name", it.name)
                    put("emoji", it.emoji)
                    put("colorHex", it.colorHex)
                })
            }
        })

        root.put("budgets", JSONArray().apply {
            budgets.forEach {
                put(JSONObject().apply {
                    put("id", it.id)
                    put("categoryId", it.categoryId)
                    put("monthlyLimit", it.monthlyLimit)
                })
            }
        })

        root.put("accounts", JSONArray().apply {
            accounts.forEach {
                put(JSONObject().apply {
                    put("senderKey", it.senderKey)
                    put("displayName", it.displayName)
                    put("colorHex", it.colorHex)
                })
            }
        })

        outputStream.bufferedWriter().use { it.write(root.toString()) }
    }

    suspend fun importBackup(db: AppDatabase, inputStream: InputStream): Int {
        val text = inputStream.bufferedReader().readText()
        val root = JSONObject(text)

        db.transactionDao().deleteAll()
        db.categoryDao().deleteAll()
        db.budgetDao().deleteAll()
        db.accountDao().deleteAll()

        val categories = root.optJSONArray("categories") ?: JSONArray()
        for (i in 0 until categories.length()) {
            val o = categories.getJSONObject(i)
            db.categoryDao().insertCategory(
                CategoryEntity(
                    id = o.getInt("id"),
                    name = o.getString("name"),
                    emoji = o.getString("emoji"),
                    colorHex = o.getString("colorHex")
                )
            )
        }

        val budgets = root.optJSONArray("budgets") ?: JSONArray()
        for (i in 0 until budgets.length()) {
            val o = budgets.getJSONObject(i)
            db.budgetDao().upsertBudget(
                BudgetEntity(
                    id = o.getInt("id"),
                    categoryId = if (o.isNull("categoryId")) null else o.getInt("categoryId"),
                    monthlyLimit = o.getDouble("monthlyLimit")
                )
            )
        }

        val accounts = root.optJSONArray("accounts") ?: JSONArray()
        for (i in 0 until accounts.length()) {
            val o = accounts.getJSONObject(i)
            db.accountDao().upsert(
                AccountEntity(
                    senderKey = o.getString("senderKey"),
                    displayName = o.getString("displayName"),
                    colorHex = o.getString("colorHex")
                )
            )
        }

        val transactions = root.optJSONArray("transactions") ?: JSONArray()
        var count = 0
        for (i in 0 until transactions.length()) {
            val o = transactions.getJSONObject(i)
            db.transactionDao().insertTransaction(
                TransactionEntity(
                    id = o.getInt("id"),
                    sender = o.getString("sender"),
                    body = o.getString("body"),
                    amount = o.getDouble("amount"),
                    date = o.getLong("date"),
                    isIncome = o.getBoolean("isIncome"),
                    merchant = if (o.isNull("merchant")) null else o.getString("merchant"),
                    bankName = o.optString("bankName", o.getString("sender")),
                    balanceAfter = if (o.isNull("balanceAfter")) null else o.getDouble("balanceAfter"),
                    categoryId = if (o.isNull("categoryId")) null else o.getInt("categoryId")
                )
            )
            count++
        }
        return count
    }
}
