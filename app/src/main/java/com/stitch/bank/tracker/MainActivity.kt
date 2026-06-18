package com.stitch.bank.tracker

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.stitch.bank.tracker.data.AccountEntity
import com.stitch.bank.tracker.data.AppDatabase
import com.stitch.bank.tracker.data.BudgetEntity
import com.stitch.bank.tracker.data.CategoryEntity
import com.stitch.bank.tracker.data.TransactionEntity
import com.stitch.bank.tracker.ui.AccountsScreen
import com.stitch.bank.tracker.ui.AnalyticsScreen
import com.stitch.bank.tracker.ui.HomeScreen
import com.stitch.bank.tracker.ui.LedgerScreen
import com.stitch.bank.tracker.ui.LockScreen
import com.stitch.bank.tracker.ui.SettingsScreen
import com.stitch.bank.tracker.ui.theme.BankTrackerTheme
import com.stitch.bank.tracker.util.AppSettings
import com.stitch.bank.tracker.util.BackupManager
import com.stitch.bank.tracker.util.CsvExporter
import com.stitch.bank.tracker.util.NotificationHelper
import com.stitch.bank.tracker.util.SettingsManager
import com.stitch.bank.tracker.util.SmsTransactionProcessor
import com.stitch.bank.tracker.util.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

data class BackupFile(val uri: Uri, val name: String, val lastModified: Long)

class MainActivity : FragmentActivity() {

    private lateinit var db: AppDatabase
    private lateinit var settingsManager: SettingsManager
    private val isLockedState = mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* no-op: permissions are read lazily by the SMS provider/receiver */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = AppDatabase.getDatabase(this)
        settingsManager = SettingsManager(this)
        isLockedState.value = settingsManager.settings.value.isPinSet
        NotificationHelper.ensureChannel(this)

        permissionLauncher.launch(requiredPermissions())

        setContent {
            val transactions by db.transactionDao().getAllTransactions().collectAsState(initial = emptyList())
            val categories by db.categoryDao().getAllCategories().collectAsState(initial = emptyList())
            val budgets by db.budgetDao().getAllBudgets().collectAsState(initial = emptyList())
            val accounts by db.accountDao().getAllAccounts().collectAsState(initial = emptyList())
            val settings by settingsManager.settings.collectAsState()
            val isLocked by isLockedState
            var restoreChoices by remember { mutableStateOf<List<BackupFile>?>(null) }

            BankTrackerTheme(themeMode = settings.themeMode) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    restoreChoices?.let { files ->
                        RestoreBackupDialog(
                            files = files,
                            onDismiss = { restoreChoices = null },
                            onSelect = { file ->
                                restoreBackupFromUri(file.uri)
                                restoreChoices = null
                            }
                        )
                    }
                    if (isLocked && settings.isPinSet) {
                        LockScreen(
                            biometricAvailable = isBiometricAvailable(),
                            onPinEntered = { pin -> settingsManager.verifyPin(pin) },
                            onUnlocked = { isLockedState.value = false },
                            onBiometricClick = { showBiometricPrompt { isLockedState.value = false } }
                        )
                    } else {
                        MainScreen(
                            transactions = transactions,
                            categories = categories,
                            budgets = budgets,
                            accounts = accounts,
                            settings = settings,
                            biometricAvailable = isBiometricAvailable(),
                            onSyncClick = { onComplete -> syncSmsToDb(onComplete) },
                            onCategoryChange = { tx, categoryId ->
                                lifecycleScope.launch(Dispatchers.IO) {
                                    db.transactionDao().setCategory(tx.id, categoryId)
                                }
                            },
                            onThemeModeChange = settingsManager::setThemeMode,
                            onCurrencyChange = settingsManager::setCurrency,
                            onSetPin = settingsManager::setPin,
                            onClearPin = settingsManager::clearPin,
                            onBiometricToggle = settingsManager::setBiometricEnabled,
                            onNotificationsToggle = settingsManager::setNotificationsEnabled,
                            onAddCategory = { name, emoji, color ->
                                lifecycleScope.launch(Dispatchers.IO) {
                                    db.categoryDao().insertCategory(CategoryEntity(name = name, emoji = emoji, colorHex = color))
                                }
                            },
                            onDeleteCategory = { category ->
                                lifecycleScope.launch(Dispatchers.IO) { db.categoryDao().deleteCategory(category) }
                            },
                            onRenameAccount = { account, name ->
                                lifecycleScope.launch(Dispatchers.IO) { db.accountDao().upsert(account.copy(displayName = name)) }
                            },
                            onRecolorAccount = { account, color ->
                                lifecycleScope.launch(Dispatchers.IO) { db.accountDao().upsert(account.copy(colorHex = color)) }
                            },
                            onExportCsv = {
                                val uri = saveToDownloads(defaultExportFileName("csv"), "text/csv")
                                if (uri != null) exportCsvToUri(uri)
                                else Toast.makeText(this, "تعذر الحفظ في ذاكرة الهاتف", Toast.LENGTH_LONG).show()
                            },
                            onBackup = {
                                val uri = saveToDownloads(defaultExportFileName("json"), "application/json")
                                if (uri != null) exportBackupToUri(uri)
                                else Toast.makeText(this, "تعذر الحفظ في ذاكرة الهاتف", Toast.LENGTH_LONG).show()
                            },
                            onRestore = { restoreChoices = listBackupFiles() }
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::settingsManager.isInitialized && settingsManager.settings.value.isPinSet) {
            isLockedState.value = true
        }
    }

    private fun requiredPermissions(): Array<String> {
        val perms = mutableListOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        return perms.toTypedArray()
    }

    private fun defaultExportFileName(extension: String): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return "bank-tracker-${sdf.format(java.util.Date())}.$extension"
    }

