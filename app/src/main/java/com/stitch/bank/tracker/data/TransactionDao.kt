package com.stitch.bank.tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE body = :body AND date = :date)")
    suspend fun exists(body: String, date: Long): Boolean

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET categoryId = :categoryId WHERE id = :transactionId")
    suspend fun setCategory(transactionId: Int, categoryId: Int?)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
