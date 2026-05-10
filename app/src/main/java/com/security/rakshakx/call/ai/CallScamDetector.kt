package com.security.rakshakx.call.ai

import android.content.Context
import android.util.Log
import com.security.rakshakx.RakshakXApplication
import com.security.rakshakx.integration.ModelResult
import com.security.rakshakx.integration.ScamAlertManager

class CallScamDetector(private val context: Context) {

    private val TAG    = "CallScamDetector"
    private val alert  = ScamAlertManager(context)

    fun analyze(callerNumber: String, transcript: String, isLive: Boolean = false): ModelResult {
        Log.d(TAG, "Analyzing call from=$callerNumber live=$isLive length=${transcript.length}")
        val fullText = "Caller: $callerNumber\nTranscript:\n$transcript"
        
        val router = RakshakXApplication.scamRouter
        if (router == null) {
            return ModelResult(isScam = false, confidence = 0f, label = "SAFE", modelUsed = "router_null", channel = "call")
        }

        val result = router.classify(fullText, "call")
        Log.d(TAG, "Call Hybrid Result: $result")

        // ── HYBRID ALERT LOGIC ───────────────────────────────────────────────
        val shouldAlert = if (isLive) {
            // Live calls require higher threshold to avoid interrupting normal calls
            result.finalScore >= 0.70f 
        } else {
            result.finalScore >= 0.50f
        }

        if (shouldAlert) {
            Log.i(TAG, "SCAM DETECTED on call (Live=$isLive). Triggering alert.")
            alert.handleResult(result)
        }

        return result
    }

    fun release() {
        // Singleton router released in Application.onTerminate
    }
}