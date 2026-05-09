package com.security.rakshakx.call.callanalysis

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.security.rakshakx.R

/**
 * HackathonModeCallMonitorService
 *
 * A specialized background monitor for hackathon demonstrations.
 * Automatically triggers call recording and analysis when any call starts.
 * This mode assumes the user will put the call on speakerphone for microphone capture.
 */
class HackathonModeCallMonitorService : Service() {

    companion object {
        private const val TAG = "HackathonMonitor"
        private const val CHANNEL_ID = "hackathon_mode"
        private const val NOTIFICATION_ID = 3001

        fun startMonitoring(context: Context) {
            val intent = Intent(context, HackathonModeCallMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopMonitoring(context: Context) {
            val intent = Intent(context, HackathonModeCallMonitorService::class.java)
            context.stopService(intent)
        }
    }

    private var callStateMonitor: CallStateMonitor? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            }
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        callStateMonitor = CallStateMonitor(this, object : CallStateMonitor.CallStateListener {
            override fun onCallStarted(phoneNumber: String?) {
                Log.d(TAG, "Call started! Triggering recording for Hackathon Mode")
                // Start the recording service automatically
                CallRecordingService.startRecording(this@HackathonModeCallMonitorService, phoneNumber ?: "Unknown")
            }

            override fun onCallEnded() {
                Log.d(TAG, "Call ended! Hackathon Mode will show results soon.")
                // The CallRecordingService handles stopping itself after 10s or when stopRecording is called.
            }
        })
        callStateMonitor?.start()
        Log.d(TAG, "Hackathon Mode Monitor Service Started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        callStateMonitor?.stop()
        Log.d(TAG, "Hackathon Mode Monitor Service Stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hackathon Mode Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors calls for automatic recording in Hackathon Mode"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RakshakX Hackathon Mode")
            .setContentText("Monitoring calls for automatic recording & analysis")
            .setSmallIcon(R.drawable.ic_recording)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}


