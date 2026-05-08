package com.security.rakshakx.web.extractors

class RedirectChainTracker {
    private val recentDomains = ArrayDeque<Pair<String, Long>>()
    private val timeWindowMs = 12_000L

    fun track(domain: String): List<String> {
        val now = System.currentTimeMillis()
        while (recentDomains.isNotEmpty() && now - recentDomains.first().second > timeWindowMs) {
            recentDomains.removeFirst()
        }

        if (recentDomains.lastOrNull()?.first != domain) {
            recentDomains.addLast(domain to now)
        }

        return recentDomains.map { it.first }
    }
}
