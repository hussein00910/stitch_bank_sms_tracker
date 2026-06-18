package com.stitch.bank.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stitch.bank.tracker.ui.theme.primaryGradient

@Composable
fun LockScreen(
    biometricAvailable: Boolean,
    onPinEntered: (String) -> Boolean,
    onUnlocked: () -> Unit,
    onBiometricClick: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun appendDigit(digit: String) {
        if (pin.length >= 4) return
        error = false
        val next = pin + digit
        pin = next
        if (next.length == 4) {
            if (onPinEntered(next)) {
                onUnlocked()
                pin = ""
            } else {
                error = true
                pin = ""
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.linearGradient(primaryGradient()))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("مرحباً بعودتك", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            if (error) "رمز غير صحيح، حاول مرة أخرى" else "أدخل رمز القفل لفتح التطبيق",
            color = if (error) Color(0xFFFFCDD2) else Color.White.copy(alpha = 0.8f),
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (index < pin.length) Color.White else Color.White.copy(alpha = 0.3f))
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    row.forEach { digit ->
                        KeypadButton(digit) { appendDigit(digit) }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                if (biometricAvailable) {
                    KeypadIconButton(Icons.Default.Fingerprint, onClick = onBiometricClick)
                } else {
                    Spacer(modifier = Modifier.size(64.dp))
                }
                KeypadButton("0") { appendDigit("0") }
                KeypadIconButton(Icons.Default.Backspace) {
                    if (pin.isNotEmpty()) {
                        pin = pin.dropLast(1)
                        error = false
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.15f),
        contentColor = Color.White,
        modifier = Modifier.size(64.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun KeypadIconButton(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = Color.White,
        modifier = Modifier.size(64.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp))
        }
    }
}
