package com.stitch.bank.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stitch.bank.tracker.data.CategoryEntity
import com.stitch.bank.tracker.ui.components.parseColor
import com.stitch.bank.tracker.util.AppSettings
import com.stitch.bank.tracker.util.CurrencyFormatter
import com.stitch.bank.tracker.util.ThemeMode

private val categoryColorPalette = listOf(
    "#006C47", "#BA1A1A", "#7A4F01", "#005691", "#7B1FA2", "#455A64", "#00346F", "#757575"
)

@Composable
fun SettingsScreen(
    settings: AppSettings,
    categories: List<CategoryEntity>,
    biometricAvailable: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onSetPin: (String) -> Unit,
    onClearPin: () -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onAddCategory: (String, String, String) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onExportCsv: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "الإعدادات",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        SettingsSectionTitle("المظهر")
        SettingsCard {
            ThemeModeRow(settings.themeMode, onThemeModeChange)
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            SettingsRow(
                title = "العملة",
                value = "${settings.currencyCode} (${CurrencyFormatter.symbolFor(settings.currencyCode)})",
                onClick = { showCurrencyDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        SettingsSectionTitle("الأمان")
        SettingsCard {
            SettingsRow(
                title = if (settings.isPinSet) "تغيير رمز القفل" else "تفعيل رمز القفل",
                value = if (settings.isPinSet) "مفعّل" else "غير مفعّل",
                onClick = { showPinDialog = true }
            )
            if (settings.isPinSet) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsRow(title = "إلغاء رمز القفل", value = "", onClick = onClearPin)
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsToggleRow(
                    title = "فتح بالبصمة / الوجه",
                    checked = settings.biometricEnabled && biometricAvailable,
                    enabled = biometricAvailable,
                    onCheckedChange = onBiometricToggle
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        SettingsSectionTitle("الإشعارات")
        SettingsCard {
            SettingsToggleRow(
                title = "إشعار فوري عند معاملة جديدة",
                checked = settings.notificationsEnabled,
                enabled = true,
                onCheckedChange = onNotificationsToggle
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        SettingsSectionTitle("الفئات")
        SettingsCard {
            categories.forEachIndexed { index, category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(category.emoji, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(category.name, fontSize = 14.sp)
                    }
                    IconButton(onClick = { onDeleteCategory(category) }) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                    }
                }
                if (index != categories.lastIndex) {
                    Divider()
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { showAddCategoryDialog = true }) {
                Text("+ إضافة فئة جديدة")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        SettingsSectionTitle("البيانات")
        SettingsCard {
            SettingsRow(title = "تصدير المعاملات (CSV)", value = "", onClick = onExportCsv)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsRow(title = "نسخ احتياطي كامل (JSON)", value = "", onClick = onBackup)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsRow(title = "استعادة من نسخة احتياطية", value = "", onClick = onRestore)
        }

        Spacer(modifier = Modifier.height(20.dp))
        SettingsSectionTitle("حول التطبيق")
        SettingsCard {
            Text("المصرف الذكي", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "تطبيق مفتوح المصدر لتتبع حركاتك البنكية من رسائل SMS مباشرة على جهازك، بدون اتصال بالإنترنت.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("اختر العملة") },
            text = {
                Column {
                    CurrencyFormatter.supportedCurrencies.forEach { code ->
                        TextButton(onClick = {
                            onCurrencyChange(code)
                            showCurrencyDialog = false
                        }) {
                            Text("$code  (${CurrencyFormatter.symbolFor(code)})")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) { Text("إغلاق") }
            }
        )
    }

    if (showPinDialog) {
        PinSetupDialog(
            onDismiss = { showPinDialog = false },
            onConfirm = { pin ->
                onSetPin(pin)
                showPinDialog = false
            }
        )
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name, emoji, color ->
                onAddCategory(name, emoji, color)
                showAddCategoryDialog = false
            }
        )
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SettingsRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 14.sp)
        if (value.isNotEmpty()) {
            Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsToggleRow(title: String, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontSize = 14.sp,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun ThemeModeRow(current: ThemeMode, onChange: (ThemeMode) -> Unit) {
    Column {
        Text("المظهر", fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeModeChip("النظام", ThemeMode.SYSTEM, current, onChange)
            ThemeModeChip("فاتح", ThemeMode.LIGHT, current, onChange)
            ThemeModeChip("داكن", ThemeMode.DARK, current, onChange)
        }
    }
}

@Composable
private fun ThemeModeChip(label: String, mode: ThemeMode, current: ThemeMode, onChange: (ThemeMode) -> Unit) {
    val selected = current == mode
    Surface(
        onClick = { onChange(mode) },
        shape = RoundedCornerShape(9999.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PinSetupDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعيين رمز قفل (4 أرقام)") },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pin = it },
                    label = { Text("الرمز") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) confirmPin = it },
                    label = { Text("تأكيد الرمز") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                if (pin.isNotEmpty() && confirmPin.isNotEmpty() && pin != confirmPin) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("الرمزان غير متطابقين", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = pin.length == 4 && pin == confirmPin,
                onClick = { onConfirm(pin) }
            ) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun AddCategoryDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("📦") }
    var color by remember { mutableStateOf(categoryColorPalette.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("فئة جديدة") },
        text = {
            Column {
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { if (it.length <= 2) emoji = it },
                    label = { Text("رمز تعبيري") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الفئة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    categoryColorPalette.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(parseColor(hex))
                                .clickable { color = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim(), emoji.ifBlank { "📦" }, color) }
            ) { Text("إضافة") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
