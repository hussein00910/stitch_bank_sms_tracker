package com.stitch.bank.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stitch.bank.tracker.data.CategoryEntity
import com.stitch.bank.tracker.data.TransactionEntity
import com.stitch.bank.tracker.ui.components.EmptyState
import com.stitch.bank.tracker.ui.components.StatCard
import com.stitch.bank.tracker.ui.components.TransactionItem
import com.stitch.bank.tracker.ui.theme.primaryGradient
import com.stitch.bank.tracker.util.CurrencyFormatter

@Composable
fun HomeScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currencyCode: String,
    onShowAllClick: () -> Unit
) {
    val totalBalance = transactions.sumOf { if (it.isIncome) it.amount else -it.amount }
    val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
    val recentTransactions = transactions.take(5)
    val categoryById = categories.associateBy { it.id }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { HeaderSection() }
        item { BalanceCard(totalBalance, currencyCode) }
        item { StatsSection(totalIncome, totalExpense, currencyCode) }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "أحدث المعاملات",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onShowAllClick) {
                    Text("عرض الكل", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (recentTransactions.isEmpty()) {
            item {
                EmptyState(message = "لا توجد معاملات بعد. انقر على مزامنة لجلب الحركات المالية.")
            }
        } else {
            items(recentTransactions) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    category = transaction.categoryId?.let { categoryById[it] },
                    currencyCode = currencyCode
                )
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("مرحباً بك", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("المصرف الذكي", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    brush = Brush.linearGradient(primaryGradient()),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("م", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun BalanceCard(balance: Double, currencyCode: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = primaryGradient(),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 1000f)
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "المصرف الذكي",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Light
                        )
                        Text(
                            "المحفظة الرقمية",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Text(
                        "إجمالي الرصيد المتوفر",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Text(
                        CurrencyFormatter.format(balance, currencyCode),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun StatsSection(income: Double, expense: Double, currencyCode: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("الوارد", income, MaterialTheme.colorScheme.secondary, Icons.Default.ArrowDownward, currencyCode, Modifier.weight(1f))
        StatCard("الصادر", expense, MaterialTheme.colorScheme.error, Icons.Default.ArrowUpward, currencyCode, Modifier.weight(1f))
    }
}
