package com.security.rakshakx.web.utils

import android.util.Log
import com.security.rakshakx.web.models.BrowserSessionData

object BrowserLogger {
    private const val TAG = "RakshakX-Browser"

    fun logSession(data: BrowserSessionData) {
        val riskSignals = buildString {
            append("Password Field: ").append(data.passwordFieldDetected)
            append(" | OTP Field: ").append(data.otpFieldDetected)
            append(" | Email Field: ").append(data.emailFieldDetected)
            append(" | Payment Field: ").append(data.paymentFieldDetected)
        }

        Log.i(
            TAG,
            "Browser: ${data.browserPackage}\n" +
                "URL: ${data.url}\n" +
                "Title: ${data.pageTitle}\n" +
                "Risk Signals: $riskSignals\n" +
                "Visible Text Summary: ${data.visibleText}"
        )
    }
}
