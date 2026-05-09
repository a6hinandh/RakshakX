package com.security.rakshakx.call.callanalysis

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.security.rakshakx.R

/**
 * FraudNotificationHelper
 *
 * Helper to show results of fraud analysis to the user via high-priority notifications.
 */
object FraudNotificationHelper {

    private const val CHANNEL_ID = "rakshakx_fraud_alerts"
    private const val CHANNEL_NAME = "Fraud Alerts"
    private const val CHANNEL_DESC = "Notifications about potential scam calls"
    private const val NOTIFICATION_ID = 2005 // Unique ID for fraud result notifications

    /**
     * Shows a notification with the hybrid fraud score and a transcript snippet.
     */
    fun showFraudResultNotification(
        context: Context,
        hybridScore: Float,
        transcript: String?,
        riskLevel: String
    ) {
        createChannelIfNeeded(context)

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

        // Include a short snippet from the transcript
        val snippet = transcript
            ?.take(120)
            ?.ifBlank { "Transcript not available or too short." }
            ?: "Transcript not available."

        val fullText = "$message\n\nTranscript snippet:\n$snippet"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Handle cases where POST_NOTIFICATIONS is not granted
        }
    }

    private fun createChannelIfNeeded(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
            }
            manager.createNotificationChannel(channel)
        }
    }
}


