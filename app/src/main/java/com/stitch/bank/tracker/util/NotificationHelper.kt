package com.stitch.bank.tracker.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.stitch.bank.tracker.R
import com.stitch.bank.tracker.data.TransactionEntity

object NotificationHelper {

    private const val CHANNEL_ID = "new_transactions"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "معاملات جديدة",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تنبيه عند رصد معاملة بنكية جديدة من الرسائل"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun notifyNewTransaction(context: Context, transaction: TransactionEntity, currencyCode: String) {
        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        val amountText = CurrencyFormatter.format(transaction.amount, currencyCode, signed = true)
        val title = if (transaction.isIncome) "معاملة واردة" else "معاملة صادرة"
        val body = listOfNotNull(transaction.bankName, transaction.merchant, amountText)
            .joinToString(" • ")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(transaction.date.toInt(), notification)
        }
    }
}
