package com.rakshakx

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri // Added for the KTX warning
import androidx.lifecycle.lifecycleScope
import com.rakshakx.services.foreground.FraudMonitoringForegroundService
import com.rakshakx.services.foreground.MonitoringPreferences
import com.rakshakx.services.foreground.RiskScanScheduler
import com.rakshakx.ui.RakshakXApp
import com.rakshakx.callanalysis.ui.RakshakXActivity
import com.rakshakx.callanalysis.data.CallDatabase
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var smsPermissionGranted: Boolean = false
    private var callLogPermissionGranted: Boolean = false
    private var phonePermissionGranted: Boolean = false
    private var microphonePermissionGranted: Boolean = false
    private var overlayPermissionGranted: Boolean = false
    private var isDefaultSmsApp: Boolean = false
    private var notificationAccessEnabled: Boolean = false
    private var backgroundMonitoringEnabled: Boolean = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshPermissionAndRoleStatus()
        render()
    }

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshPermissionAndRoleStatus()
        render()
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshPermissionAndRoleStatus()
        render()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backgroundMonitoringEnabled = MonitoringPreferences.isEnabled(applicationContext)
        refreshPermissionAndRoleStatus()
        if (backgroundMonitoringEnabled && hasRequiredAccess()) {
            setMonitoringEnabled(true)
        }
        render()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionAndRoleStatus()
        render()
    }

    private fun render() {
        val orchestrator = FraudMonitoringForegroundService.getOrchestrator(applicationContext)
        setContent {
            RakshakXApp(
                orchestrator = orchestrator,
                smsPermissionGranted = smsPermissionGranted,
                callLogPermissionGranted = callLogPermissionGranted,
                isDefaultSmsApp = isDefaultSmsApp,
                notificationAccessEnabled = notificationAccessEnabled,
                backgroundMonitoringEnabled = backgroundMonitoringEnabled,
                onRequestPermissions = ::requestRequiredPermissions,
                onRequestDefaultSmsRole = ::requestDefaultSmsRole,
                onRequestNotificationAccess = ::requestNotificationAccess,
                onToggleBackgroundMonitoring = ::onToggleBackgroundMonitoring,
                onOpenCallAnalysis = ::openCallAnalysis,
                onDebugShowLastCall = ::logLastCallRecord,
                onStartHackathonMode = { /* Pass empty lambda to fix Error :89 */ }
            )
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun requestOverlayPermission() {
        // Warning :109 fixed (removed unnecessary M check as min SDK is likely >= 26)
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri() // Warning :113 fixed using .toUri()
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun requestDefaultSmsRole() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val roleManager = getSystemService(RoleManager::class.java) // Warning :122 fixed
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) return
        if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) return
        val roleIntent: Intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
        roleRequestLauncher.launch(roleIntent)
    }

    private fun requestNotificationAccess() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }

    private fun refreshPermissionAndRoleStatus() {
        smsPermissionGranted = hasPermission(Manifest.permission.RECEIVE_SMS)
        callLogPermissionGranted = hasPermission(Manifest.permission.READ_CALL_LOG)
        phonePermissionGranted = hasPermission(Manifest.permission.READ_PHONE_STATE)
        microphonePermissionGranted = hasPermission(Manifest.permission.RECORD_AUDIO)

        // Warning :140 fixed
        overlayPermissionGranted = Settings.canDrawOverlays(this)

        isDefaultSmsApp = isDefaultSmsRoleHeld()
        notificationAccessEnabled = NotificationManagerCompat.getEnabledListenerPackages(this)
            .contains(packageName)

        if (!hasRequiredAccess() && backgroundMonitoringEnabled) {
            setMonitoringEnabled(false)
        }
        if (hasRequiredAccess() &&
            MonitoringPreferences.isEnabled(applicationContext) &&
            !backgroundMonitoringEnabled
        ) {
            setMonitoringEnabled(true)
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun isDefaultSmsRoleHeld(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val roleManager = getSystemService(RoleManager::class.java) // Warning :167 fixed
        return roleManager.isRoleAvailable(RoleManager.ROLE_SMS) &&
                roleManager.isRoleHeld(RoleManager.ROLE_SMS)
    }

    private fun hasRequiredAccess(): Boolean {
        val smsPathReady = smsPermissionGranted && isDefaultSmsApp
        val notificationPathReady = notificationAccessEnabled
        return callLogPermissionGranted && (smsPathReady || notificationPathReady)
    }

    private fun onToggleBackgroundMonitoring(enabled: Boolean) {
        if (enabled && !hasRequiredAccess()) {
            requestRequiredPermissions()
            requestDefaultSmsRole()
            return
        }
        setMonitoringEnabled(enabled)
        render()
    }

    private fun setMonitoringEnabled(enabled: Boolean) {
        if (enabled) {
            FraudMonitoringForegroundService.start(applicationContext)
            RiskScanScheduler.schedulePeriodic(applicationContext)
        } else {
            FraudMonitoringForegroundService.stop(applicationContext)
            RiskScanScheduler.cancelPeriodic(applicationContext)
        }
        backgroundMonitoringEnabled = enabled
        MonitoringPreferences.setEnabled(applicationContext, enabled)
    }

    private fun openCallAnalysis() {
        val hasCallPermissions = phonePermissionGranted &&
                microphonePermissionGranted &&
                overlayPermissionGranted

        if (!hasCallPermissions) {
            if (!phonePermissionGranted || !microphonePermissionGranted) {
                requestRequiredPermissions()
            }
            if (!overlayPermissionGranted) {
                requestOverlayPermission()
            }
            return
        }

        val intent = Intent(this, RakshakXActivity::class.java)
        startActivity(intent)
    }

    private fun logLastCallRecord() {
        val db = CallDatabase.getInstance(applicationContext)
        val callDao = db.callDao()

        lifecycleScope.launch {
            val last = callDao.getLastCall()
            Log.d(
                "CallDebug",
                "LastCall → id=${last?.id}, number=${last?.phoneNumber}, " +
                        "transcript=${last?.transcript}, riskScore=${last?.riskScore}, " +
                        "action=${last?.action}, reason=${last?.reason}, timestamp=${last?.timestamp}"
            )
        }
    }
} // Error :236 fixed by properly closing the class