package com.security.rakshakx.web.analyzers

import kotlin.math.log2

// ─── Data models ────────────────────────────────────────────────────────────

data class TrafficAnomaly(
    val type: AnomalyType,
    val domain: String,
    val description: String,
    val severity: AnomalySeverity,
    val detectedAt: Long = System.currentTimeMillis()
)

enum class AnomalyType {
    DNS_TUNNELING,
    BEACONING,
    DGA_DOMAIN,
    CRYPTOMINING,
    DATA_EXFILTRATION
}

enum class AnomalySeverity { HIGH, MEDIUM, LOW }

// ─── Detector ────────────────────────────────────────────────────────────────

object TrafficAnomalyDetector {

    // domain → sorted list of query timestamps
    private val queryHistory: MutableMap<String, MutableList<Long>> =
        java.util.Collections.synchronizedMap(mutableMapOf())

    // domain → total query count in the current window
    private val queryVolume: MutableMap<String, Int> =
        java.util.Collections.synchronizedMap(mutableMapOf())

    // Known cryptomining pool domains
    private val MINING_DOMAINS = setOf(
        "pool.minergate.com",
        "xmr.pool.supportxmr.com",
        "moneropool.com",
        "pool.minexmr.com",
        "xmrpool.eu",
        "crypto-pool.fr",
        "coinhive.com",
        "authedmine.com",
        "minercryptonight.eu",
        "nanopool.org",
        "ethermine.org",
        "ethpool.org",
        "dwarfpool.com",
        "miningpoolhub.com",
        "2miners.com",
        "f2pool.com",
        "antpool.com",
        "slushpool.com",
        "btc.com",
        "nicehash.com",
        "pool.bitcoin.com",
        "sha256.mining-pool.com",
        "stratum.slushpool.com",
        "pool.zcash.flypool.org"
    )

    // Common English word fragments used for DGA false-positive suppression
    private val COMMON_WORDS = setOf(
        "google", "apple", "amazon", "facebook", "twitter", "microsoft",
        "youtube", "instagram", "reddit", "linkedin", "netflix", "spotify",
        "github", "android", "windows", "content", "static", "update",
        "api", "cdn", "mail", "news", "shop", "store", "media", "cloud"
    )

    private const val WINDOW_MS = 60_000L         // 1-minute rolling window
    private const val FIVE_MIN_MS = 300_000L      // 5-minute window for exfiltration

    // ── Public API ───────────────────────────────────────────────────────────

    /** Record a DNS query for the given domain. Thread-safe. */
    fun recordQuery(domain: String) {
        val now = System.currentTimeMillis()
        synchronized(queryHistory) {
            queryHistory.getOrPut(domain) { mutableListOf() }.add(now)
        }
        synchronized(queryVolume) {
            queryVolume[domain] = (queryVolume[domain] ?: 0) + 1
        }
    }

    /** Run all anomaly detectors and return findings. */
    fun analyze(): List<TrafficAnomaly> {
        val now = System.currentTimeMillis()
        val anomalies = mutableListOf<TrafficAnomaly>()

        // Snapshot both maps for safe iteration
        val historySnapshot: Map<String, List<Long>>
        val volumeSnapshot: Map<String, Int>
        synchronized(queryHistory) { historySnapshot = queryHistory.toMap() }
        synchronized(queryVolume)  { volumeSnapshot  = queryVolume.toMap() }

        anomalies += detectBeaconing(historySnapshot, now)
        anomalies += detectTunneling(historySnapshot, volumeSnapshot, now)
        anomalies += detectExfiltration(historySnapshot, now)

        // Per-domain checks
        historySnapshot.keys.forEach { domain ->
            if (detectDga(domain)) {
                anomalies += TrafficAnomaly(
                    type = AnomalyType.DGA_DOMAIN,
                    domain = domain,
                    description = "Domain exhibits characteristics of algorithmically generated names (high entropy, unusual structure).",
                    severity = AnomalySeverity.HIGH
                )
            }
            if (detectMining(domain)) {
                anomalies += TrafficAnomaly(
                    type = AnomalyType.CRYPTOMINING,
                    domain = domain,
                    description = "Traffic detected to known cryptocurrency mining pool: $domain.",
                    severity = AnomalySeverity.HIGH
                )
            }
        }

        return anomalies.distinctBy { it.type to it.domain }
    }

    // ── Beaconing ────────────────────────────────────────────────────────────

    private fun detectBeaconing(
        history: Map<String, List<Long>>,
        now: Long
    ): List<TrafficAnomaly> {
        val findings = mutableListOf<TrafficAnomaly>()
        for ((domain, timestamps) in history) {
            val recent = timestamps.filter { now - it < WINDOW_MS }
            if (recent.size < 5) continue

            val sorted = recent.sorted()
            val intervals = sorted.zipWithNext { a, b -> (b - a).toDouble() }
            if (intervals.isEmpty()) continue

            val avg = intervals.average()
            val variance = intervals.map { (it - avg) * (it - avg) }.average()

            // Very regular cadence (low variance) at under 2-minute intervals
            if (variance < 5_000_000.0 && avg < 120_000.0) {
                findings += TrafficAnomaly(
                    type = AnomalyType.BEACONING,
                    domain = domain,
                    description = "Regular query cadence detected: avg interval ${avg.toLong() / 1000}s, variance ${variance.toLong()}ms² — possible C2 beaconing.",
                    severity = AnomalySeverity.HIGH
                )
            }
        }
        return findings
    }

