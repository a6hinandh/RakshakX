package com.security.rakshakx.core.callerid

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.security.rakshakx.call.core.storage.DatabaseFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class ScamCallDatabase private constructor(private val context: Context) {

    companion object {
        private const val TAG = "ScamCallDB"
        private const val PREFS_NAME = "rakshakx_scam_numbers"
        private const val KEY_REPORTED_NUMBERS = "reported_numbers"
        private const val KEY_SAFE_NUMBERS = "safe_numbers"
        private const val KEY_AUTO_SILENCE = "auto_silence_enabled"

        @Volatile
        private var instance: ScamCallDatabase? = null

        fun getInstance(context: Context): ScamCallDatabase {
            return instance ?: synchronized(this) {
                instance ?: ScamCallDatabase(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _autoSilenceEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_SILENCE, false))
    val autoSilenceEnabled: StateFlow<Boolean> = _autoSilenceEnabled

    fun setAutoSilence(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SILENCE, enabled).apply()
        _autoSilenceEnabled.value = enabled
    }

    fun isKnownScamNumber(phoneNumber: String): Boolean {
        val normalized = normalizePhone(phoneNumber)
        val reported = getReportedNumbers()
        return reported.contains(normalized)
    }

    fun isMarkedSafe(phoneNumber: String): Boolean {
        val normalized = normalizePhone(phoneNumber)
        val safe = getSafeNumbers()
        return safe.contains(normalized)
    }

    fun reportNumber(phoneNumber: String, reason: String = "user_reported") {
        val normalized = normalizePhone(phoneNumber)
        val reported = getReportedNumbers().toMutableSet()
        reported.add(normalized)
        prefs.edit().putStringSet(KEY_REPORTED_NUMBERS, reported).apply()

        val safe = getSafeNumbers().toMutableSet()
        safe.remove(normalized)
        prefs.edit().putStringSet(KEY_SAFE_NUMBERS, safe).apply()

        Log.d(TAG, "Number reported: $normalized ($reason)")
    }

    fun markSafe(phoneNumber: String) {
        val normalized = normalizePhone(phoneNumber)
        val safe = getSafeNumbers().toMutableSet()
        safe.add(normalized)
        prefs.edit().putStringSet(KEY_SAFE_NUMBERS, safe).apply()

        val reported = getReportedNumbers().toMutableSet()
        reported.remove(normalized)
        prefs.edit().putStringSet(KEY_REPORTED_NUMBERS, reported).apply()

        Log.d(TAG, "Number marked safe: $normalized")
    }

    suspend fun getNumberRiskLevel(phoneNumber: String): NumberRiskLevel = withContext(Dispatchers.IO) {
        val normalized = normalizePhone(phoneNumber)

        if (isMarkedSafe(normalized)) return@withContext NumberRiskLevel.SAFE
        if (isKnownScamNumber(normalized)) return@withContext NumberRiskLevel.SCAM

        try {
            val db = DatabaseFactory.getInstance(context)
            val dao = db.fraudDao()
            val since = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)

            val callHistory = dao.findRecentCallsByPhone(normalized, since)
            val smsHistory = dao.findRecentSmsByPhone(normalized, since)

            val suspiciousCalls = callHistory.filter { it.fraudRiskScore > 0.4f }
            val suspiciousSms = smsHistory.filter { it.fraudRiskScore > 0.4f }

            when {
                suspiciousCalls.size >= 2 || suspiciousSms.size >= 3 -> NumberRiskLevel.HIGH_RISK
                suspiciousCalls.isNotEmpty() || suspiciousSms.isNotEmpty() -> NumberRiskLevel.SUSPICIOUS
                callHistory.isNotEmpty() || smsHistory.isNotEmpty() -> NumberRiskLevel.KNOWN
                else -> NumberRiskLevel.UNKNOWN
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking number risk", e)
            NumberRiskLevel.UNKNOWN
        }
    }

    fun getReportedNumberCount(): Int = getReportedNumbers().size

    fun getSafeNumberCount(): Int = getSafeNumbers().size

    private fun getReportedNumbers(): Set<String> =
        prefs.getStringSet(KEY_REPORTED_NUMBERS, emptySet()) ?: emptySet()

    private fun getSafeNumbers(): Set<String> =
        prefs.getStringSet(KEY_SAFE_NUMBERS, emptySet()) ?: emptySet()

    private fun normalizePhone(phone: String): String {
        val digits = phone.replace(Regex("[^0-9]"), "")
        return if (digits.length > 10) digits.takeLast(10) else digits
    }
}

enum class NumberRiskLevel(val label: String, val emoji: String) {
    SCAM("Known Scam", "🚫"),
    HIGH_RISK("High Risk", "⚠️"),
    SUSPICIOUS("Suspicious", "⚡"),
    UNKNOWN("Unknown", "❓"),
    KNOWN("Known", "📞"),
    SAFE("Verified Safe", "✅")
}
