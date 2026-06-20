package com.security.rakshakx.web.services

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Pure-Kotlin DNS relay for the VPN TUN interface.
 *
 * Reads DNS queries from the TUN fd, forwards them to real upstream DNS servers
 * via protected UDP sockets, and writes responses back to the TUN fd.
 *
 * Also sends TCP RST for any TCP connections (e.g. DNS-over-TLS on port 853)
 * so Android falls back to plain DNS immediately instead of waiting for a timeout.
 */
class DnsVpnRelay(
    private val vpnService: VpnService,
    private val tunDescriptor: ParcelFileDescriptor,
    private val scope: CoroutineScope,
    private val upstreamDns: List<InetAddress> = listOf(
        InetAddress.getByName("1.1.1.1"),
        InetAddress.getByName("8.8.8.8")
    )
) {
    private var relayJob: Job? = null
    private val writeMutex = Mutex()

    /** Callback invoked for every packet flowing through the relay. Returns true if the packet should be blocked. */
    var onDnsPacket: ((rawPacket: ByteArray, length: Int) -> Boolean)? = null

    fun start() {
        relayJob = scope.launch { runRelay() }
        Log.i(TAG, "DNS relay started, upstream=${upstreamDns.map { it.hostAddress }}")
    }

    fun stop() {
        relayJob?.cancel()
        relayJob = null
    }

    private suspend fun runRelay() {
        val input = FileInputStream(tunDescriptor.fileDescriptor)
        val output = FileOutputStream(tunDescriptor.fileDescriptor)
        val buffer = ByteArray(32767)

        while (scope.isActive) {
            val length = withContext(Dispatchers.IO) {
                try {
                    input.read(buffer)
                } catch (_: Exception) {
                    -1
                }
            }
            if (length <= 0) continue

            val packet = buffer.copyOfRange(0, length)

            // Notify observer for threat analysis and check if it should be blocked
            var shouldBlock = false
            try {
                shouldBlock = onDnsPacket?.invoke(packet, length) == true
            } catch (_: Exception) {}

            when {
                isIpv4UdpDns(packet, length) -> {
                    // Forward DNS queries asynchronously so we don't block reads
                    scope.launch(Dispatchers.IO) {
                        try {
                            val response = if (shouldBlock) {
                                buildBlockedDnsResponse(packet, length)
                            } else {
                                buildDnsResponse(packet, length)
                            }
                            
                            if (response != null) {
                                writeMutex.withLock {
                                    output.write(response)
                                    output.flush()
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "DNS forward error", e)
                        }
                    }
                }
                isIpv4Tcp(packet, length) -> {
                    // Immediately reject TCP connections (e.g. DNS-over-TLS port 853)
                    // with a RST so Android falls back to plain UDP DNS instantly
                    scope.launch(Dispatchers.IO) {
                        try {
                            val rst = buildTcpRst(packet, length)
                            if (rst != null) {
                                writeMutex.withLock {
                                    output.write(rst)
                                    output.flush()
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "TCP RST error", e)
                        }
                    }
                }
            }
        }
    }

    // ===================== Packet classification =====================

    private fun isIpv4UdpDns(packet: ByteArray, length: Int): Boolean {
        if (length < 28) return false
        if ((packet[0].toInt() shr 4) and 0x0F != 4) return false
        if (packet[9].toInt() and 0xFF != 17) return false
        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (length < ihl + 8) return false
        return readUint16(packet, ihl + 2) == 53
    }

    private fun isIpv4Tcp(packet: ByteArray, length: Int): Boolean {
        if (length < 40) return false
        if ((packet[0].toInt() shr 4) and 0x0F != 4) return false
        if (packet[9].toInt() and 0xFF != 6) return false
        return true
    }

    // ===================== DNS forwarding =====================

    private fun buildDnsResponse(packet: ByteArray, length: Int): ByteArray? {
        val ihl = (packet[0].toInt() and 0x0F) * 4
        val dnsOffset = ihl + 8
        if (dnsOffset >= length) return null

        val srcIp = copyBytes(packet, 12, 4)
        val dstIp = copyBytes(packet, 16, 4)
        val srcPort = readUint16(packet, ihl)
        val dstPort = readUint16(packet, ihl + 2)
        val dnsPayload = packet.copyOfRange(dnsOffset, length)

        val responsePayload = forwardDns(dnsPayload) ?: return null

        return buildIpv4UdpPacket(
            srcIp = dstIp, dstIp = srcIp,
            srcPort = dstPort, dstPort = srcPort,
            payload = responsePayload
        )
    }

    private fun buildBlockedDnsResponse(packet: ByteArray, length: Int): ByteArray? {
        val ihl = (packet[0].toInt() and 0x0F) * 4
        val dnsOffset = ihl + 8
        if (dnsOffset >= length) return null

        val srcIp = copyBytes(packet, 12, 4)
        val dstIp = copyBytes(packet, 16, 4)
        val srcPort = readUint16(packet, ihl)
        val dstPort = readUint16(packet, ihl + 2)
        val dnsPayload = packet.copyOfRange(dnsOffset, length)

        // Parse query to construct a fake response
        // Transaction ID (2 bytes)
        val txId = copyBytes(dnsPayload, 0, 2)
        // Flags: QR=1, Opcode=0, AA=1, TC=0, RD=1, RA=1, Z=0, RCODE=0
        val flags = byteArrayOf(0x81.toByte(), 0x80.toByte())
        
        // Find end of query name to get QTYPE and QCLASS
        var qOffset = 12
        while (qOffset < dnsPayload.size && dnsPayload[qOffset].toInt() != 0) {
            qOffset += dnsPayload[qOffset].toInt() + 1
        }
        qOffset += 1 // skip null byte
        
        if (qOffset + 4 > dnsPayload.size) return null // invalid query

        // We only care if it's an A record (type 1) or AAAA record (type 28).
        // Let's just return a generic A record pointing to 0.0.0.0
        val querySection = copyBytes(dnsPayload, 12, qOffset + 4 - 12)
        
        // Response payload size: 12 (header) + query section + 16 (answer section)
        val responsePayload = ByteArray(12 + querySection.size + 16)
        
        // Header
        System.arraycopy(txId, 0, responsePayload, 0, 2)
        System.arraycopy(flags, 0, responsePayload, 2, 2)
        // QDCOUNT = 1
        responsePayload[4] = 0; responsePayload[5] = 1
        // ANCOUNT = 1
        responsePayload[6] = 0; responsePayload[7] = 1
        // NSCOUNT = 0, ARCOUNT = 0 (leave 0)
        
        // Query Section
        System.arraycopy(querySection, 0, responsePayload, 12, querySection.size)
        
        // Answer Section
        var ansOffset = 12 + querySection.size
        // Name (Pointer to query name at offset 12)
        responsePayload[ansOffset++] = 0xC0.toByte()
        responsePayload[ansOffset++] = 0x0C.toByte()
        // Type (A = 1)
        responsePayload[ansOffset++] = 0
        responsePayload[ansOffset++] = 1
        // Class (IN = 1)
        responsePayload[ansOffset++] = 0
        responsePayload[ansOffset++] = 1
        // TTL (300)
        responsePayload[ansOffset++] = 0
        responsePayload[ansOffset++] = 0
        responsePayload[ansOffset++] = 1
        responsePayload[ansOffset++] = 0x2C.toByte()
        // RDLENGTH (4 bytes for IPv4)
        responsePayload[ansOffset++] = 0
        responsePayload[ansOffset++] = 4
        // RDATA (0.0.0.0)
        responsePayload[ansOffset++] = 0
        responsePayload[ansOffset++] = 0
        responsePayload[ansOffset++] = 0
        responsePayload[ansOffset++] = 0

        return buildIpv4UdpPacket(
            srcIp = dstIp, dstIp = srcIp,
            srcPort = dstPort, dstPort = srcPort,
            payload = responsePayload
        )
    }

    private fun forwardDns(query: ByteArray): ByteArray? {
        for (upstream in upstreamDns) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                vpnService.protect(socket)
                socket.soTimeout = 3000
                socket.send(DatagramPacket(query, query.size, upstream, 53))
                val buf = ByteArray(4096)
                val response = DatagramPacket(buf, buf.size)
                socket.receive(response)
                return buf.copyOfRange(0, response.length)
            } catch (e: Exception) {
                Log.w(TAG, "DNS upstream ${upstream.hostAddress} failed", e)
            } finally {
                socket?.close()
            }
        }
        return null
    }

    // ===================== TCP RST (reject DoT) =====================

    private fun buildTcpRst(packet: ByteArray, length: Int): ByteArray? {
        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (length < ihl + 20) return null

        val flags = packet[ihl + 13].toInt() and 0xFF
        // Only RST SYN packets (don't RST a RST)
        if (flags and 0x04 != 0) return null // Already a RST
        if (flags and 0x02 == 0) return null // Not a SYN

        val srcIp = copyBytes(packet, 12, 4)
        val dstIp = copyBytes(packet, 16, 4)
        val srcPort = readUint16(packet, ihl)
        val dstPort = readUint16(packet, ihl + 2)
        val seqNum = readUint32(packet, ihl + 4)

        // Build a minimal RST+ACK response
        val ipLen = 20
        val tcpLen = 20
        val total = ipLen + tcpLen
        val rst = ByteArray(total)

        // IPv4 header
        rst[0] = 0x45.toByte()
        writeUint16(rst, 2, total)
        writeUint16(rst, 6, 0x4000) // Don't Fragment
        rst[8] = 64 // TTL
        rst[9] = 6  // TCP
        System.arraycopy(dstIp, 0, rst, 12, 4) // swap src/dst
        System.arraycopy(srcIp, 0, rst, 16, 4)
        writeUint16(rst, 10, computeChecksum(rst, 0, ipLen))

        // TCP header
        val t = ipLen
        writeUint16(rst, t, dstPort)      // swap ports
        writeUint16(rst, t + 2, srcPort)
        writeUint32(rst, t + 4, 0)         // seq = 0
        writeUint32(rst, t + 8, seqNum + 1) // ack = their seq + 1
        rst[t + 12] = 0x50.toByte()        // data offset = 5 (20 bytes)
        rst[t + 13] = 0x14.toByte()        // flags: RST + ACK
        writeUint16(rst, t + 14, 0)        // window = 0

        // TCP checksum
        writeUint16(rst, t + 16, computeTcpChecksum(dstIp, srcIp, rst, t, tcpLen))

        return rst
    }

    // ===================== Packet construction =====================

    private fun buildIpv4UdpPacket(
        srcIp: ByteArray, dstIp: ByteArray,
        srcPort: Int, dstPort: Int,
        payload: ByteArray
    ): ByteArray {
        val ipLen = 20
        val udpLen = 8 + payload.size
        val total = ipLen + udpLen
        val pkt = ByteArray(total)

        // IPv4 header
        pkt[0] = 0x45.toByte()
        writeUint16(pkt, 2, total)
        writeUint16(pkt, 6, 0x4000)
        pkt[8] = 64; pkt[9] = 17
        System.arraycopy(srcIp, 0, pkt, 12, 4)
        System.arraycopy(dstIp, 0, pkt, 16, 4)
        writeUint16(pkt, 10, computeChecksum(pkt, 0, ipLen))

        // UDP header + payload
        val u = ipLen
        writeUint16(pkt, u, srcPort)
        writeUint16(pkt, u + 2, dstPort)
        writeUint16(pkt, u + 4, udpLen)
        System.arraycopy(payload, 0, pkt, u + 8, payload.size)
        writeUint16(pkt, u + 6, computeUdpChecksum(srcIp, dstIp, pkt, u, udpLen))

        return pkt
    }

    // ===================== Checksum helpers =====================

    private fun computeChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var i = offset; var rem = length
        while (rem > 1) { sum += readUint16(data, i).toLong(); i += 2; rem -= 2 }
        if (rem == 1) sum += (data[i].toInt() and 0xFF).toLong() shl 8
        while (sum shr 16 != 0L) sum = (sum and 0xFFFF) + (sum shr 16)
        return sum.toInt().inv() and 0xFFFF
    }

    private fun computeUdpChecksum(
        srcIp: ByteArray, dstIp: ByteArray,
        pkt: ByteArray, offset: Int, udpLen: Int
    ): Int {
        val pseudo = ByteArray(12)
        System.arraycopy(srcIp, 0, pseudo, 0, 4)
        System.arraycopy(dstIp, 0, pseudo, 4, 4)
        pseudo[9] = 17; writeUint16(pseudo, 10, udpLen)

        val saved = byteArrayOf(pkt[offset + 6], pkt[offset + 7])
        pkt[offset + 6] = 0; pkt[offset + 7] = 0

        var sum = 0L
        for (i in 0 until 12 step 2) sum += readUint16(pseudo, i).toLong()
        var i = offset; var rem = udpLen
        while (rem > 1) { sum += readUint16(pkt, i).toLong(); i += 2; rem -= 2 }
        if (rem == 1) sum += (pkt[i].toInt() and 0xFF).toLong() shl 8

        pkt[offset + 6] = saved[0]; pkt[offset + 7] = saved[1]
        while (sum shr 16 != 0L) sum = (sum and 0xFFFF) + (sum shr 16)
        val r = sum.toInt().inv() and 0xFFFF
        return if (r == 0) 0xFFFF else r
    }

    private fun computeTcpChecksum(
        srcIp: ByteArray, dstIp: ByteArray,
        pkt: ByteArray, offset: Int, tcpLen: Int
    ): Int {
        val pseudo = ByteArray(12)
        System.arraycopy(srcIp, 0, pseudo, 0, 4)
        System.arraycopy(dstIp, 0, pseudo, 4, 4)
        pseudo[9] = 6; writeUint16(pseudo, 10, tcpLen)

        val saved = byteArrayOf(pkt[offset + 16], pkt[offset + 17])
        pkt[offset + 16] = 0; pkt[offset + 17] = 0

        var sum = 0L
        for (i in 0 until 12 step 2) sum += readUint16(pseudo, i).toLong()
        var i = offset; var rem = tcpLen
        while (rem > 1) { sum += readUint16(pkt, i).toLong(); i += 2; rem -= 2 }
        if (rem == 1) sum += (pkt[i].toInt() and 0xFF).toLong() shl 8

        pkt[offset + 16] = saved[0]; pkt[offset + 17] = saved[1]
        while (sum shr 16 != 0L) sum = (sum and 0xFFFF) + (sum shr 16)
        val r = sum.toInt().inv() and 0xFFFF
        return if (r == 0) 0xFFFF else r
    }

    // ===================== Byte helpers =====================

    private fun readUint16(d: ByteArray, o: Int) =
        ((d[o].toInt() and 0xFF) shl 8) or (d[o + 1].toInt() and 0xFF)

    private fun readUint32(d: ByteArray, o: Int): Long =
        ((d[o].toInt() and 0xFF).toLong() shl 24) or
        ((d[o + 1].toInt() and 0xFF).toLong() shl 16) or
        ((d[o + 2].toInt() and 0xFF).toLong() shl 8) or
        (d[o + 3].toInt() and 0xFF).toLong()

    private fun writeUint16(d: ByteArray, o: Int, v: Int) {
        d[o] = (v shr 8 and 0xFF).toByte(); d[o + 1] = (v and 0xFF).toByte()
    }

    private fun writeUint32(d: ByteArray, o: Int, v: Long) {
        d[o] = (v shr 24 and 0xFF).toByte(); d[o + 1] = (v shr 16 and 0xFF).toByte()
        d[o + 2] = (v shr 8 and 0xFF).toByte(); d[o + 3] = (v and 0xFF).toByte()
    }

    private fun copyBytes(src: ByteArray, offset: Int, length: Int) =
        src.copyOfRange(offset, offset + length)

    companion object {
        private const val TAG = "RakshakX-DNS"
    }
}
