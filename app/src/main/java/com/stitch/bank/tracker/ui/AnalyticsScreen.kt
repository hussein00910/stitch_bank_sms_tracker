package com.stitch.bank.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.stitch.bank.tracker.data.BudgetEntity
import com.stitch.bank.tracker.data.CategoryEntity
import com.stitch.bank.tracker.data.TransactionEntity
import com.stitch.bank.tracker.ui.components.EmptyState
import com.stitch.bank.tracker.ui.components.parseColor
import com.stitch.bank.tracker.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class MonthlySummary(val label: String, val income: Double, val expense: Double)

private fun buildMonthlySummaries(transactions: List<TransactionEntity>): List<MonthlySummary> {
    val sdfKey = SimpleDateFormat("yyyy-MM", Locale.US)
    val sdfLabel = SimpleDateFormat("MMM", Locale("ar"))
    val grouped = transactions.groupBy { sdfKey.format(Date(it.date)) }

    return (5 downTo 0).map { offset ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -offset)
        val key = sdfKey.format(cal.time)
        val txs = grouped[key].orEmpty()
        MonthlySummary(
            label = sdfLabel.format(cal.time),
            income = txs.filter { it.isIncome }.sumOf { it.amount },
            expense = txs.filter { !it.isIncome }.sumOf { it.amount }
        )
    }
}

private fun currentMonthExpensesByCategory(transactions: List<TransactionEntity>): Map<Int?, Double> {
    val sdfKey = SimpleDateFormat("yyyy-MM", Locale.US)
    val currentKey = sdfKey.format(Date())
    return transactions
        .filter { !it.isIncome && sdfKey.format(Date(it.date)) == currentKey }
        .groupBy { it.categoryId }
        .mapValues { (_, txs) -> txs.sumOf { it.amount } }
}

@Composable
fun AnalyticsScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    budgets: List<BudgetEntity>,
    currencyCode: String
) {
    val categoryById = categories.associateBy { it.id }
    val monthly = remember(transactions) { buildMonthlySummaries(transactions) }
    val expenseByCategory = remember(transactions) { currentMonthExpensesByCategory(transactions) }
    val totalExpenseThisMonth = expenseByCategory.values.sum()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "التحليلات",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        if (transactions.isEmpty()) {
            EmptyState(message = "لا توجد بيانات كافية لعرض التحليلات بعد.")
            return@Column
        }

        SectionTitle("الوارد والصادر خلال آخر 6 أشهر")
        MonthlyBarChart(monthly)
        Legend()

        Spacer(modifier = Modifier.height(24.dp))

        SectionTitle("المصروفات حسب الفئة (هذا الشهر)")
        if (expenseByCategory.isEmpty()) {
            EmptyState(message = "لا توجد مصروفات مسجلة هذا الشهر.")
        } else {
            expenseByCategory.entries.sortedByDescending { it.value }.forEach { (categoryId, amount) ->
                val category = categoryId?.let { categoryById[it] }
                val percentage = if (totalExpenseThisMonth > 0) (amount / totalExpenseThisMonth).toFloat() else 0f
                CategoryBreakdownRow(category, amount, percentage, currencyCode)
            }
        }

        if (budgets.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("الميزانية الشهرية")
            budgets.forEach { budget ->
                val category = budget.categoryId?.let { categoryById[it] }
                val spent = if (budget.categoryId == null) totalExpenseThisMonth else expenseByCategory[budget.categoryId] ?: 0.0
                BudgetCard(budget, category, spent, currencyCode)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun MonthlyBarChart(data: List<MonthlySummary>) {
    val maxValue = (data.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0.0).takeIf { it > 0 } ?: 1.0
    val maxBarHeight = 130.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(maxBarHeight + 28.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { month ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier
                            .width(10.dp)
                            .height(maxBarHeight * (month.income / maxValue).toFloat())
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                    Box(
                        Modifier
                            .width(10.dp)
                            .height(maxBarHeight * (month.expense / maxValue).toFloat())
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.error)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(month.label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        LegendItem(color = MaterialTheme.colorScheme.secondary, label = "الوارد")
        LegendItem(color = MaterialTheme.colorScheme.error, label = "الصادر")
    }
}

@Composable
private fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CategoryBreakdownRow(category: CategoryEntity?, amount: Double, percentage: Float, currencyCode: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${category?.emoji ?: "📦"} ${category?.name ?: "غير مصنف"}", fontSize = 14.sp)
            Text(CurrencyFormatter.format(amount, currencyCode), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = percentage.coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = parseColor(category?.colorHex ?: "#757575"),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun BudgetCard(budget: BudgetEntity, category: CategoryEntity?, spent: Double, currencyCode: String) {
    val ratio = if (budget.monthlyLimit > 0) (spent / budget.monthlyLimit).toFloat().coerceIn(0f, 1f) else 0f
    val exceeded = spent > budget.monthlyLimit

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                category?.let { "${it.emoji} ${it.name}" } ?: "الميزانية الإجمالية",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = ratio,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = if (exceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "${CurrencyFormatter.format(spent, currencyCode)} من ${CurrencyFormatter.format(budget.monthlyLimit, currencyCode)}",
                fontSize = 12.sp,
                color = if (exceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (exceeded) {
                Text("⚠ تم تجاوز الميزانية الشهرية", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
