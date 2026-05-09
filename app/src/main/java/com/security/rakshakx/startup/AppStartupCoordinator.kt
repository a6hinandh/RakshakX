package com.security.rakshakx.startup

import android.Manifest
import android.content.Context
import com.security.rakshakx.call.services.foreground.FraudMonitoringForegroundService
import com.security.rakshakx.call.services.foreground.MonitoringPreferences
import com.security.rakshakx.call.services.foreground.RiskScanScheduler
import com.security.rakshakx.permissions.PermissionManager
import com.security.rakshakx.sms.SmsPollingWorker

object AppStartupCoordinator {

    fun reconcileOnAppLaunch(context: Context) {
        reconcileCallMonitoring(context)
        reconcileSmsPolling(context)
    }

    fun reconcileOnBoot(context: Context) {
        reconcileCallMonitoring(context)
        reconcileSmsPolling(context)
    }

    private fun reconcileCallMonitoring(context: Context) {
        val state = PermissionManager.getReadinessState(context)
        val monitoringEnabled = MonitoringPreferences.isEnabled(context)

        if (monitoringEnabled && state.callReady) {
            FraudMonitoringForegroundService.start(context.applicationContext)
            RiskScanScheduler.schedulePeriodic(context.applicationContext)
        } else {
            FraudMonitoringForegroundService.stop(context.applicationContext)
            RiskScanScheduler.cancelPeriodic(context.applicationContext)
            if (monitoringEnabled && !state.callReady) {
                MonitoringPreferences.setEnabled(context.applicationContext, false)
            }
        }
    }

    private fun reconcileSmsPolling(context: Context) {
        val hasReadSms = PermissionManager.hasPermission(context, Manifest.permission.READ_SMS)
        if (hasReadSms) {
            SmsPollingWorker.schedule(context.applicationContext)
        } else {
            SmsPollingWorker.cancel(context.applicationContext)
        }
    }
}

