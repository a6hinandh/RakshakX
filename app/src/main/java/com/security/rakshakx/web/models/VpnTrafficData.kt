package com.security.rakshakx.web.models

data class VpnTrafficData(
    val timestamp: Long,
    val protocol: String,
    val sourceIp: String,
    val destinationIp: String,
    val sourcePort: Int,
    val destinationPort: Int,
    val dnsQuery: String,
    val sniHost: String,
    val redirectChain: List<String>
)
