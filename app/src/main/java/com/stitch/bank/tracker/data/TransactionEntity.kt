package com.stitch.bank.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String,
    val body: String,
    val amount: Double,
    val date: Long,
    val isIncome: Boolean,
    val merchant: String? = null,
    val bankName: String = sender,
    val balanceAfter: Double? = null,
    val categoryId: Int? = null
)
