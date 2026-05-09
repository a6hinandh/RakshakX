package com.rakshakx.services.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rakshakx.services.foreground.FraudMonitoringForegroundService
import com.rakshakx.services.foreground.MonitoringPreferences
import com.rakshakx.services.foreground.RiskScanScheduler

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!MonitoringPreferences.isEnabled(context)) return

        FraudMonitoringForegroundService.start(context.applicationContext)
        RiskScanScheduler.schedulePeriodic(context.applicationContext)
    }
}
