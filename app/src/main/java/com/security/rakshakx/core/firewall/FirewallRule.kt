package com.security.rakshakx.core.firewall

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

// ─── Data models ────────────────────────────────────────────────────────────

data class FirewallRule(
    val id: Long = 0L,
    val packageName: String,
    val appName: String,
    val allowWifi: Boolean = true,
    val allowMobile: Boolean = true,
    val blockedDomains: List<String> = emptyList(),
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class FirewallAction { ALLOW, BLOCK, WARN }

// ─── JSON serialisation helpers ──────────────────────────────────────────────

private fun FirewallRule.toJson(): JSONObject = JSONObject().apply {
    put("id",             id)
    put("packageName",    packageName)
    put("appName",        appName)
    put("allowWifi",      allowWifi)
    put("allowMobile",    allowMobile)
    put("blockedDomains", JSONArray(blockedDomains))
    put("enabled",        enabled)
    put("createdAt",      createdAt)
}

private fun JSONObject.toFirewallRule(): FirewallRule {
    val domains = mutableListOf<String>()
    val domainArray = optJSONArray("blockedDomains")
    if (domainArray != null) {
        for (i in 0 until domainArray.length()) {
            domains.add(domainArray.getString(i))
        }
    }
    return FirewallRule(
        id            = optLong("id", 0L),
        packageName   = getString("packageName"),
        appName       = getString("appName"),
        allowWifi     = optBoolean("allowWifi", true),
        allowMobile   = optBoolean("allowMobile", true),
        blockedDomains = domains,
        enabled       = optBoolean("enabled", true),
        createdAt     = optLong("createdAt", System.currentTimeMillis())
    )
}

// ─── Store ───────────────────────────────────────────────────────────────────

object FirewallRuleStore {

    private const val PREFS_FILE = "rakshakx_firewall_rules"
    private const val KEY_RULES  = "rules_json"

    private fun getPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /** Persist or update a rule. Keyed by packageName (upsert semantics). */
    fun saveRule(context: Context, rule: FirewallRule) {
        val prefs = getPrefs(context)
        val current = readAllRules(prefs).toMutableList()
        val idx = current.indexOfFirst { it.packageName == rule.packageName }
        if (idx >= 0) current[idx] = rule else current.add(rule)
        writeAllRules(prefs, current)
    }

    /** Return all stored rules. */
    fun getRules(context: Context): List<FirewallRule> =
        readAllRules(getPrefs(context))

    /** Remove the rule for the given package. */
    fun deleteRule(context: Context, packageName: String) {
        val prefs = getPrefs(context)
        val updated = readAllRules(prefs).filter { it.packageName != packageName }
        writeAllRules(prefs, updated)
    }

    /** Return the rule for a specific package, or null if none exists. */
    fun getRule(context: Context, packageName: String): FirewallRule? =
        getRules(context).firstOrNull { it.packageName == packageName }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun readAllRules(
        prefs: android.content.SharedPreferences
    ): List<FirewallRule> {
        val json = prefs.getString(KEY_RULES, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            List(array.length()) { i -> array.getJSONObject(i).toFirewallRule() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeAllRules(
        prefs: android.content.SharedPreferences,
        rules: List<FirewallRule>
    ) {
        val array = JSONArray()
        rules.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_RULES, array.toString()).apply()
    }
}
