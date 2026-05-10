package com.security.rakshakx.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.security.rakshakx.notifications.SmsFraudNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * SmsReceiver — FALLBACK only on Android 10+.
 *
 * On Android 15 the OS only delivers SMS_RECEIVED to the DEFAULT SMS app.
 * This receiver fires reliably only on Android 9 and below, or if the user
 * has set RakshakX as the default SMS app.
 *
 * Primary detection is handled by NotificationListenerService.
 * Keep this registered as a bonus catch for edge cases.
 */
import com.security.rakshakx.core.SettingsStore
import kotlinx.coroutines.flow.first

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "RakshakX_SMS"
    }

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        Log.d(TAG, "SMS_RECEIVED broadcast received")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {

            try {
                val settings = SettingsStore.getInstance(context)
                if (!settings.smsEnabled.value) {
                    Log.d(TAG, "SMS Protection is disabled in settings. Skipping analysis.")
                    return@launch
                }

                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

                if (messages.isNullOrEmpty()) return@launch

                for (sms in messages) {

                    val sender = sms.displayOriginatingAddress ?: "Unknown"
                    val body   = sms.messageBody ?: ""

                    // ── Prevent duplicate processing ──────────────────────────
                    if (!SmsDeduplicationGuard.shouldProcess(context, sender, body)) {
                        Log.d(TAG, "Skipping duplicate SMS event from broadcast path")
                        continue
                    }

                    // ── New ML Detection Pipeline ────────────────────────────
                    val detector = SmsScamDetector(context)
                    val result = detector.analyze(sender, body)

                    Log.d(
                        TAG,
                        "From=$sender isScam=${result.isScam} confidence=${result.confidence}"
                    )

                    // ── Fraud Notification ───────────────────────────────────
                    if (result.isScam) {
                        val score0to100 = result.ruleScore.takeIf { it > 0 }
                            ?: (result.finalScore * 100f).toInt().coerceIn(0, 100)
                        SmsFraudNotifications.showFraudAlert(
                            context   = context,
                            sender    = sender,
                            message   = body,
                            riskScore = score0to100,
                            source    = "SMS"
                        )
                    }
                }

            } catch (e: Exception) {

                Log.e(TAG, "Error in SmsReceiver", e)

            } finally {

                pendingResult.finish()
            }
        }
    }
}