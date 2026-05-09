package com.security.rakshakx.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.security.rakshakx.startup.AppStartupCoordinator

/**
 * BootReceiver — UNIFIED boot receiver for all channels.
 *
 * Handles:
 *   1. SMS channel: reschedules the SMS polling WorkManager job after reboot
 *   2. Call channel: restarts FraudMonitoringForegroundService and periodic scans
 *
 * WorkManager persists jobs across reboots automatically on most devices,
 * but some Android 15+ ROMs (Xiaomi, Samsung, OnePlus) kill pending jobs
 * on reboot. This receiver guarantees everything restarts regardless.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON") return

        Log.d("RakshakX_BOOT", "Device booted — restarting all channel services")

        // Reconcile startup contracts with current permission state.
        AppStartupCoordinator.reconcileOnBoot(context.applicationContext)
    }
}
