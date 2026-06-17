package com.stitch.bank.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per distinct SMS sender (bank) seen, so the user can rename/recolor it as an "account". */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val senderKey: String,
    val displayName: String,
    val colorHex: String
)
