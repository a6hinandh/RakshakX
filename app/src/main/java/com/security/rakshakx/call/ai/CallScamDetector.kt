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
        val result   = RakshakXApplication.scamRouter?.classify(fullText, "call")
            ?: ModelResult(isScam = false, confidence = 0f, label = "SAFE", modelUsed = "router_null", channel = "call")

        Log.d(TAG, "Call result: $result")

        val shouldAlert = if (isLive) {
            result.isScam && result.confidence > 0.85f
        } else {
            result.isScam
        }
        if (shouldAlert) alert.handleResult(result)
        return result
    }

    fun release() {
        // Singleton router released in Application.onTerminate
    }
}