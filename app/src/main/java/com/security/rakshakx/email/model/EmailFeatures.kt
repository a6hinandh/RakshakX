package com.security.rakshakx.email.model

data class EmailFeatures(

    val originalText: String,

    val normalizedText: String,

    val hasLink: Boolean,

    val hasUrgency: Boolean,

    val hasFinancialWords: Boolean,

    val suspiciousIntent: Boolean,

    val suspiciousPhraseCount: Int,

    val excessiveCaps: Boolean,

    val symbolReplacement: Boolean,

    val repeatedSymbols: Boolean,

    val multipleLinks: Boolean,

    val dangerousAttachment: Boolean
)