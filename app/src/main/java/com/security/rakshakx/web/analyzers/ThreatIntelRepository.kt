package com.security.rakshakx.web.analyzers

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.Locale

class ThreatIntelRepository(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "rakshakx_threat_intel",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val defaultRiskyTlds = setOf("zip", "xyz", "top", "gq", "work", "click", "help")
    private val defaultBrandKeywords = setOf(
        "bank", "secure", "login", "verify", "account", "pay", "wallet", "upi", "card"
    )
    private val defaultBankBrands = setOf(
        "chase", "wellsfargo", "paypal", "visa", "mastercard", "amex", "capitalone",
        "citibank", "hsbc", "barclays", "icici", "hdfc", "sbi", "axis", "kotak", "rakshak"
    )
    private val defaultShorteners = setOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "is.gd", "cutt.ly", "rebrand.ly",
        "ow.ly", "buff.ly", "tiny.cc", "lnkd.in"
    )
    private val defaultPhishingKeywords = setOf(
        "login", "verify", "secure", "update", "account", "support", "bank", "payment"
    )
    private val defaultScamPatterns = setOf(
        "urgent action", "account suspended", "refund", "payment failed", "kyc update",
        "security alert", "verify now"
    )
    private val defaultSafeSuffixes = setOf(
        "gvt2.com", "gstatic.com", "google.com", "googleusercontent.com", "doubleclick.net",
        "wikimedia.org", "wikipedia.org", "ampproject.org", "cloudfront.net", "akamaihd.net",
        "fastly.net", "cdninstagram.com", "fbcdn.net"
    )

    fun riskyTlds(): Set<String> = defaultRiskyTlds
    fun brandKeywords(): Set<String> = defaultBrandKeywords
    fun bankBrandKeywords(): Set<String> = defaultBankBrands
    fun urlShorteners(): Set<String> = defaultShorteners
    fun phishingKeywords(): Set<String> = defaultPhishingKeywords
    fun scamPatterns(): Set<String> = defaultScamPatterns
    fun safeSuffixes(): Set<String> = defaultSafeSuffixes

    fun allowList(): Set<String> = loadSet(KEY_ALLOWLIST)
    fun suspiciousDomains(): Set<String> = loadSet(KEY_SUSPICIOUS)

    fun isAllowListed(domain: String): Boolean {
        return allowList().contains(domain.lowercase(Locale.US))
    }

    fun isShortener(domain: String): Boolean {
        return urlShorteners().contains(domain.lowercase(Locale.US))
    }

    fun isSafeSuffix(domain: String): Boolean {
        val normalized = domain.lowercase(Locale.US)
        return safeSuffixes().any { normalized == it || normalized.endsWith(".$it") }
    }

    fun addAllowDomain(domain: String) {
        val normalized = domain.lowercase(Locale.US)
        val updated = allowList().toMutableSet().apply { add(normalized) }
        saveSet(KEY_ALLOWLIST, updated)
    }

    fun addSuspiciousDomain(domain: String) {
        val normalized = domain.lowercase(Locale.US)
        val updated = suspiciousDomains().toMutableSet().apply { add(normalized) }
        saveSet(KEY_SUSPICIOUS, updated)
    }

    private fun loadSet(key: String): Set<String> {
        val raw = prefs.getString(key, "")?.trim().orEmpty()
        if (raw.isBlank()) return emptySet()
        return raw.split("\n").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    private fun saveSet(key: String, values: Set<String>) {
        val payload = values.joinToString("\n")
        prefs.edit().putString(key, payload).apply()
    }

    companion object {
        private const val KEY_ALLOWLIST = "allow_list"
        private const val KEY_SUSPICIOUS = "suspicious_domains"
    }
}
