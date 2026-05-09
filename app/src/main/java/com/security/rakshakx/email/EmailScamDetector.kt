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
        val result   = RakshakXApplication.scamRouter?.classify(fullText, "email")
            ?: ModelResult(isScam = false, confidence = 0f, label = "SAFE", modelUsed = "router_null", channel = "email")

        Log.d(TAG, "Email result: $result")
        if (result.isScam) alert.handleResult(result)
        return result
    }

    fun release() {
        // Singleton router released in Application.onTerminate
    }
}