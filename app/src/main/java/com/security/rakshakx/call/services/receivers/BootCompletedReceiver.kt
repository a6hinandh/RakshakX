package com.security.rakshakx.call.services.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.security.rakshakx.call.services.foreground.FraudMonitoringForegroundService
import com.security.rakshakx.call.services.foreground.MonitoringPreferences
import com.security.rakshakx.call.services.foreground.RiskScanScheduler

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!MonitoringPreferences.isEnabled(context)) return

        FraudMonitoringForegroundService.start(context.applicationContext)
        RiskScanScheduler.schedulePeriodic(context.applicationContext)
    }
}


