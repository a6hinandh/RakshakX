package com.security.rakshakx.web

import android.content.Context
import android.util.Log
import com.security.rakshakx.RakshakXApplication
import com.security.rakshakx.integration.ModelResult
import com.security.rakshakx.integration.ScamAlertManager

class WebScamDetector(private val context: Context) {

    private val TAG    = "WebScamDetector"
    private val alert  = ScamAlertManager(context)

    private val WHITELIST = setOf(
        "google.com", "youtube.com", "wikipedia.org",
        "gov.in", "nic.in", "rbi.org.in", "npci.org.in"
    )

    fun analyze(url: String, pageText: String, pageTitle: String = ""): ModelResult {
        Log.d(TAG, "Analyzing web url=$url length=${pageText.length}")

        if (WHITELIST.any { domain -> url.contains(domain) }) {
            Log.d(TAG, "URL whitelisted: $url")
            return ModelResult(false, 1f, "SAFE", "whitelist", "web")
        }

        val fullText = "URL: $url\nTitle: $pageTitle\n\n$pageText"
        val result   = RakshakXApplication.scamRouter?.classify(fullText, "web")
            ?: ModelResult(isScam = false, confidence = 0f, label = "SAFE", modelUsed = "router_null", channel = "web")

        Log.d(TAG, "Web result: $result")
        if (result.isScam) alert.handleResult(result)
        return result
    }

    fun release() {
        // Singleton router released in Application.onTerminate
    }
}