package com.stitch.bank.tracker

import android.Manifest
import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.stitch.bank.tracker.data.AppDatabase
import com.stitch.bank.tracker.data.TransactionEntity
import com.stitch.bank.tracker.util.TransactionParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// الالوان المستوحاة من تصميم Equitas Modern (Architectural Trust)
val PrimaryBlue = Color(0xFF00346F)
val SecondaryGreen = Color(0xFF006C47)
val ErrorRed = Color(0xFFBA1A1A)
val BackgroundGray = Color(0xFFF7F9FB)
val SurfaceWhite = Color(0xFFFFFFFF)

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val db = AppDatabase.getDatabase(this)
        val dao = db.transactionDao()

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.READ_SMS] == true) {
                // المزامنة الأولية عند الطلب
            }
        }

        permissionLauncher.launch(arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
        ))

        setContent {
            val transactions by dao.getAllTransactions().collectAsState(initial = emptyList())
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MainScreen(
                    transactions = transactions,
                    onSyncClick = { onComplete -> syncSmsToDb(onComplete) }
                )
            }
        }
    }

    private fun syncSmsToDb(onComplete: (Int) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            var newCount = 0
            val cursor = contentResolver.query(
                Uri.parse("content://sms/inbox"),
                null, null, null, "date DESC"
            )

            val db = AppDatabase.getDatabase(this@MainActivity)
            val dao = db.transactionDao()

            cursor?.use {
                val bodyIndex = it.getColumnIndex("body")
                val addressIndex = it.getColumnIndex("address")
                val dateIndex = it.getColumnIndex("date")

                while (it.moveToNext()) {
                    val address = it.getString(addressIndex) ?: "Unknown"
                    val body = it.getString(bodyIndex) ?: ""
                    val date = it.getLong(dateIndex)

                    if (TransactionParser.isBankMessage(body)) {
                        val amount = TransactionParser.extractAmount(body)
                        val isIncome = TransactionParser.isIncome(body)
                        
                        if (!dao.exists(body, date)) {
                            dao.insertTransaction(
                                TransactionEntity(
                                    sender = address,
                                    body = body,
                                    amount = amount,
                                    date = date,
                                    isIncome = isIncome
                                )
                            )
                            newCount++
                        }
                    }
                }
            }
            CoroutineScope(Dispatchers.Main).launch {
                onComplete(newCount)
            }
        }
    }
}

