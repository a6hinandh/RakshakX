package com.security.rakshakx.sms

import android.content.Context
import java.security.MessageDigest

object SmsDeduplicationGuard {
    private const val PREFS = "rakshak_sms_dedupe"
    private const val KEY_LAST_HASH = "last_hash"
    private const val KEY_LAST_TS = "last_ts"
    private const val WINDOW_MS = 30_000L

    fun shouldProcess(context: Context, sender: String?, message: String?): Boolean {
        val normalizedSender = sender.orEmpty().trim().lowercase()
        val normalizedMessage = message.orEmpty().trim().lowercase()
        if (normalizedMessage.isBlank()) return false

        val currentHash = hash("$normalizedSender|$normalizedMessage")
        val now = System.currentTimeMillis()
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastHash = prefs.getString(KEY_LAST_HASH, null)
        val lastTs = prefs.getLong(KEY_LAST_TS, 0L)

        if (currentHash == lastHash && now - lastTs < WINDOW_MS) {
            return false
        }

        prefs.edit()
            .putString(KEY_LAST_HASH, currentHash)
            .putLong(KEY_LAST_TS, now)
            .apply()
        return true
    }

    private fun hash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

