package com.security.rakshakx.core.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

// ─── Data models ────────────────────────────────────────────────────────────

data class NetworkDevice(
    val ipAddress: String,
    val macAddress: String?,
    val hostname: String?,
    val openPorts: List<OpenPort>,
    val deviceType: DeviceType,
    val riskLevel: DeviceRisk,
    val vendor: String?,
    val customName: String? = null,
    val isTrusted: Boolean = false
)

data class OpenPort(val port: Int, val service: String, val isRisky: Boolean)

enum class DeviceType { ROUTER, COMPUTER, PHONE, TABLET, IOT_DEVICE, PRINTER, TV, UNKNOWN }
enum class DeviceRisk { SAFE, SUSPICIOUS, HIGH_RISK }

// ─── Scanner ─────────────────────────────────────────────────────────────────

class LocalNetworkScanner private constructor(private val context: Context) {

    companion object {
        @Volatile private var instance: LocalNetworkScanner? = null

        fun getInstance(context: Context): LocalNetworkScanner =
            instance ?: synchronized(this) {
                instance ?: LocalNetworkScanner(context.applicationContext).also { instance = it }
            }

        // Ports to probe on each live host
        private val PROBE_PORTS = listOf(
            21   to "FTP",
            22   to "SSH",
            23   to "Telnet",
            80   to "HTTP",
            443  to "HTTPS",
            445  to "SMB",
            1883 to "MQTT",
            3389 to "RDP",
            5683 to "CoAP",
            5900 to "VNC",
            8080 to "HTTP-Alt",
            8443 to "HTTPS-Alt"
        )

        // Risky ports
        private val RISKY_PORTS = setOf(21, 23, 445, 1883, 3389, 5900)

        // OUI prefix → (vendor name, likely device type)
        private val OUI_MAP = mapOf(
            "00:17:f2" to Pair("Apple",         DeviceType.COMPUTER),
            "00:1b:63" to Pair("Apple",         DeviceType.PHONE),
            "00:26:bb" to Pair("Apple",         DeviceType.PHONE),
            "ac:de:48" to Pair("Apple",         DeviceType.PHONE),
            "b8:27:eb" to Pair("Raspberry Pi",  DeviceType.IOT_DEVICE),
            "dc:a6:32" to Pair("Raspberry Pi",  DeviceType.IOT_DEVICE),
            "e4:5f:01" to Pair("Raspberry Pi",  DeviceType.IOT_DEVICE),
            "00:16:3e" to Pair("Samsung",       DeviceType.PHONE),
            "8c:77:12" to Pair("Samsung",       DeviceType.TV),
            "fc:f1:36" to Pair("Samsung",       DeviceType.PHONE),
            "00:50:f2" to Pair("Microsoft",     DeviceType.COMPUTER),
            "28:d2:44" to Pair("Microsoft",     DeviceType.COMPUTER),
            "00:0c:29" to Pair("VMware",        DeviceType.COMPUTER),
            "00:1a:11" to Pair("Google",        DeviceType.IOT_DEVICE),
            "f4:f5:d8" to Pair("Google",        DeviceType.IOT_DEVICE),
            "50:c7:bf" to Pair("TP-Link",       DeviceType.ROUTER),
            "ec:08:6b" to Pair("TP-Link",       DeviceType.ROUTER),
            "00:1e:e5" to Pair("Cisco",         DeviceType.ROUTER),
            "cc:46:d6" to Pair("Cisco",         DeviceType.ROUTER),
            "00:18:0a" to Pair("HP",            DeviceType.PRINTER)
        )
    }

