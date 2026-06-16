package com.security.rakshakx.core.threatintel

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.security.rakshakx.call.core.storage.DatabaseFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class ThreatIntelligenceManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "ThreatIntel"
        private const val PREFS_NAME = "rakshakx_threat_intel"
        private const val KEY_OPT_IN = "community_opt_in"
        private const val KEY_BLOCKLIST = "local_blocklist"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"

        @Volatile
        private var instance: ThreatIntelligenceManager? = null

        fun getInstance(context: Context): ThreatIntelligenceManager {
            return instance ?: synchronized(this) {
                instance ?: ThreatIntelligenceManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isOptedIn = MutableStateFlow(prefs.getBoolean(KEY_OPT_IN, false))
    val isOptedIn: StateFlow<Boolean> = _isOptedIn

    private val _blocklist = MutableStateFlow(loadBlocklist())
    val blocklist: StateFlow<Set<String>> = _blocklist

    fun setOptIn(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OPT_IN, enabled).apply()
        _isOptedIn.value = enabled
        Log.d(TAG, "Community sharing opt-in: $enabled")
    }

    fun isNumberBlocked(phoneNumber: String): Boolean {
        val hash = hashPhone(phoneNumber)
        return _blocklist.value.contains(hash)
    }

    fun isDomainBlocked(domain: String): Boolean {
        val hash = hashDomain(domain)
        return _blocklist.value.contains(hash)
    }

    fun addToBlocklist(identifier: String, type: BlocklistType) {
        val hash = when (type) {
            BlocklistType.PHONE -> hashPhone(identifier)
            BlocklistType.DOMAIN -> hashDomain(identifier)
        }
        val updated = _blocklist.value + hash
        saveBlocklist(updated)
        _blocklist.value = updated
        Log.d(TAG, "Added to blocklist: ${type.name} hash=$hash")
    }

    fun removeFromBlocklist(identifier: String, type: BlocklistType) {
        val hash = when (type) {
            BlocklistType.PHONE -> hashPhone(identifier)
            BlocklistType.DOMAIN -> hashDomain(identifier)
        }
        val updated = _blocklist.value - hash
        saveBlocklist(updated)
        _blocklist.value = updated
    }

    suspend fun generateAnonymousReport(): ThreatReport? = withContext(Dispatchers.IO) {
        if (!_isOptedIn.value) return@withContext null

        try {
            val db = DatabaseFactory.getInstance(context)
            val dao = db.fraudDao()
            val since = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)

            val recentSms = dao.getAllSmsList(200).filter {
                it.timestamp > since && it.fraudRiskScore > 0.5f
            }

            val hashedPhones = recentSms.map { hashPhone(it.sender) }.distinct()
            val hashedDomains = recentSms
                .flatMap { it.detectedUrls.split(",") }
                .filter { it.isNotBlank() }
                .map { extractDomain(it) }
                .filter { it.length > 3 }
                .map { hashDomain(it) }
                .distinct()

            ThreatReport(
                phoneHashes = hashedPhones,
                domainHashes = hashedDomains,
                threatCount = recentSms.size,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate report", e)
            null
        }
    }

    fun mergeFromCommunityFeed(phoneHashes: List<String>, domainHashes: List<String>) {
        val updated = _blocklist.value + phoneHashes + domainHashes
        saveBlocklist(updated)
        _blocklist.value = updated
        prefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()
        Log.d(TAG, "Merged ${phoneHashes.size} phones + ${domainHashes.size} domains from community")
    }

    fun getLastSyncTime(): Long = prefs.getLong(KEY_LAST_SYNC, 0L)

    fun getBlocklistSize(): Int = _blocklist.value.size

    private fun hashPhone(phone: String): String {
        val normalized = phone.replace(Regex("[^0-9]"), "").takeLast(10)
        return sha256("phone:$normalized")
    }

    private fun hashDomain(domain: String): String {
        val normalized = domain.lowercase().removePrefix("www.")
        return sha256("domain:$normalized")
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun extractDomain(urlStr: String): String {
        return try {
            val formatted = if (!urlStr.startsWith("http")) "https://$urlStr" else urlStr
            android.net.Uri.parse(formatted).host?.removePrefix("www.") ?: urlStr
        } catch (_: Exception) {
            urlStr
        }
    }

    private fun loadBlocklist(): Set<String> {
        val raw = prefs.getStringSet(KEY_BLOCKLIST, emptySet()) ?: emptySet()
        return raw.toSet()
    }

    private fun saveBlocklist(blocklist: Set<String>) {
        prefs.edit().putStringSet(KEY_BLOCKLIST, blocklist).apply()
    }
}

enum class BlocklistType { PHONE, DOMAIN }

data class ThreatReport(
    val phoneHashes: List<String>,
    val domainHashes: List<String>,
    val threatCount: Int,
    val timestamp: Long
)
