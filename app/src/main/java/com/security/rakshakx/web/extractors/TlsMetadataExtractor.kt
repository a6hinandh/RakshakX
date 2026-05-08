package com.security.rakshakx.web.extractors

class TlsMetadataExtractor {
    fun extractSni(parsedPacket: PacketParser.ParsedPacket): String {
        if (parsedPacket.protocol != "TCP") {
            return ""
        }

        val payload = parsedPacket.payload
        if (payload.size < 5) {
            return ""
        }

        val contentType = payload[0].toInt() and 0xFF
        if (contentType != 0x16) {
            return ""
        }

        val recordLength = ((payload[3].toInt() and 0xFF) shl 8) or (payload[4].toInt() and 0xFF)
        if (payload.size < 5 + recordLength) {
            return ""
        }

        var offset = 5
        val handshakeType = payload[offset].toInt() and 0xFF
        if (handshakeType != 0x01) {
            return ""
        }

        offset += 4
        offset += 2
        offset += 32
        if (offset >= payload.size) {
            return ""
        }

        val sessionIdLength = payload[offset].toInt() and 0xFF
        offset += 1 + sessionIdLength
        if (offset + 2 > payload.size) {
            return ""
        }

        val cipherSuitesLength = readUint16(payload, offset)
        offset += 2 + cipherSuitesLength
        if (offset >= payload.size) {
            return ""
        }

        val compressionMethodsLength = payload[offset].toInt() and 0xFF
        offset += 1 + compressionMethodsLength
        if (offset + 2 > payload.size) {
            return ""
        }

        val extensionsLength = readUint16(payload, offset)
        offset += 2
        val extensionsEnd = offset + extensionsLength

        while (offset + 4 <= extensionsEnd && offset + 4 <= payload.size) {
            val extType = readUint16(payload, offset)
            val extLen = readUint16(payload, offset + 2)
            offset += 4
            if (extType == 0x0000) {
                return parseServerName(payload, offset, extLen)
            }
            offset += extLen
        }

        return ""
    }

    private fun parseServerName(payload: ByteArray, offsetStart: Int, extLen: Int): String {
        var offset = offsetStart
        if (offset + 2 > payload.size) {
            return ""
        }

        val listLength = readUint16(payload, offset)
        offset += 2
        val listEnd = offset + listLength
        if (listEnd > offsetStart + extLen || listEnd > payload.size) {
            return ""
        }

        while (offset + 3 <= listEnd) {
            val nameType = payload[offset].toInt() and 0xFF
            val nameLen = readUint16(payload, offset + 1)
            offset += 3
            if (nameType == 0 && offset + nameLen <= payload.size) {
                return String(payload, offset, nameLen)
            }
            offset += nameLen
        }

        return ""
    }

    private fun readUint16(payload: ByteArray, offset: Int): Int {
        return ((payload[offset].toInt() and 0xFF) shl 8) or (payload[offset + 1].toInt() and 0xFF)
    }
}
