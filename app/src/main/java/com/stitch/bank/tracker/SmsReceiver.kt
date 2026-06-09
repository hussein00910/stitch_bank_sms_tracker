package com.stitch.bank.tracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.stitch.bank.tracker.data.AppDatabase
import com.stitch.bank.tracker.data.TransactionEntity
import com.stitch.bank.tracker.util.TransactionParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val body = sms.displayMessageBody
                val sender = sms.displayOriginatingAddress ?: "Unknown"
                val date = sms.timestampMillis

                if (TransactionParser.isBankMessage(body)) {
                    val amount = TransactionParser.extractAmount(body)
                    val isIncome = TransactionParser.isIncome(body)
                    
                    val transaction = TransactionEntity(
                        sender = sender,
                        body = body,
                        amount = amount,
                        date = date,
                        isIncome = isIncome
                    )

                    // حفظ في قاعدة البيانات في الخلفية
                    val db = AppDatabase.getDatabase(context)
                    CoroutineScope(Dispatchers.IO).launch {
                        if (!db.transactionDao().exists(body, date)) {
                            db.transactionDao().insertTransaction(transaction)
                        }
                    }
                }
            }
        }
    }
}
