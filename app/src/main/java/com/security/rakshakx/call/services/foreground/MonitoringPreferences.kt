package com.security.rakshakx.call.services.foreground

import android.content.Context

object MonitoringPreferences {
    private const val PREFS_NAME = "rakshakx_prefs"
    private const val KEY_BACKGROUND_MONITORING_ENABLED = "background_monitoring_enabled"

    fun isEnabled(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BACKGROUND_MONITORING_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BACKGROUND_MONITORING_ENABLED, enabled).apply()
    }
}


