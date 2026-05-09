package com.security.rakshakx.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.security.rakshakx.call.services.foreground.FraudMonitoringForegroundService
import com.security.rakshakx.call.services.foreground.MonitoringPreferences
import com.security.rakshakx.call.services.foreground.RiskScanScheduler

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

        // ── SMS channel: reschedule inbox polling ──
        SmsPollingWorker.schedule(context)

        // ── Call channel: restart monitoring if it was enabled ──
        if (MonitoringPreferences.isEnabled(context)) {
            FraudMonitoringForegroundService.start(context.applicationContext)
            RiskScanScheduler.schedulePeriodic(context.applicationContext)
        }
    }
}