    /** Saves a file directly into the phone's internal storage (Downloads) without any picker UI. */
    private fun saveToDownloads(fileName: String, mimeType: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists() && !dir.mkdirs()) return null
            Uri.fromFile(java.io.File(dir, fileName))
        }
    }

    /** Lists previously saved backup JSON files from the phone's Downloads folder, newest first. */
    private fun listBackupFiles(): List<BackupFile> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val results = mutableListOf<BackupFile>()
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_MODIFIED
            )
            contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
                arrayOf("bank-tracker-%.json"),
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                while (cursor.moveToNext()) {
                    val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cursor.getLong(idCol))
                    results.add(BackupFile(uri, cursor.getString(nameCol), cursor.getLong(dateCol) * 1000))
                }
            }
            results
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.listFiles { f -> f.name.startsWith("bank-tracker-") && f.name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() }
                ?.map { BackupFile(Uri.fromFile(it), it.name, it.lastModified()) }
                ?: emptyList()
        }
    }

    private fun isBiometricAvailable(): Boolean {
        val manager = BiometricManager.from(this)
        return manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
        })
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("فتح القفل")
            .setSubtitle("استخدم بصمتك أو وجهك لفتح التطبيق")
            .setNegativeButtonText("استخدام الرمز")
            .build()
        prompt.authenticate(promptInfo)
    }

    private fun syncSmsToDb(onComplete: (Int) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            var newCount = 0
            val cursor = contentResolver.query(
                Uri.parse("content://sms/inbox"),
                null, null, null, "date DESC"
            )
            cursor?.use {
                val bodyIndex = it.getColumnIndex("body")
                val addressIndex = it.getColumnIndex("address")
                val dateIndex = it.getColumnIndex("date")

                while (it.moveToNext()) {
                    val address = it.getString(addressIndex) ?: "Unknown"
                    val body = it.getString(bodyIndex) ?: ""
                    val date = it.getLong(dateIndex)

                    if (SmsTransactionProcessor.process(db, address, body, date) != null) {
                        newCount++
                    }
                }
            }
            withContext(Dispatchers.Main) {
                onComplete(newCount)
            }
        }
    }

    private fun exportCsvToUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val transactions = db.transactionDao().getAllTransactions().first()
                val categories = db.categoryDao().getAllCategories().first()
                contentResolver.openOutputStream(uri)?.use { out ->
                    CsvExporter.export(transactions, categories, out)
                }
                showToast("تم تصدير الملف بنجاح")
            } catch (e: Exception) {
                showToast("فشل تصدير الملف: ${e.message}")
            }
        }
    }

    private fun exportBackupToUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                contentResolver.openOutputStream(uri)?.use { out ->
                    BackupManager.exportBackup(db, out)
                }
                showToast("تم إنشاء النسخة الاحتياطية بنجاح")
            } catch (e: Exception) {
                showToast("فشل إنشاء النسخة الاحتياطية: ${e.message}")
            }
        }
    }

    private fun restoreBackupFromUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    BackupManager.importBackup(db, input)
                }
                showToast("تمت الاستعادة بنجاح")
            } catch (e: Exception) {
                showToast("فشلت الاستعادة: الملف غير صالح أو تالف")
            }
        }
    }

    private suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun MainScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    budgets: List<BudgetEntity>,
    accounts: List<AccountEntity>,
    settings: AppSettings,
    biometricAvailable: Boolean,
    onSyncClick: ((Int) -> Unit) -> Unit,
    onCategoryChange: (TransactionEntity, Int?) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onSetPin: (String) -> Unit,
    onClearPin: () -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onAddCategory: (String, String, String) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onRenameAccount: (AccountEntity, String) -> Unit,
    onRecolorAccount: (AccountEntity, String) -> Unit,
    onExportCsv: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (selectedTab == 0) {
                TopAppBar(
                    title = { Text("المصرف الذكي", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = {
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
                        }) {
                            Icon(Icons.Default.Sync, contentDescription = "مزامنة")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        },
        bottomBar = {
            BottomNavigationBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    transactions = transactions,
                    categories = categories,
                    currencyCode = settings.currencyCode,
                    onShowAllClick = { selectedTab = 1 }
                )
                1 -> LedgerScreen(
                    transactions = transactions,
                    categories = categories,
                    currencyCode = settings.currencyCode,
                    onCategoryChange = onCategoryChange
                )
                2 -> AnalyticsScreen(
                    transactions = transactions,
                    categories = categories,
                    budgets = budgets,
                    currencyCode = settings.currencyCode
                )
                3 -> AccountsScreen(
                    accounts = accounts,
                    transactions = transactions,
                    currencyCode = settings.currencyCode,
                    onRename = onRenameAccount,
                    onRecolor = onRecolorAccount
                )
                4 -> SettingsScreen(
                    settings = settings,
                    categories = categories,
                    biometricAvailable = biometricAvailable,
                    onThemeModeChange = onThemeModeChange,
                    onCurrencyChange = onCurrencyChange,
                    onSetPin = onSetPin,
                    onClearPin = onClearPin,
                    onBiometricToggle = onBiometricToggle,
                    onNotificationsToggle = onNotificationsToggle,
                    onAddCategory = onAddCategory,
                    onDeleteCategory = onDeleteCategory,
                    onExportCsv = onExportCsv,
                    onBackup = onBackup,
                    onRestore = onRestore
                )
            }
        }
    }
}

@Composable
fun RestoreBackupDialog(
    files: List<BackupFile>,
    onDismiss: () -> Unit,
    onSelect: (BackupFile) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختر نسخة احتياطية") },
        text = {
            if (files.isEmpty()) {
                Text("لا توجد نسخة احتياطية محفوظة في ذاكرة الهاتف (مجلد التنزيلات).")
            } else {
                val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
                LazyColumn {
                    items(files) { file ->
                        Text(
                            text = "${file.name}\n${sdf.format(java.util.Date(file.lastModified))}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(file) }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}

@Composable
fun BottomNavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "الرئيسية") },
            label = { Text("الرئيسية") },
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "السجل") },
            label = { Text("السجل") },
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.PieChart, contentDescription = "التحليلات") },
            label = { Text("التحليلات") },
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountBalance, contentDescription = "الحسابات") },
            label = { Text("الحسابات") },
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "الإعدادات") },
            label = { Text("الإعدادات") },
            selected = selectedTab == 4,
            onClick = { onTabSelected(4) }
        )
    }
}
