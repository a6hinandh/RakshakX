package com.security.rakshakx.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("rakshakx_settings", Context.MODE_PRIVATE)

    companion object {
        private var instance: SettingsStore? = null
        fun getInstance(context: Context): SettingsStore {
            return instance ?: synchronized(this) {
                instance ?: SettingsStore(context.applicationContext).also { instance = it }
            }
        }
    }

    // Keys
    private val KEY_SMS_ENABLED = "sms_protection_enabled"
    private val KEY_CALL_ENABLED = "call_protection_enabled"
    private val KEY_EMAIL_ENABLED = "email_protection_enabled"
    private val KEY_WEB_ENABLED = "web_protection_enabled"
    private val KEY_AUTO_DELETE_DAYS = "auto_delete_days"
    private val KEY_SENSITIVITY = "protection_sensitivity"

    // Flows for reactive UI updates
    private val _smsEnabled = MutableStateFlow(prefs.getBoolean(KEY_SMS_ENABLED, true))
    val smsEnabled: StateFlow<Boolean> = _smsEnabled

    private val _callEnabled = MutableStateFlow(prefs.getBoolean(KEY_CALL_ENABLED, true))
    val callEnabled: StateFlow<Boolean> = _callEnabled

    private val _emailEnabled = MutableStateFlow(prefs.getBoolean(KEY_EMAIL_ENABLED, true))
    val emailEnabled: StateFlow<Boolean> = _emailEnabled

    private val _webEnabled = MutableStateFlow(prefs.getBoolean(KEY_WEB_ENABLED, true))
    val webEnabled: StateFlow<Boolean> = _webEnabled

    private val _autoDeleteDays = MutableStateFlow(prefs.getInt(KEY_AUTO_DELETE_DAYS, 30))
    val autoDeleteDays: StateFlow<Int> = _autoDeleteDays

    private val _sensitivity = MutableStateFlow(prefs.getFloat(KEY_SENSITIVITY, 0.5f))
    val sensitivity: StateFlow<Float> = _sensitivity

    fun setSmsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SMS_ENABLED, enabled).apply()
        _smsEnabled.value = enabled
    }

    fun setCallEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CALL_ENABLED, enabled).apply()
        _callEnabled.value = enabled
    }

    fun setEmailEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EMAIL_ENABLED, enabled).apply()
        _emailEnabled.value = enabled
    }

    fun setWebEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WEB_ENABLED, enabled).apply()
        _webEnabled.value = enabled
    }

    fun setAutoDeleteDays(days: Int) {
        prefs.edit().putInt(KEY_AUTO_DELETE_DAYS, days).apply()
        _autoDeleteDays.value = days
    }

    fun setSensitivity(value: Float) {
        prefs.edit().putFloat(KEY_SENSITIVITY, value).apply()
        _sensitivity.value = value
    }
}
