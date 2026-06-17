package com.stitch.bank.tracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.stitch.bank.tracker.data.AppDatabase
import com.stitch.bank.tracker.util.NotificationHelper
import com.stitch.bank.tracker.util.SettingsManager
import com.stitch.bank.tracker.util.SmsTransactionProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val db = AppDatabase.getDatabase(context)
        val settingsManager = SettingsManager(context)
        NotificationHelper.ensureChannel(context)

        CoroutineScope(Dispatchers.IO).launch {
            for (sms in messages) {
                val body = sms.displayMessageBody ?: continue
                val sender = sms.displayOriginatingAddress ?: "Unknown"
                val date = sms.timestampMillis

                val transaction = SmsTransactionProcessor.process(db, sender, body, date)
                if (transaction != null && settingsManager.settings.value.notificationsEnabled) {
                    NotificationHelper.notifyNewTransaction(context, transaction, settingsManager.settings.value.currencyCode)
                }
            }
        }
    }
}
