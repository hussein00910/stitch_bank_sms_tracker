package com.stitch.bank.tracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.stitch.bank.tracker.util.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF00346F),
    secondary = Color(0xFF006C47),
    error = Color(0xFFBA1A1A),
    background = Color(0xFFF7F9FB),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE7ECF1)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4E8),
    secondary = Color(0xFF6FCB9F),
    error = Color(0xFFFF8A80),
    background = Color(0xFF101418),
    surface = Color(0xFF1A1F24),
    onPrimary = Color(0xFF00234A),
    onSecondary = Color(0xFF00391F),
    onBackground = Color(0xFFE3E5E8),
    onSurface = Color(0xFFE3E5E8),
    surfaceVariant = Color(0xFF262C32)
)

@Composable
fun BankTrackerTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}

/** Two-stop gradient derived from the current primary color, used for hero/balance cards. */
@Composable
fun primaryGradient(): List<Color> {
    val primary = MaterialTheme.colorScheme.primary
    return listOf(primary, lerp(primary, Color.White, 0.18f))
}
