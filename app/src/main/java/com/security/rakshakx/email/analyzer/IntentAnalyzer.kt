package com.security.rakshakx.email.analyzer

object IntentAnalyzer {

    private val suspiciousPhrases = listOf(

        "verify your account",
        "urgent action required",
        "click here",
        "your account will be blocked",
        "login immediately",
        "bank account",
        "payment failed",
        "confirm your identity",
        "security alert",
        "unauthorized login"
    )

    fun detectSuspiciousIntent(text: String): Boolean {

        return suspiciousPhrases.any {
            text.contains(it, true)
        }
    }

    fun countSuspiciousPhrases(text: String): Int {

        return suspiciousPhrases.count {
            text.contains(it, true)
        }
    }
}