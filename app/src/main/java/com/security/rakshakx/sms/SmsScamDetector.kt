package com.security.rakshakx.sms

import android.content.Context
import android.util.Log
import com.security.rakshakx.RakshakXApplication
import com.security.rakshakx.integration.ModelResult
import com.security.rakshakx.integration.ScamAlertManager

class SmsScamDetector(private val context: Context) {

    private val TAG    = "SmsScamDetector"
    private val alert  = ScamAlertManager(context)

    fun analyze(sender: String, body: String, blocklistedSender: Boolean = false): ModelResult {
        Log.d(TAG, "Analyzing SMS from=$sender, length=${body.length}, blocklisted=$blocklistedSender")
        val fullText = "From: $sender\n$body"

        val router = RakshakXApplication.scamRouter
        if (router == null) {
            Log.e(TAG, "CRITICAL: ScamClassifierRouter is NULL in Application. Pipeline skipped.")
            return ModelResult(isScam = false, confidence = 0f, label = "SAFE", modelUsed = "router_null", channel = "sms")
        }

        var result = router.classify(fullText, "sms")

        if (blocklistedSender && result.finalScore < 0.50f) {
            val boosted = (result.finalScore + 0.25f).coerceAtMost(0.95f)
            Log.i(TAG, "Blocklist boost: ${result.finalScore} -> $boosted")
            result = result.copy(
                finalScore = boosted,
                isScam = boosted >= 0.40f,
                label = when {
                    boosted >= 0.70f -> "SCAM"
                    boosted >= 0.40f -> "SUSPICIOUS"
                    else -> result.label
                }
            )
        }

        Log.d(TAG, "SMS Hybrid Result: $result")

        if (result.finalScore >= 0.50f) {
            Log.i(TAG, "HIGH RISK SCAM DETECTED. Triggering alert.")
            alert.handleResult(result)
        } else if (result.finalScore >= 0.40f) {
            Log.i(TAG, "SUSPICIOUS activity detected (Score: ${result.finalScore}).")
            alert.handleResult(result)
        }
        return result
    }

    fun release() {
        // No longer releasing singleton router here
    }
}