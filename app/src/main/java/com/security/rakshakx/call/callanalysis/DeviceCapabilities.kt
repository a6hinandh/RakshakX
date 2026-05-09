package com.security.rakshakx.call.callanalysis

import android.content.Context
import android.content.SharedPreferences

/**
 * DeviceCapabilities
 *
 * Singleton to manage device-specific capability detection results.
 * Persists results in SharedPreferences.
 */
object DeviceCapabilities {

    private const val PREF_NAME = "rakshakx_device_capabilities"
    private const val KEY_SUPPORTS_CALL_RECORDING = "supports_call_recording"
    private const val KEY_HAS_RUN_PROBE = "has_run_call_recording_probe"

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Check if the call recording probe has been executed at least once.
     */
    fun hasRunProbe(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_HAS_RUN_PROBE, false)
    }

    /**
     * Mark the call recording probe as completed.
     */
    fun setHasRunProbe(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_HAS_RUN_PROBE, value).apply()
    }

    /**
     * Check if the device is likely to support call recording based on the last probe.
     */
    fun supportsCallRecording(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_SUPPORTS_CALL_RECORDING, false)
    }

    /**
     * Persist the result of the call recording capability probe.
     */
    fun setSupportsCallRecording(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_SUPPORTS_CALL_RECORDING, value).apply()
    }
}


