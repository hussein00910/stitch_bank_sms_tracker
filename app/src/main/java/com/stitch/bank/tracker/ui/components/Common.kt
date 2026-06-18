package com.stitch.bank.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stitch.bank.tracker.data.CategoryEntity
import com.stitch.bank.tracker.data.TransactionEntity
import com.stitch.bank.tracker.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: IllegalArgumentException) {
    Color.Gray
}

@Composable
fun EmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ReceiptLong,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(64.dp)
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun FilterChipItem(title: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9999.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.height(38.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatCard(title: String, amount: Double, color: Color, icon: ImageVector, currencyCode: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                CurrencyFormatter.format(amount, currencyCode),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    category: CategoryEntity?,
    currencyCode: String,
    onClick: () -> Unit = {}
) {
    val sdf = SimpleDateFormat("hh:mm a", Locale("ar"))
    val dateString = sdf.format(Date(transaction.date))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        category?.let { parseColor(it.colorHex).copy(alpha = 0.15f) }
                            ?: MaterialTheme.colorScheme.background,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (category != null) {
                    Text(category.emoji, fontSize = 18.sp)
                } else {
                    Icon(
                        if (transaction.isIncome) Icons.Default.Payments else Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.merchant ?: transaction.bankName,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                )
                Text(
                    listOfNotNull(category?.name, dateString).joinToString(" • "),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val displayAmount = if (transaction.isIncome) transaction.amount else -transaction.amount
            Text(
                CurrencyFormatter.format(displayAmount, currencyCode, signed = true),
                fontWeight = FontWeight.Bold,
                color = if (transaction.isIncome) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                fontSize = 16.sp
            )
        }
    }
}
