package com.security.rakshakx.web.extractors

import java.net.InetAddress
import java.nio.ByteBuffer

class PacketParser {
    data class ParsedPacket(
        val protocol: String,
        val sourceIp: String,
        val destinationIp: String,
        val sourcePort: Int,
        val destinationPort: Int,
        val payload: ByteArray
    )

    fun parse(packet: ByteArray, length: Int): ParsedPacket? {
        if (length < 1) {
            return null
        }

        val version = (packet[0].toInt() shr 4) and 0x0F
        return when (version) {
            4 -> parseIpv4(packet, length)
            6 -> parseIpv6(packet, length)
            else -> null
        }
    }

    private fun parseIpv4(packet: ByteArray, length: Int): ParsedPacket? {
        if (length < 20) {
            return null
        }

        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (ihl < 20 || length < ihl) {
            return null
        }

        val protocolByte = packet[9].toInt() and 0xFF
        val sourceIp = ipToString(packet, 12, 4)
        val destinationIp = ipToString(packet, 16, 4)

        return parseTransport(packet, length, ihl, protocolByte, sourceIp, destinationIp)
    }

    private fun parseIpv6(packet: ByteArray, length: Int): ParsedPacket? {
        if (length < 40) {
            return null
        }

        val nextHeader = packet[6].toInt() and 0xFF
        val sourceIp = ipToString(packet, 8, 16)
        val destinationIp = ipToString(packet, 24, 16)

        return parseTransport(packet, length, 40, nextHeader, sourceIp, destinationIp)
    }

    private fun parseTransport(
        packet: ByteArray,
        length: Int,
        headerOffset: Int,
        protocolByte: Int,
        sourceIp: String,
        destinationIp: String
    ): ParsedPacket? {
        if (length < headerOffset + 4) {
            return null
        }

        val sourcePort = readUnsignedShort(packet, headerOffset)
        val destinationPort = readUnsignedShort(packet, headerOffset + 2)

        return when (protocolByte) {
            6 -> parseTcp(packet, length, headerOffset, sourceIp, destinationIp, sourcePort, destinationPort)
            17 -> parseUdp(packet, length, headerOffset, sourceIp, destinationIp, sourcePort, destinationPort)
            else -> ParsedPacket("OTHER", sourceIp, destinationIp, sourcePort, destinationPort, ByteArray(0))
        }
    }

    private fun parseTcp(
        packet: ByteArray,
        length: Int,
        headerOffset: Int,
        sourceIp: String,
        destinationIp: String,
        sourcePort: Int,
        destinationPort: Int
    ): ParsedPacket? {
        if (length < headerOffset + 20) {
            return null
        }

        val dataOffset = ((packet[headerOffset + 12].toInt() shr 4) and 0x0F) * 4
        val payloadOffset = headerOffset + dataOffset
        if (payloadOffset > length) {
            return null
        }

        val payload = packet.copyOfRange(payloadOffset, length)
        return ParsedPacket("TCP", sourceIp, destinationIp, sourcePort, destinationPort, payload)
    }

    private fun parseUdp(
        packet: ByteArray,
        length: Int,
        headerOffset: Int,
        sourceIp: String,
        destinationIp: String,
        sourcePort: Int,
        destinationPort: Int
    ): ParsedPacket? {
        if (length < headerOffset + 8) {
            return null
        }

        val payloadOffset = headerOffset + 8
        if (payloadOffset > length) {
            return null
        }

        val payload = packet.copyOfRange(payloadOffset, length)
        return ParsedPacket("UDP", sourceIp, destinationIp, sourcePort, destinationPort, payload)
    }

    private fun readUnsignedShort(packet: ByteArray, offset: Int): Int {
        return ((packet[offset].toInt() and 0xFF) shl 8) or (packet[offset + 1].toInt() and 0xFF)
    }

    private fun ipToString(packet: ByteArray, offset: Int, length: Int): String {
        val addressBytes = packet.copyOfRange(offset, offset + length)
        return InetAddress.getByAddress(addressBytes).hostAddress ?: ""
    }
}
