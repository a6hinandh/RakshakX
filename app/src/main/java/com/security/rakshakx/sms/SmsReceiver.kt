package com.security.rakshakx.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.security.rakshakx.core.SettingsStore
import com.security.rakshakx.core.correlation.MultiChannelCorrelationEngine
import com.security.rakshakx.core.threatintel.BlocklistType
import com.security.rakshakx.core.threatintel.ThreatIntelligenceManager
import com.security.rakshakx.notifications.SmsFraudNotifications
import com.security.rakshakx.call.core.storage.DatabaseFactory
import com.security.rakshakx.data.entities.SmsEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * SmsReceiver — processes incoming SMS events using the Hybrid ML Pipeline.
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "RakshakX_SMS_RCV"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        Log.d(TAG, "SMS broadcast received")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsStore.getInstance(context)
                if (!settings.smsEnabled.value) {
                    Log.d(TAG, "SMS Protection disabled in settings.")
                    return@launch
                }

                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages.isNullOrEmpty()) return@launch

                val detector = SmsScamDetector(context)

                val threatIntel = ThreatIntelligenceManager.getInstance(context)

                for (sms in messages) {
                    val sender = sms.displayOriginatingAddress ?: "Unknown"
                    val body   = sms.messageBody ?: ""

                    if (!SmsDeduplicationGuard.shouldProcess(context, sender, body)) {
                        Log.d(TAG, "Skipping duplicate SMS")
                        continue
                    }

                    // ── Blocklist pre-check ──────────────────────────────────
                    val blocklistedSender = threatIntel.isNumberBlocked(sender)
                    if (blocklistedSender) {
                        Log.i(TAG, "Sender $sender is in threat intel blocklist — boosting risk")
                    }

                    // ── New ML Detection Pipeline ────────────────────────────
                    val result = detector.analyze(sender, body, blocklistedSender)

                    Log.d(
                        TAG,
                        "From=$sender isScam=${result.isScam} confidence=${result.confidence}"
                    )

                    // ── Fraud Notification ───────────────────────────────────
                    if (result.isScam) {
                        RiskEngine.recordFlaggedSender(context, sender)
                        threatIntel.addToBlocklist(sender, BlocklistType.PHONE)
                        val urls = extractUrls(body)
                        for (url in urls) {
                            threatIntel.addToBlocklist(url, BlocklistType.DOMAIN)
                        }
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

                    // ── Log to Database for Correlation ──────────────────────
                    try {
                        val db = DatabaseFactory.getInstance(context)
                        val urls = extractUrls(body)
                        val entity = SmsEventEntity(
                            sender = sender,
                            messageBody = body,
                            fraudRiskScore = result.finalScore,
                            detectedUrls = urls.joinToString(","),
                            containsOtp = body.lowercase().contains("otp"),
                            detectedKeywords = result.label
                        )
                        val smsId = db.fraudDao().insertSms(entity)
                        Log.d(TAG, "Logged SMS to DB for correlation: ${entity.sender}")

                        if (result.isScam) {
                            try {
                                val engine = MultiChannelCorrelationEngine(context)
                                val savedEntity = entity.copy(id = smsId)
                                val correlations = engine.correlateSmsEvent(savedEntity)
                                for (corr in correlations) {
                                    Log.i(TAG, "Cross-channel correlation: ${corr.reason}")
                                }
                            } catch (corrError: Exception) {
                                Log.e(TAG, "Correlation check failed", corrError)
                            }
                        }
                    } catch (dbError: Exception) {
                        Log.e(TAG, "Failed to log SMS to DB", dbError)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in SmsReceiver", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun extractUrls(text: String): List<String> {
        val urls = mutableListOf<String>()
        val matcher = android.util.Patterns.WEB_URL.matcher(text)
        while (matcher.find()) {
            urls.add(matcher.group())
        }
        return urls
    }
}

