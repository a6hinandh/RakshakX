package com.security.rakshakx.core.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.WifiInfo
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.net.InetAddress

// ─── Data models ────────────────────────────────────────────────────────────

data class WifiSecurityResult(
    val ssid: String,
    val bssid: String,
    val encryptionType: WifiEncryption,
    val signalStrength: Int,        // dBm
    val frequency: Int,             // MHz  (2400 = 2.4 GHz, 5000 = 5 GHz)
    val isConnected: Boolean,
    val securityScore: Int,         // 0-100
    val threats: List<WifiThreat>,
    val recommendations: List<String>,
    val localIp: String,
    val gatewayIp: String,
    val subnetMask: String,
    val dns1: String,
    val dns2: String,
    val wifiStandard: String,
    val linkSpeed: Int,
    val txSpeed: Int,
    val rxSpeed: Int,
    val gatewayLatencyMs: Long,
    val dnsResolutionLatencyMs: Long,
    val isPrivateDnsActive: Boolean
)

enum class WifiEncryption { OPEN, WEP, WPA, WPA2, WPA3, UNKNOWN }

data class WifiThreat(
    val type: WifiThreatType,
    val severity: ThreatSeverity,
    val description: String
)

enum class WifiThreatType {
    OPEN_NETWORK,
    WEAK_ENCRYPTION,
    EVIL_TWIN,
    DNS_HIJACK,
    CAPTIVE_PORTAL
}

enum class ThreatSeverity { CRITICAL, HIGH, MEDIUM, LOW }

// ─── Analyzer ───────────────────────────────────────────────────────────────

class WifiSecurityAnalyzer private constructor(private val context: Context) {