    // ── DGA domain ───────────────────────────────────────────────────────────

    internal fun detectDga(domain: String): Boolean {
        // Strip TLD for analysis
        val label = domain.substringBefore('.').lowercase()
        if (label.length < 6) return false  // too short to be DGA

        // Common word suppression
        if (COMMON_WORDS.any { label.contains(it) }) return false

        // 1. Shannon entropy on the registrable label
        val entropy = shannonEntropy(label)
        if (entropy < 3.5) return false

        // 2. Consonant run > 8
        val consonants = "bcdfghjklmnpqrstvwxyz"
        val maxConsonantRun = label.fold(0 to 0) { (max, cur), c ->
            if (c in consonants) max.coerceAtLeast(cur + 1) to cur + 1
            else max to 0
        }.first
        if (maxConsonantRun > 8) return true

        // 3. High digit ratio
        val digitRatio = label.count { it.isDigit() }.toDouble() / label.length
        if (digitRatio > 0.3) return true

        // 4. Long label
        if (label.length > 15) return true

        return false
    }

    // ── DNS tunneling ─────────────────────────────────────────────────────────

    private fun detectTunneling(
        history: Map<String, List<Long>>,
        volume: Map<String, Int>,
        now: Long
    ): List<TrafficAnomaly> {
        val findings = mutableListOf<TrafficAnomaly>()
        for ((domain, timestamps) in history) {
            // Subdomain label length > 30 chars → HIGH
            val labels = domain.split('.')
            val longLabel = labels.firstOrNull { it.length > 30 }
            if (longLabel != null) {
                findings += TrafficAnomaly(
                    type = AnomalyType.DNS_TUNNELING,
                    domain = domain,
                    description = "Subdomain label '${longLabel.take(20)}…' is ${longLabel.length} chars — consistent with DNS data exfiltration.",
                    severity = AnomalySeverity.HIGH
                )
                continue
            }

            // > 50 queries in 1 minute → MEDIUM
            val recentCount = timestamps.count { now - it < WINDOW_MS }
            if (recentCount > 50) {
                findings += TrafficAnomaly(
                    type = AnomalyType.DNS_TUNNELING,
                    domain = domain,
                    description = "$recentCount queries to $domain in the last 60 seconds — possible DNS tunneling.",
                    severity = AnomalySeverity.MEDIUM
                )
            }
        }
        return findings
    }

    // ── Cryptomining ─────────────────────────────────────────────────────────

    internal fun detectMining(domain: String): Boolean =
        MINING_DOMAINS.any { pool ->
            domain == pool || domain.endsWith(".$pool")
        }

    // ── Data exfiltration ────────────────────────────────────────────────────

    private fun detectExfiltration(
        history: Map<String, List<Long>>,
        now: Long
    ): List<TrafficAnomaly> {
        val findings = mutableListOf<TrafficAnomaly>()

        // Group subdomains by registrable domain within the 5-min window
        // e.g. a1.evil.com, a2.evil.com → evil.com gets 2 unique subdomains
        val subdomainCount = mutableMapOf<String, MutableSet<String>>()
        for ((domain, timestamps) in history) {
            val recentCount = timestamps.count { now - it < FIVE_MIN_MS }
            if (recentCount == 0) continue

            val parts = domain.split('.')
            if (parts.size < 3) continue  // no subdomain to count

            // Registrable domain = last two labels
            val registrable = "${parts[parts.size - 2]}.${parts[parts.size - 1]}"
            val sub = parts.dropLast(2).joinToString(".")
            subdomainCount.getOrPut(registrable) { mutableSetOf() }.add(sub)
        }

        for ((baseDomain, subs) in subdomainCount) {
            if (subs.size > 100) {
                findings += TrafficAnomaly(
                    type = AnomalyType.DATA_EXFILTRATION,
                    domain = baseDomain,
                    description = "${subs.size} unique subdomains queried under $baseDomain in 5 minutes — possible DNS exfiltration channel.",
                    severity = AnomalySeverity.HIGH
                )
            }
        }
        return findings
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private fun shannonEntropy(s: String): Double {
        if (s.isEmpty()) return 0.0
        val freq = mutableMapOf<Char, Int>()
        s.forEach { freq[it] = (freq[it] ?: 0) + 1 }
        val len = s.length.toDouble()
        return freq.values.sumOf { cnt ->
            val p = cnt / len
            -p * log2(p)
        }
    }

    /** Clear all recorded state (call between sessions or on demand). */
    fun reset() {
        synchronized(queryHistory) { queryHistory.clear() }
        synchronized(queryVolume)  { queryVolume.clear() }
    }
}
