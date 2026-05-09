package com.security.rakshakx.permissions

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionManager {
    private val corePermissions = mutableListOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_PHONE_STATE
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun hasCorePermissions(context: Context): Boolean {
        return corePermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun corePermissionsForRuntimeRequest(): Array<String> = corePermissions.toTypedArray()

    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
    }

    fun isAccessibilityEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return flat.split(':').any { entry ->
            ComponentName.unflattenFromString(entry)?.packageName == context.packageName
        }
    }

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return flat.split(':').any { entry ->
            ComponentName.unflattenFromString(entry)?.packageName == context.packageName
        }
    }

    fun isVpnPrepared(context: Context): Boolean {
        return VpnService.prepare(context) == null
    }

    data class ReadinessState(
        val corePermissionsGranted: Boolean,
        val notificationListenerEnabled: Boolean,
        val accessibilityEnabled: Boolean,
        val smsReady: Boolean,
        val callReady: Boolean,
        val emailReady: Boolean,
        val webReady: Boolean,
        val minimumDashboardReady: Boolean
    )

    fun getReadinessState(context: Context): ReadinessState {
        return buildReadinessState(
            corePermissionsGranted = hasCorePermissions(context),
            notificationListenerEnabled = isNotificationListenerEnabled(context),
            accessibilityEnabled = isAccessibilityEnabled(context),
            receiveSmsGranted = hasPermission(context, Manifest.permission.RECEIVE_SMS),
            readSmsGranted = hasPermission(context, Manifest.permission.READ_SMS),
            readCallLogGranted = hasPermission(context, Manifest.permission.READ_CALL_LOG),
            readPhoneStateGranted = hasPermission(context, Manifest.permission.READ_PHONE_STATE),
            recordAudioGranted = hasPermission(context, Manifest.permission.RECORD_AUDIO),
            postNotificationsGranted = hasNotificationPermission(context)
        )
    }

    internal fun buildReadinessState(
        corePermissionsGranted: Boolean,
        notificationListenerEnabled: Boolean,
        accessibilityEnabled: Boolean,
        receiveSmsGranted: Boolean,
        readSmsGranted: Boolean,
        readCallLogGranted: Boolean,
        readPhoneStateGranted: Boolean,
        recordAudioGranted: Boolean,
        postNotificationsGranted: Boolean
    ): ReadinessState {
        val smsReady = receiveSmsGranted && readSmsGranted && notificationListenerEnabled
        val callReady = readCallLogGranted && readPhoneStateGranted && recordAudioGranted && postNotificationsGranted
        val emailReady = notificationListenerEnabled
        val webReady = accessibilityEnabled

        return ReadinessState(
            corePermissionsGranted = corePermissionsGranted,
            notificationListenerEnabled = notificationListenerEnabled,
            accessibilityEnabled = accessibilityEnabled,
            smsReady = smsReady,
            callReady = callReady,
            emailReady = emailReady,
            webReady = webReady,
            minimumDashboardReady = corePermissionsGranted && notificationListenerEnabled && accessibilityEnabled
        )
    }

    fun getMissingOnboardingRequirements(context: Context): List<String> {
        val state = getReadinessState(context)
        val missing = mutableListOf<String>()
        if (!state.corePermissionsGranted) missing += "Core runtime permissions"
        if (!state.notificationListenerEnabled) missing += "Notification access"
        if (!state.accessibilityEnabled) missing += "Accessibility service"
        return missing
    }
}
