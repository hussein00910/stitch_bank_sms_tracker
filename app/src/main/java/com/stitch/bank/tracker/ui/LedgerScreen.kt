package com.stitch.bank.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stitch.bank.tracker.data.CategoryEntity
import com.stitch.bank.tracker.data.TransactionEntity
import com.stitch.bank.tracker.ui.components.EmptyState
import com.stitch.bank.tracker.ui.components.FilterChipItem
import com.stitch.bank.tracker.ui.components.TransactionItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LedgerScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currencyCode: String,
    onCategoryChange: (TransactionEntity, Int?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(0) } // 0: All, 1: Income, 2: Expense
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) } // null = all categories
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    val categoryById = categories.associateBy { it.id }

    val filteredTransactions = remember(transactions, searchQuery, selectedFilter, selectedCategoryId) {
        transactions.filter {
            val matchesSearch = it.sender.contains(searchQuery, ignoreCase = true) ||
                    it.body.contains(searchQuery, ignoreCase = true) ||
                    (it.merchant?.contains(searchQuery, ignoreCase = true) == true)
            val matchesFilter = when (selectedFilter) {
                1 -> it.isIncome
                2 -> !it.isIncome
                else -> true
            }
            val matchesCategory = selectedCategoryId == null || it.categoryId == selectedCategoryId
            matchesSearch && matchesFilter && matchesCategory
        }
    }

    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { transaction ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dateStr = sdf.format(Date(transaction.date))

            val todayStr = sdf.format(Date())
            val yesterdayStr = sdf.format(Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))

            when (dateStr) {
                todayStr -> "اليوم"
                yesterdayStr -> "أمس"
                else -> {
                    val displaySdf = SimpleDateFormat("d MMMM yyyy", Locale("ar"))
                    displaySdf.format(Date(transaction.date))
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            "سجل المعاملات",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("بحث عن جهة الاتصال أو تفاصيل المعاملة...", color = Color.Gray, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipItem(title = "الكل", selected = selectedFilter == 0, onClick = { selectedFilter = 0 })
            FilterChipItem(title = "الوارد", selected = selectedFilter == 1, onClick = { selectedFilter = 1 })
            FilterChipItem(title = "الصادر", selected = selectedFilter == 2, onClick = { selectedFilter = 2 })
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChipItem(title = "كل الفئات", selected = selectedCategoryId == null, onClick = { selectedCategoryId = null })
            }
            items(categories) { category ->
                FilterChipItem(
                    title = "${category.emoji} ${category.name}",
                    selected = selectedCategoryId == category.id,
                    onClick = { selectedCategoryId = category.id }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (groupedTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                EmptyState(message = "لا توجد معاملات مطابقة لخيارات البحث أو الفلترة.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                groupedTransactions.forEach { (dateHeader, transactionsForDate) ->
                    item {
                        Text(
                            text = dateHeader,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                        )
                    }
                    items(transactionsForDate) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            category = transaction.categoryId?.let { categoryById[it] },
                            currencyCode = currencyCode,
                            onClick = { editingTransaction = transaction }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    editingTransaction?.let { transaction ->
        AlertDialog(
            onDismissRequest = { editingTransaction = null },
            title = { Text("تصنيف المعاملة") },
            text = {
                Column {
                    categories.forEach { category ->
                        TextButton(onClick = {
                            onCategoryChange(transaction, category.id)
                            editingTransaction = null
                        }) {
                            Text("${category.emoji}  ${category.name}")
                        }
                    }
                    TextButton(onClick = {
                        onCategoryChange(transaction, null)
                        editingTransaction = null
                    }) {
                        Text("بلا تصنيف", color = Color.Gray)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editingTransaction = null }) { Text("إغلاق") }
            }
        )
    }
}