    companion object {
        @Volatile private var instance: WifiSecurityAnalyzer? = null

        fun getInstance(context: Context): WifiSecurityAnalyzer =
            instance ?: synchronized(this) {
                instance ?: WifiSecurityAnalyzer(context.applicationContext).also { instance = it }
            }

        // Known Google connectivity-check IPs (142.250.x.x range)
        private val KNOWN_CONNECTIVITY_IPS = setOf(
            "142.250.0.0",   // anchor for range check
        )

        private const val CONNECTIVITY_CHECK_HOST = "connectivitycheck.gstatic.com"

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
            "nicehash.com"
        )
    }

    @Suppress("DEPRECATION")
    suspend fun analyze(): WifiSecurityResult = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Wi-Fi state permission not granted. Please grant ACCESS_WIFI_STATE in device settings.")
        }

        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager

        val connInfo = wifiManager.connectionInfo
        val ssid = connInfo.ssid?.removeSurrounding("\"") ?: "<unknown>"
        val bssid = connInfo.bssid ?: ""
        val rssi = connInfo.rssi
        val freq = connInfo.frequency

        // ── IP Configuration Details ────────────────────────────────────
        val dhcpInfo = wifiManager.dhcpInfo
        val localIp = intToIpString(dhcpInfo?.ipAddress ?: 0)
        val gatewayIp = intToIpString(dhcpInfo?.gateway ?: 0)
        val subnetMask = intToIpString(dhcpInfo?.netmask ?: 0)
        val dns1 = intToIpString(dhcpInfo?.dns1 ?: 0)
        val dns2 = intToIpString(dhcpInfo?.dns2 ?: 0)

        // ── Wi-Fi Protocol / Standard ────────────────────────────────────
        val wifiStandard = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            when (connInfo.wifiStandard) {
                6 -> "Wi-Fi 6 (802.11ax)"
                5 -> "Wi-Fi 5 (802.11ac)"
                4 -> "Wi-Fi 4 (802.11n)"
                1 -> "Legacy (802.11a/b/g)"
                8 -> "Wi-Fi 7 (802.11be)"
                else -> "Unknown Standard"
            }
        } else {
            "Legacy / Unknown"
        }

        // ── Link Speeds ──────────────────────────────────────────────────
        val linkSpeed = connInfo.linkSpeed
        val txSpeed = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            connInfo.txLinkSpeedMbps
        } else {
            -1
        }
        val rxSpeed = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            connInfo.rxLinkSpeedMbps
        } else {
            -1
        }

        // ── Fetch scan results ───────────────────────────────────────────
        val scanResults = wifiManager.scanResults ?: emptyList()

        // Match BSSID first (most reliable), fall back to SSID
        val matchedScan = scanResults.firstOrNull { it.BSSID == bssid }
            ?: scanResults.firstOrNull { it.SSID == ssid }

        val capabilities = matchedScan?.capabilities ?: ""

        // ── Parse encryption ─────────────────────────────────────────────
        val encryption = parseEncryption(capabilities)

        // ── Base score from encryption ───────────────────────────────────
        var score = when (encryption) {
            WifiEncryption.WPA3    -> 100
            WifiEncryption.WPA2    -> 85
            WifiEncryption.WPA     -> 50
            WifiEncryption.WEP     -> 20
            WifiEncryption.OPEN    -> 0
            WifiEncryption.UNKNOWN -> 40
        }

        val threats = mutableListOf<WifiThreat>()
        val recommendations = mutableListOf<String>()

        // ── Threat: open network ─────────────────────────────────────────
        if (encryption == WifiEncryption.OPEN) {
            score = (score - 40).coerceAtLeast(0)
            threats.add(
                WifiThreat(
                    type = WifiThreatType.OPEN_NETWORK,
                    severity = ThreatSeverity.CRITICAL,
                    description = "Network has no encryption — all traffic is transmitted in plaintext."
                )
            )
            recommendations.add("Use a VPN immediately to encrypt your traffic on this open network.")
            recommendations.add("Avoid accessing sensitive accounts (banking, email) on this network.")
        }

        // ── Threat: WEP ─────────────────────────────────────────────────
        if (encryption == WifiEncryption.WEP) {
            score = (score - 30).coerceAtLeast(0)
            threats.add(
                WifiThreat(
                    type = WifiThreatType.WEAK_ENCRYPTION,
                    severity = ThreatSeverity.HIGH,
                    description = "WEP encryption is broken and can be cracked in under a minute."
                )
            )
            recommendations.add("Change your router's security to WPA2 or WPA3 immediately.")
        }

        // ── Threat: WPA only (not WPA2/3) ───────────────────────────────
        if (encryption == WifiEncryption.WPA) {
            score = (score - 15).coerceAtLeast(0)
            threats.add(
                WifiThreat(
                    type = WifiThreatType.WEAK_ENCRYPTION,
                    severity = ThreatSeverity.MEDIUM,
                    description = "WPA (TKIP) has known vulnerabilities. Upgrade to WPA2/WPA3."
                )
            )
            recommendations.add("Update your router firmware and switch to WPA2-AES or WPA3.")
        }

        // ── Evil twin detection: multiple scan results with same SSID ────
        val ssidDuplicates = scanResults.filter { it.SSID == ssid }
        if (ssidDuplicates.size > 1) {
            // Different BSSID with same SSID and strong signal is suspicious
            val suspicious = ssidDuplicates.filter { it.BSSID != bssid && it.level > -65 }
            if (suspicious.isNotEmpty()) {
                score = (score - 20).coerceAtLeast(0)
                threats.add(
                    WifiThreat(
                        type = WifiThreatType.EVIL_TWIN,
                        severity = ThreatSeverity.HIGH,
                        description = "Detected ${suspicious.size} other access point(s) broadcasting the same SSID with strong signal — possible evil-twin attack."
                    )
                )
                recommendations.add("Verify the access point's MAC address with your network administrator.")
                recommendations.add("Disconnect and use mobile data until the network is verified.")
            }
        }

        // ── Private DNS Security Check ───────────────────────────────────
        val privateDnsActive = isPrivateDnsEnabled(context)
        if (!privateDnsActive) {
            score = (score - 10).coerceAtLeast(0)
            threats.add(
                WifiThreat(
                    type = WifiThreatType.DNS_HIJACK,
                    severity = ThreatSeverity.LOW,
                    description = "Private DNS (DoT/DoH) is disabled. Your DNS queries are vulnerable to local interception/eavesdropping."
                )
            )
            recommendations.add("Configure Private DNS in your Android Settings to encrypt lookup queries.")
        }

        // ── Parallel network probes (DNS hijack, Captive portal, Latency checks) ──
        val dnsDeferred = async { testDnsHijack() }
        val portalDeferred = async { detectCaptivePortal() }
        val gatewayLatencyDeferred = async { measureGatewayLatency(gatewayIp) }
        val dnsLatencyDeferred = async { measureDnsLatency() }

        val dnsHijacked = dnsDeferred.await()
        val captivePortal = portalDeferred.await()
        val gatewayLatency = gatewayLatencyDeferred.await()
        val dnsResolutionLatency = dnsLatencyDeferred.await()

        if (dnsHijacked) {
            score = (score - 20).coerceAtLeast(0)
            threats.add(
                WifiThreat(
                    type = WifiThreatType.DNS_HIJACK,
                    severity = ThreatSeverity.HIGH,
                    description = "DNS resolution for $CONNECTIVITY_CHECK_HOST returned an unexpected IP — possible DNS hijacking."
                )
            )
            recommendations.add("Use a trusted DNS provider (e.g. 1.1.1.1 or 8.8.8.8) via your VPN.")
            recommendations.add("Avoid logging in to any accounts on this network until DNS is verified.")
        }

        if (captivePortal) {
            threats.add(
                WifiThreat(
                    type = WifiThreatType.CAPTIVE_PORTAL,
                    severity = ThreatSeverity.LOW,
                    description = "Network appears to have a captive portal — your traffic may be intercepted before authentication."
                )
            )
            recommendations.add("Complete portal login before transmitting any sensitive data.")
        }

        // ── Diagnostic Latency recommendations ───────────────────────────
        if (dnsResolutionLatency > 300) {
            recommendations.add("DNS resolution is slow (${dnsResolutionLatency}ms). Consider configuring a faster public DNS (e.g. Cloudflare 1.1.1.1).")
        }
        if (gatewayLatency > 150) {
            recommendations.add("High router response latency (${gatewayLatency}ms). Check network load or wireless interference.")
        }

        // ── Generic recommendations ──────────────────────────────────────
        if (recommendations.isEmpty()) {
            recommendations.add("Your network appears secure. Keep your router firmware updated.")
        }

        score = score.coerceIn(0, 100)

        WifiSecurityResult(
            ssid = ssid,
            bssid = bssid,
            encryptionType = encryption,
            signalStrength = rssi,
            frequency = freq,
            isConnected = bssid.isNotEmpty() && bssid != "02:00:00:00:00:00",
            securityScore = score,
            threats = threats,
            recommendations = recommendations,
            localIp = localIp,
            gatewayIp = gatewayIp,
            subnetMask = subnetMask,
            dns1 = dns1,
            dns2 = dns2,
            wifiStandard = wifiStandard,
            linkSpeed = linkSpeed,
            txSpeed = txSpeed,
            rxSpeed = rxSpeed,
            gatewayLatencyMs = gatewayLatency,
            dnsResolutionLatencyMs = dnsResolutionLatency,
            isPrivateDnsActive = privateDnsActive
        )
    }

    // ────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────

    private fun parseEncryption(capabilities: String): WifiEncryption {
        if (capabilities.isBlank()) return WifiEncryption.OPEN
        return when {
            capabilities.contains("WPA3", ignoreCase = true) ||
                    capabilities.contains("SAE", ignoreCase = true) -> WifiEncryption.WPA3

            capabilities.contains("WPA2", ignoreCase = true) ||
                    capabilities.contains("RSN", ignoreCase = true) -> WifiEncryption.WPA2

            capabilities.contains("WPA", ignoreCase = true) -> WifiEncryption.WPA

            capabilities.contains("WEP", ignoreCase = true) -> WifiEncryption.WEP

            // No known security tag means open
            !capabilities.contains("PSK") &&
                    !capabilities.contains("EAP") &&
                    !capabilities.contains("SAE") -> WifiEncryption.OPEN

            else -> WifiEncryption.UNKNOWN
        }
    }

    private fun testDnsHijack(): Boolean {
        return try {
            val resolved = InetAddress.getByName(CONNECTIVITY_CHECK_HOST).hostAddress ?: return false
            // Google's connectivity check should resolve to 142.250.x.x or 216.58.x.x range
            val isGoogle = resolved.startsWith("142.250.") ||
                    resolved.startsWith("142.251.") ||
                    resolved.startsWith("216.58.") ||
                    resolved.startsWith("172.217.") ||
                    resolved.startsWith("74.125.")
            !isGoogle
        } catch (_: Exception) {
            false // Network error — not conclusive
        }
    }

    private fun detectCaptivePortal(): Boolean {
        return try {
            val url = java.net.URL("http://connectivitycheck.gstatic.com/generate_204")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.instanceFollowRedirects = false
            val code = conn.responseCode
            conn.disconnect()
            code != 204 // 204 = no content, expected from Google check
        } catch (_: Exception) {
            false
        }
    }

    private fun intToIpString(ipInt: Int): String {
        return "%d.%d.%d.%d".format(
            ipInt and 0xFF,
            (ipInt shr 8) and 0xFF,
            (ipInt shr 16) and 0xFF,
            (ipInt shr 24) and 0xFF
        )
    }

    private fun measureGatewayLatency(gatewayIp: String): Long {
        if (gatewayIp == "0.0.0.0" || gatewayIp.isEmpty()) return -1L
        return try {
            val start = System.currentTimeMillis()
            val address = InetAddress.getByName(gatewayIp)
            if (address.isReachable(500)) {
                System.currentTimeMillis() - start
            } else {
                var connected = false
                for (port in listOf(80, 443)) {
                    try {
                        java.net.Socket().use { socket ->
                            socket.connect(java.net.InetSocketAddress(address, port), 200)
                            connected = true
                        }
                    } catch (_: Exception) {}
                    if (connected) break
                }
                if (connected) System.currentTimeMillis() - start else -1L
            }
        } catch (_: Exception) {
            -1L
        }
    }

    private fun measureDnsLatency(): Long {
        return try {
            val start = System.currentTimeMillis()
            InetAddress.getByName("www.google.com")
            System.currentTimeMillis() - start
        } catch (_: Exception) {
            -1L
        }
    }

    private fun isPrivateDnsEnabled(context: Context): Boolean {
        return try {
            val mode = android.provider.Settings.Global.getString(
                context.contentResolver,
                "private_dns_mode"
            )
            mode != null && mode != "off"
        } catch (_: Exception) {
            false
        }
    }
}
