package com.security.rakshakx.notifications

import android.Manifest
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
import com.security.rakshakx.call.callanalysis.FraudDecision
import com.security.rakshakx.call.callanalysis.RiskConfig
import com.security.rakshakx.call.callanalysis.data.CallRecord
import com.security.rakshakx.call.callanalysis.ui.RakshakXActivity
import com.security.rakshakx.notifications.receivers.NotificationActionReceiver

/**
 * Call-channel fraud notifications (risk UI + recording hybrid result).
 * Merged from former RakshakXNotificationManager + FraudNotificationHelper.
 */
object CallFraudNotifications {

    private const val TAG = "CallFraudNotifications"
    private const val RECORDING_NOTIFICATION_ID = 9999
    private const val FRAUD_RESULT_NOTIFICATION_ID = 2005

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
            RakshakNotificationChannels.bootstrap(context)

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
                putExtra("phoneNumber", callRecord.phoneNumber)
                putExtra("callId", callRecord.id)
            }
            val safePendingIntent = PendingIntent.getBroadcast(
                context,
                callRecord.id.toInt() + 2,
                safeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val colorInt = ContextCompat.getColor(context, colorResId)

            val builder = NotificationCompat.Builder(context, RakshakNotificationChannels.ALERTS)
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
            RakshakNotificationChannels.bootstrap(context)

            if (!hasPostNotificationPermission(context)) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted, skipping recording notification")
                return
            }

            val notification = NotificationCompat.Builder(context, RakshakNotificationChannels.RECORDING)
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

    /** Hybrid recording fraud result (formerly FraudNotificationHelper). */
    fun showFraudResultNotification(
        context: Context,
        hybridScore: Float,
        transcript: String?,
        riskLevel: String
    ) {
        RakshakNotificationChannels.bootstrap(context)

        val pct = (hybridScore * 100).toInt()

        val title: String
        val message: String
        val iconRes: Int

        when {
            hybridScore >= RiskConfig.THRESHOLD_HIGH -> {
                title = "⚠️ Possible scam detected ($pct%)"
                message = "This call looks risky. Risk level: $riskLevel."
                iconRes = R.drawable.ic_shield_warning
            }
            hybridScore >= RiskConfig.THRESHOLD_MEDIUM -> {
                title = "🤔 Call may be suspicious ($pct%)"
                message = "Some risk indicators were found. Risk level: $riskLevel."
                iconRes = R.drawable.ic_shield_warning
            }
            else -> {
                title = "✅ Call looks safe ($pct%)"
                message = "Low fraud risk detected. Risk level: $riskLevel."
                iconRes = R.drawable.ic_check
            }
        }

        val snippet = transcript
            ?.take(120)
            ?.ifBlank { "Transcript not available or too short." }
            ?: "Transcript not available."

        val fullText = "$message\n\nTranscript snippet:\n$snippet"

        val builder = NotificationCompat.Builder(context, RakshakNotificationChannels.FRAUD_RESULTS)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(FRAUD_RESULT_NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted
        }
    }
}
