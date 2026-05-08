package com.security.rakshakx.web.models

data class BrowserSessionData(
    val timestamp: Long,
    val browserPackage: String,
    val url: String,
    val pageTitle: String,
    val visibleText: String,
    val passwordFieldDetected: Boolean,
    val otpFieldDetected: Boolean,
    val emailFieldDetected: Boolean,
    val paymentFieldDetected: Boolean
)
