package com.example.rakshakxdemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BootReceiver — reschedules the SMS polling WorkManager job after reboot.
 *
 * WorkManager persists jobs across reboots automatically on most devices,
 * but some Android 15+ ROMs (Xiaomi, Samsung, OnePlus) kill pending jobs
 * on reboot. This receiver guarantees the poll restarts regardless.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("RakshakX_BOOT", "Device booted — rescheduling SMS polling")
            SmsPollingWorker.schedule(context)
        }
    }
}