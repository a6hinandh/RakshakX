package com.security.rakshakx.core.network

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

data class DeviceMeta(
    val customName: String? = null,
    val isTrusted: Boolean = false
)

class NetworkDeviceStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("network_device_store", Context.MODE_PRIVATE)

    fun getDeviceMeta(macAddress: String): DeviceMeta {
        val jsonStr = prefs.getString(macAddress, null) ?: return DeviceMeta()
        return try {
            val json = JSONObject(jsonStr)
            DeviceMeta(
                customName = if (json.has("customName")) json.getString("customName") else null,
                isTrusted = json.optBoolean("isTrusted", false)
            )
        } catch (e: Exception) {
            DeviceMeta()
        }
    }

    fun setDeviceMeta(macAddress: String, meta: DeviceMeta) {
        val json = JSONObject().apply {
            meta.customName?.let { put("customName", it) }
            put("isTrusted", meta.isTrusted)
        }
        prefs.edit().putString(macAddress, json.toString()).apply()
    }
}
