package com.security.rakshakx.email

import android.content.Context
import android.util.Log
import com.security.rakshakx.RakshakXApplication
import com.security.rakshakx.integration.ModelResult
import com.security.rakshakx.integration.ScamAlertManager

class EmailScamDetector(private val context: Context) {

    private val TAG    = "EmailScamDetector"
    private val alert  = ScamAlertManager(context)

    fun analyze(sender: String, subject: String, body: String): ModelResult {
        Log.d(TAG, "Analyzing email from=$sender subject=$subject")
        val fullText = "From: $sender\nSubject: $subject\n\n$body"
        
        val router = RakshakXApplication.scamRouter
        if (router == null) {
            return ModelResult(isScam = false, confidence = 0f, label = "SAFE", modelUsed = "router_null", channel = "email")
        }

        val result = router.classify(fullText, "email")
        Log.d(TAG, "Email Hybrid Result: $result")

        // ── HYBRID ALERT LOGIC ───────────────────────────────────────────────
        if (result.finalScore >= 0.50f) {
            Log.i(TAG, "HIGH RISK EMAIL SCAM DETECTED. Triggering alert.")
            alert.handleResult(result)
        } else if (result.finalScore >= 0.40f) {
            Log.i(TAG, "SUSPICIOUS Email detected (Score: ${result.finalScore}).")
            alert.handleResult(result)
        }

        return result
    }

    fun release() {
        // Singleton router released in Application.onTerminate
    }
}