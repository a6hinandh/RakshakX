package com.security.rakshakx.web.extractors

import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class DnsTrafficAnalyzer {
    data class DnsResult(
        val domain: String,
        val suspicious: Boolean,
        val reasons: List<String>
    )

    private val domainHits = ConcurrentHashMap<String, Int>()
    private val highRiskTlds = setOf("zip", "xyz", "top", "gq", "work", "click", "help")
    private val phishingKeywords = listOf("login", "verify", "secure", "bank", "support", "update")

    fun analyze(parsedPacket: PacketParser.ParsedPacket): DnsResult? {
        if (!isDnsPacket(parsedPacket)) {
            return null
        }

        val domain = parseDnsQuery(parsedPacket.payload) ?: return null
        val normalized = domain.lowercase(Locale.US)
        domainHits[normalized] = (domainHits[normalized] ?: 0) + 1

        val reasons = mutableListOf<String>()
        val tld = normalized.substringAfterLast('.', "")
        if (tld in highRiskTlds) {
            reasons.add("High-risk TLD: .$tld")
        }

        if (normalized.count { it == '.' } >= 4) {
            reasons.add("Excessive subdomains")
        }

        if (phishingKeywords.any { normalized.contains(it) }) {
            reasons.add("Phishing keyword in domain")
        }

        if (normalized.length > 45) {
            reasons.add("Long domain length")
        }

        return DnsResult(domain = normalized, suspicious = reasons.isNotEmpty(), reasons = reasons)
    }

    private fun isDnsPacket(parsedPacket: PacketParser.ParsedPacket): Boolean {
        return parsedPacket.destinationPort == 53 || parsedPacket.sourcePort == 53
    }

    private fun parseDnsQuery(payload: ByteArray): String? {
        if (payload.size < 12) {
            return null
        }

        var offset = 12
        if (payload.size > 2 && payload[0].toInt() == 0) {
            val tcpLength = ((payload[0].toInt() and 0xFF) shl 8) or (payload[1].toInt() and 0xFF)
            if (tcpLength > 0 && payload.size >= tcpLength + 2) {
                offset = 14
            }
        }

        val labels = StringBuilder()
        while (offset < payload.size) {
            val len = payload[offset].toInt() and 0xFF
            if (len == 0) {
                break
            }
            if (len > 63 || offset + len >= payload.size) {
                return null
            }
            if (labels.isNotEmpty()) {
                labels.append('.')
            }
            labels.append(String(payload, offset + 1, len))
            offset += len + 1
        }

        return labels.toString().takeIf { it.isNotBlank() }
    }
}
