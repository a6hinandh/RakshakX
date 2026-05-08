package com.security.rakshakx.email.model

data class RiskResult(

    val score: Int,

    val riskLevel: String,

    val reasons: List<String>
)