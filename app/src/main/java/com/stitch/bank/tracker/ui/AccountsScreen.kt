package com.stitch.bank.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stitch.bank.tracker.data.AccountEntity
import com.stitch.bank.tracker.data.TransactionEntity
import com.stitch.bank.tracker.ui.components.EmptyState
import com.stitch.bank.tracker.ui.components.parseColor
import com.stitch.bank.tracker.util.CurrencyFormatter

private val accountColorPalette = listOf(
    "#00346F", "#006C47", "#BA1A1A", "#7A4F01", "#7B1FA2", "#455A64", "#005691", "#00838F"
)

@Composable
fun AccountsScreen(
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    currencyCode: String,
    onRename: (AccountEntity, String) -> Unit,
    onRecolor: (AccountEntity, String) -> Unit
) {
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    val transactionsBySender = remember(transactions) { transactions.groupBy { it.sender } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            "الحسابات",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        if (accounts.isEmpty()) {
            EmptyState(message = "لا توجد حسابات بعد. قم بالمزامنة لاكتشاف الحسابات البنكية من رسائلك.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(accounts) { account ->
                    val accountTransactions = transactionsBySender[account.senderKey].orEmpty()
                    AccountCard(
                        account = account,
                        transactionCount = accountTransactions.size,
                        balance = accountTransactions.sumOf { if (it.isIncome) it.amount else -it.amount },
                        currencyCode = currencyCode,
                        onEditClick = { editingAccount = account }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    editingAccount?.let { account ->
        EditAccountDialog(
            account = account,
            onDismiss = { editingAccount = null },
            onConfirm = { name, color ->
                if (name != account.displayName) onRename(account, name)
                if (color != account.colorHex) onRecolor(account, color)
                editingAccount = null
            }
        )
    }
}

@Composable
private fun AccountCard(
    account: AccountEntity,
    transactionCount: Int,
    balance: Double,
    currencyCode: String,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(parseColor(account.colorHex)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    account.displayName.take(1),
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(account.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "$transactionCount معاملة",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    CurrencyFormatter.format(balance, currencyCode),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (balance >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                )
            }
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EditAccountDialog(
    account: AccountEntity,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var nameInput by remember(account.senderKey) { mutableStateOf(account.displayName) }
    var colorInput by remember(account.senderKey) { mutableStateOf(account.colorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل الحساب") },
        text = {
            Column {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("اسم الحساب") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("اللون", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    accountColorPalette.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parseColor(hex))
                                .then(
                                    if (hex == colorInput)
                                        Modifier.background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.25f), CircleShape)
                                    else Modifier
                                )
                                .clickable { colorInput = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = nameInput.isNotBlank(),
                onClick = { onConfirm(nameInput.trim(), colorInput) }
            ) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
