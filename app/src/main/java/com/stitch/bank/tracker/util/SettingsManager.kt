package com.stitch.bank.tracker.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom

enum class ThemeMode { LIGHT, DARK, SYSTEM }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val currencyCode: String = "SAR",
    val pinHash: String? = null,
    val pinSalt: String? = null,
    val biometricEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true
) {
    val isPinSet: Boolean get() = pinHash != null
}

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fall back to a regular (still app-private) prefs file if the keystore is unavailable.
        context.getSharedPreferences("settings_fallback", Context.MODE_PRIVATE)
    }

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun load(): AppSettings = AppSettings(
        themeMode = runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!) }
            .getOrDefault(ThemeMode.SYSTEM),
        currencyCode = prefs.getString(KEY_CURRENCY, "SAR") ?: "SAR",
        pinHash = prefs.getString(KEY_PIN_HASH, null),
        pinSalt = prefs.getString(KEY_PIN_SALT, null),
        biometricEnabled = prefs.getBoolean(KEY_BIOMETRIC, false),
        notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true)
    )

    private fun persist(update: AppSettings) {
        prefs.edit()
            .putString(KEY_THEME, update.themeMode.name)
            .putString(KEY_CURRENCY, update.currencyCode)
            .putString(KEY_PIN_HASH, update.pinHash)
            .putString(KEY_PIN_SALT, update.pinSalt)
            .putBoolean(KEY_BIOMETRIC, update.biometricEnabled)
            .putBoolean(KEY_NOTIFICATIONS, update.notificationsEnabled)
            .apply()
        _settings.value = update
    }

    fun setThemeMode(mode: ThemeMode) = persist(_settings.value.copy(themeMode = mode))

    fun setCurrency(code: String) = persist(_settings.value.copy(currencyCode = code))

    fun setNotificationsEnabled(enabled: Boolean) =
        persist(_settings.value.copy(notificationsEnabled = enabled))

    fun setBiometricEnabled(enabled: Boolean) =
        persist(_settings.value.copy(biometricEnabled = enabled))

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val saltHex = salt.joinToString("") { "%02x".format(it) }
        val hash = hashPin(pin, saltHex)
        persist(_settings.value.copy(pinHash = hash, pinSalt = saltHex))
    }

    fun clearPin() = persist(_settings.value.copy(pinHash = null, pinSalt = null, biometricEnabled = false))

    fun verifyPin(pin: String): Boolean {
        val state = _settings.value
        val salt = state.pinSalt ?: return false
        return hashPin(pin, salt) == state.pinHash
    }

    private fun hashPin(pin: String, saltHex: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(saltHex.toByteArray())
        val bytes = digest.digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_THEME = "theme_mode"
        private const val KEY_CURRENCY = "currency_code"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_BIOMETRIC = "biometric_enabled"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
    }
}