@Composable
fun MainScreen(
    transactions: List<TransactionEntity>,
    onSyncClick: ((Int) -> Unit) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    if (tab == 2) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("جاري جلب المعاملات الجديدة...")
                        }
                        onSyncClick { count ->
                            coroutineScope.launch {
                                if (count > 0) {
                                    snackbarHostState.showSnackbar("تمت المزامنة بنجاح! تم استيراد $count من المعاملات الجديدة.")
                                } else {
                                    snackbarHostState.showSnackbar("المعاملات محدثة بالفعل. لم يتم العثور على حركات جديدة.")
                                }
                            }
                        }
                    } else {
                        selectedTab = tab
                    }
                }
            )
        },
        containerColor = BackgroundGray
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    transactions = transactions,
                    onShowAllClick = { selectedTab = 1 }
                )
                1 -> LedgerScreen(
                    transactions = transactions
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    transactions: List<TransactionEntity>,
    onShowAllClick: () -> Unit
) {
    val totalBalance = transactions.sumOf { if (it.isIncome) it.amount else -it.amount }
    val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
    val recentTransactions = transactions.take(5)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { HeaderSection() }
        item { BalanceCard(totalBalance) }
        item { StatsSection(totalIncome, totalExpense) }
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
                    color = PrimaryBlue
                )
                TextButton(onClick = onShowAllClick) {
                    Text("عرض الكل", color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (recentTransactions.isEmpty()) {
            item {
                EmptyState(message = "لا توجد معاملات بعد. انقر على مزامنة لجلب الحركات المالية.")
            }
        } else {
            items(recentTransactions) { transaction ->
                TransactionItem(transaction)
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun LedgerScreen(transactions: List<TransactionEntity>) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(0) } // 0: All, 1: Income, 2: Expense

    val filteredTransactions = remember(transactions, searchQuery, selectedFilter) {
        transactions.filter {
            val matchesSearch = it.sender.contains(searchQuery, ignoreCase = true) ||
                    it.body.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                1 -> it.isIncome
                2 -> !it.isIncome
                else -> true
            }
            matchesSearch && matchesFilter
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
            color = PrimaryBlue,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("بحث عن جهة الاتصال أو تفاصيل المعاملة...", color = Color.Gray, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryBlue) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWhite, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                focusedContainerColor = SurfaceWhite,
                unfocusedContainerColor = SurfaceWhite
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

        Spacer(modifier = Modifier.height(16.dp))

        if (groupedTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
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
                        TransactionItem(transaction)
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun FilterChipItem(title: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9999.dp),
        color = if (selected) PrimaryBlue else SurfaceWhite,
        contentColor = if (selected) SurfaceWhite else Color.Gray,
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
fun EmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ReceiptLong,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
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
            Text("مرحباً بك", fontSize = 14.sp, color = Color.Gray)
            Text("المصرف الذكي", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(PrimaryBlue, Color(0xFF005691))
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("م", color = SurfaceWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun BalanceCard(balance: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryBlue)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(PrimaryBlue, Color(0xFF005691)),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
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
                            color = SurfaceWhite.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Light
                        )
                        Text(
                            "المحفظة الرقمية",
                            color = SurfaceWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(SurfaceWhite.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = SurfaceWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Text(
                        "إجمالي الرصيد المتوفر",
                        color = SurfaceWhite.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val formattedBalance = String.format(Locale.US, "%,.2f", balance)
                        val parts = formattedBalance.split(".")
                        Text(
                            parts[0],
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SurfaceWhite
                        )
                        Text(
                            "." + parts[1] + " ر.س",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SurfaceWhite.copy(alpha = 0.9f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatsSection(income: Double, expense: Double) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("الوارد", income, SecondaryGreen, Icons.Default.ArrowDownward, Modifier.weight(1f))
        StatCard("الصادر", expense, ErrorRed, Icons.Default.ArrowUpward, Modifier.weight(1f))
    }
}

@Composable
fun StatCard(title: String, amount: Double, color: Color, icon: ImageVector, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontSize = 12.sp, color = Color.Gray)
            Text(String.format(Locale.US, "%,.2f ر.س", amount), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
        }
    }
}

@Composable
fun TransactionItem(transaction: TransactionEntity) {
    val sdf = SimpleDateFormat("hh:mm a", Locale("ar"))
    val dateString = sdf.format(Date(transaction.date))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(BackgroundGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (transaction.isIncome) Icons.Default.Payments else Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = PrimaryBlue
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.sender, fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 16.sp)
                Text(dateString, fontSize = 12.sp, color = Color.Gray)
            }
            Text(
                (if (transaction.isIncome) "+ " else "- ") + String.format(Locale.US, "%.2f", transaction.amount) + " ر.س",
                fontWeight = FontWeight.Bold,
                color = if (transaction.isIncome) SecondaryGreen else ErrorRed,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun BottomNavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar(containerColor = SurfaceWhite) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Dashboard, "Dashboard") }, 
            label = { Text("الرئيسية") }, 
            selected = selectedTab == 0, 
            onClick = { onTabSelected(0) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.ReceiptLong, "Ledger") }, 
            label = { Text("السجل") }, 
            selected = selectedTab == 1, 
            onClick = { onTabSelected(1) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Sync, "Sync") }, 
            label = { Text("مزامنة") }, 
            selected = false, 
            onClick = { onTabSelected(2) }
        )
    }
}
