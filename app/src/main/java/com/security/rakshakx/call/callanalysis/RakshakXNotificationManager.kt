package com.security.rakshakx.call.callanalysis

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.security.rakshakx.R
import com.security.rakshakx.call.callanalysis.ui.RakshakXActivity
import com.security.rakshakx.call.callanalysis.data.CallRecord
object RakshakXNotificationManager {

    private const val TAG = "RakshakXNotificationMgr"
    private const val CHANNEL_ID_ALERTS = "rakshakx_alerts"
    private const val CHANNEL_ID_RECORDING = "rakshakx_recording"
    private const val RECORDING_NOTIFICATION_ID = 9999

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "RakshakX Fraud Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for detected fraud calls"
                enableLights(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(alertChannel)

            val recordingChannel = NotificationChannel(
                CHANNEL_ID_RECORDING,
                "RakshakX Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification while analyzing calls"
            }
            notificationManager.createNotificationChannel(recordingChannel)
        }
    }

    private fun hasPostNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showRiskNotification(
        context: Context,
        callRecord: CallRecord,
        decision: FraudDecision
    ) {
        try {
            createNotificationChannels(context)

            if (callRecord.riskScore < RiskConfig.THRESHOLD_SAFE_ROUTING) {
                Log.d(TAG, "Risk score too low (${callRecord.riskScore}), skipping notification")
                return
            }

            if (!hasPostNotificationPermission(context)) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted, skipping risk notification")
                return
            }

            val (riskLevel, colorResId) = when {
                callRecord.riskScore >= RiskConfig.THRESHOLD_CRITICAL -> "🔴 HIGH RISK" to RiskConfig.COLOR_CRITICAL
                callRecord.riskScore >= RiskConfig.THRESHOLD_SAFE_ROUTING -> "🟡 SUSPICIOUS" to RiskConfig.COLOR_MEDIUM
                else -> "🟢 SAFE" to RiskConfig.COLOR_SAFE
            }

            val riskPercent = (callRecord.riskScore * 100).toInt()

            val detailsIntent = Intent(context, RakshakXActivity::class.java).apply {
                putExtra("callId", callRecord.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val detailsPendingIntent = PendingIntent.getActivity(
                context,
                callRecord.id.toInt(),
                detailsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val blockIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = "ACTION_BLOCK_NUMBER"
                putExtra("phoneNumber", callRecord.phoneNumber)
                putExtra("callId", callRecord.id)
            }
            val blockPendingIntent = PendingIntent.getBroadcast(
                context,
                callRecord.id.toInt() + 1,
                blockIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val safeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = "ACTION_MARK_SAFE"
                putExtra("callId", callRecord.id)
            }
            val safePendingIntent = PendingIntent.getBroadcast(
                context,
                callRecord.id.toInt() + 2,
                safeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val colorInt = ContextCompat.getColor(context, colorResId)

            val builder = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
                .setSmallIcon(R.drawable.ic_shield_warning)
                .setContentTitle("⚠️ RakshakX Alert: $riskLevel")
                .setContentText("${callRecord.phoneNumber} | ${decision.reason}")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        buildString {
                            append("Risk Score: $riskPercent%\n")
                            append("Action: ${decision.action}\n")
                            append(
                                "Transcript: ${
                                    callRecord.transcript?.take(100) ?: "N/A"
                                }"
                            )
                        }
                    )
                )
                .setContentIntent(detailsPendingIntent)
                .setAutoCancel(true)
                .setColor(colorInt)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .addAction(R.drawable.ic_block, "Block Number", blockPendingIntent)
                .addAction(R.drawable.ic_check, "Mark Safe", safePendingIntent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder
                    .setVibrate(longArrayOf(0, 500, 200, 500))
                    .setSound(
                        RingtoneManager.getDefaultUri(
                            RingtoneManager.TYPE_NOTIFICATION
                        )
                    )
            }

            val notification = builder.build()

            try {
                NotificationManagerCompat.from(context).notify(
                    callRecord.id.toInt(),
                    notification
                )
            } catch (se: SecurityException) {
                Log.e(TAG, "SecurityException while showing risk notification", se)
            }

            Log.d(
                TAG,
                "Risk notification requested: ${callRecord.phoneNumber}, Risk: ${callRecord.riskScore}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show risk notification", e)
        }
    }

    fun showRecordingNotification(context: Context, phoneNumber: String) {
        try {
            createNotificationChannels(context)

            if (!hasPostNotificationPermission(context)) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted, skipping recording notification")
                return
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID_RECORDING)
                .setSmallIcon(R.drawable.ic_recording)
                .setContentTitle("RakshakX is analyzing this call...")
                .setContentText("Phone: $phoneNumber")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(
                    RECORDING_NOTIFICATION_ID,
                    notification
                )
            } catch (se: SecurityException) {
                Log.e(TAG, "SecurityException while showing recording notification", se)
            }

            Log.d(TAG, "Recording notification requested")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show recording notification", e)
        }
    }

    fun dismissRecordingNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(RECORDING_NOTIFICATION_ID)
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException while dismissing recording notification", se)
        }
    }
}

