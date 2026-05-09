package com.security.rakshakx.call.callanalysis

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * PermissionManager
 *
 * Centralized permission handling with fallback modes.
 * Detects missing permissions and guides user to grant them.
 */
class PermissionManager(private val context: Context) {

    companion object {
        const val REQUEST_CODE_PERMISSIONS = 1001
        const val REQUEST_CODE_OVERLAY = 1002

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )
    }

    /**
     * Check if all required permissions are granted.
     */
    fun hasAllPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if overlay permission is granted (required for floating widget).
     */
    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // Not required on older versions
        }
    }

    /**
     * Request runtime permissions from Activity.
     */
    fun requestPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            REQUIRED_PERMISSIONS,
            REQUEST_CODE_PERMISSIONS
        )
    }

    /**
     * Request overlay permission (opens system settings).
     */
    fun requestOverlayPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            activity.startActivityForResult(intent, REQUEST_CODE_OVERLAY)
        }
    }

    /**
     * Get missing permissions as human-readable list.
     */
    fun getMissingPermissions(): List<String> {
        val missing = mutableListOf<String>()

        REQUIRED_PERMISSIONS.forEach { permission ->
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                missing.add(getPermissionName(permission))
            }
        }

        if (!hasOverlayPermission()) {
            missing.add("Display over other apps")
        }

        return missing
    }

    /**
     * Convert permission constant to human-readable name.
     */
    private fun getPermissionName(permission: String): String {
        return when (permission) {
            Manifest.permission.READ_PHONE_STATE -> "Phone State"
            Manifest.permission.RECORD_AUDIO -> "Microphone/Call Recording"
            Manifest.permission.POST_NOTIFICATIONS -> "Notifications"
            else -> permission
        }
    }

    /**
     * Determine available operation mode based on permissions.
     */
    fun getOperationMode(): OperationMode {
        val hasPhoneState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val hasRecordAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val hasOverlay = hasOverlayPermission()

        return when {
            hasPhoneState && hasRecordAudio && hasOverlay -> OperationMode.FULL_CALL_ANALYSIS
            hasPhoneState && hasOverlay -> OperationMode.WIDGET_ONLY // Can show widget but not record
            hasRecordAudio -> OperationMode.MANUAL_RECORDING // User must manually trigger
            else -> OperationMode.SMS_WEB_ONLY // Fallback to SMS/web analysis only
        }
    }

    /**
     * Operation modes based on available permissions.
     */
    enum class OperationMode {
        FULL_CALL_ANALYSIS,    // All permissions: auto-detect calls + record + widget
        WIDGET_ONLY,            // No recording: show widget but can't analyze audio
        MANUAL_RECORDING,       // No phone state: user must manually start recording
        SMS_WEB_ONLY           // No call features: only SMS and web link analysis
    }
}