    @Suppress("DEPRECATION")
    suspend fun scanNetwork(): List<NetworkDevice> = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Wi-Fi state permission not granted. Please grant ACCESS_WIFI_STATE in device settings.")
        }

        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager

        val dhcpInfo = wifiManager.dhcpInfo
        val localIpInt = dhcpInfo.ipAddress

        // Convert little-endian int IP to string
        val localIp = intToIpString(localIpInt)
        val subnet = localIp.substringBeforeLast('.')

        // Pre-parse ARP table to find already known active devices
        val arpIps = getActiveArpIps()

        // Parallel reachability check for all .1-.254
        val reachableIps = (1..254).map { host ->
            async {
                val ip = "$subnet.$host"
                if (arpIps.contains(ip)) {
                    ip
                } else {
                    try {
                        val reachable = InetAddress.getByName(ip).isReachable(150)
                        if (reachable) ip else null
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }.awaitAll().filterNotNull()

        val store = NetworkDeviceStore(context)
        
        // For each reachable IP, probe ports and build device profile
        reachableIps.map { ip ->
            async { buildDeviceProfile(ip, subnet, store) }
        }.awaitAll()
    }

    private suspend fun buildDeviceProfile(ip: String, subnet: String, store: NetworkDeviceStore): NetworkDevice {
        val lastOctet = ip.substringAfterLast('.').toIntOrNull() ?: 0

        // Hostname resolution (best-effort)
        val hostname = try {
            InetAddress.getByName(ip).canonicalHostName.takeIf { it != ip }
        } catch (_: Exception) { null }

        // MAC and vendor (from ARP cache via /proc/net/arp)
        val mac = readArpMac(ip)
        val ouiKey = mac?.substring(0, 8)?.lowercase()
        val (vendor, ouiType) = OUI_MAP[ouiKey] ?: Pair(null, null)
        
        // Load metadata
        val meta = if (mac != null) store.getDeviceMeta(mac) else DeviceMeta()

        // Port scan
        val openPorts = PROBE_PORTS.mapNotNull { (port, service) ->
            val open = isTcpPortOpen(ip, port, timeoutMs = 300)
            if (open) OpenPort(port, service, port in RISKY_PORTS) else null
        }

        // Device type heuristic
        val deviceType = when {
            ouiType != null                                          -> ouiType
            lastOctet == 1                                           -> DeviceType.ROUTER
            openPorts.any { it.port == 9100 || it.port == 631 }     -> DeviceType.PRINTER
            openPorts.any { it.port == 1883 || it.port == 5683 }    -> DeviceType.IOT_DEVICE
            openPorts.size >= 4                                      -> DeviceType.COMPUTER
            openPorts.any { it.port == 80 && lastOctet == 1 }       -> DeviceType.ROUTER
            else                                                     -> DeviceType.UNKNOWN
        }

        // Risk level
        val riskLevel = when {
            openPorts.any { it.port == 23 }           -> DeviceRisk.HIGH_RISK  // Telnet
            openPorts.any { it.port == 445 }           -> DeviceRisk.SUSPICIOUS // SMB
            openPorts.any { it.port == 5900 }          -> DeviceRisk.SUSPICIOUS // VNC
            openPorts.any { it.port == 3389 }          -> DeviceRisk.SUSPICIOUS // RDP
            openPorts.any { it.isRisky }               -> DeviceRisk.SUSPICIOUS
            else                                        -> DeviceRisk.SAFE
        }

        return NetworkDevice(
            ipAddress = ip,
            macAddress = mac,
            hostname = hostname,
            openPorts = openPorts,
            deviceType = deviceType,
            riskLevel = riskLevel,
            vendor = vendor,
            customName = meta.customName,
            isTrusted = meta.isTrusted
        )
    }

    private fun isTcpPortOpen(host: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun readArpMac(ip: String): String? {
        return try {
            java.io.File("/proc/net/arp")
                .readLines()
                .firstOrNull { it.startsWith(ip) }
                ?.split(Regex("\\s+"))
                ?.getOrNull(3)
                ?.takeIf { it.matches(Regex("[0-9a-fA-F:]{17}")) }
        } catch (_: Exception) {
            null
        }
    }

    private fun getActiveArpIps(): Set<String> {
        val ips = mutableSetOf<String>()
        try {
            val file = java.io.File("/proc/net/arp")
            if (file.exists()) {
                val lines = file.readLines()
                if (lines.size > 1) {
                    for (i in 1 until lines.size) {
                        val tokens = lines[i].split(Regex("\\s+"))
                        if (tokens.size >= 4) {
                            val ip = tokens[0]
                            val mac = tokens[3]
                            if (mac.matches(Regex("[0-9a-fA-F:]{17}")) && mac != "00:00:00:00:00:00") {
                                ips.add(ip)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return ips
    }

    private fun intToIpString(ipInt: Int): String {
        // WifiManager gives the IP in little-endian format
        return "%d.%d.%d.%d".format(
            ipInt and 0xFF,
            (ipInt shr 8) and 0xFF,
            (ipInt shr 16) and 0xFF,
            (ipInt shr 24) and 0xFF
        )
    }

    // Wake on LAN Magic Packet
    suspend fun sendWakeOnLan(macAddress: String) = withContext(Dispatchers.IO) {
        try {
            val macBytes = getMacBytes(macAddress)
            val bytes = ByteArray(6 + 16 * macBytes.size)
            for (i in 0..5) bytes[i] = 0xff.toByte()
            for (i in 6 until bytes.size step macBytes.size) {
                System.arraycopy(macBytes, 0, bytes, i, macBytes.size)
            }
            
            val address = InetAddress.getByName("255.255.255.255")
            val packet = DatagramPacket(bytes, bytes.size, address, 9)
            val socket = DatagramSocket()
            socket.broadcast = true
            socket.send(packet)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getMacBytes(macStr: String): ByteArray {
        val bytes = ByteArray(6)
        val hex = macStr.split(":")
        if (hex.size != 6) throw IllegalArgumentException("Invalid MAC address.")
        try {
            for (i in 0..5) {
                bytes[i] = hex[i].toInt(16).toByte()
            }
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid hex digit in MAC address.")
        }
        return bytes
    }
}
