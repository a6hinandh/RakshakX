package com.security.rakshakx.email.analyzer

object UrlReputationAnalyzer {

    // Suspicious top-level domains
    private val suspiciousTlds = listOf(

        ".xyz",
        ".tk",
        ".ru",
        ".click",
        ".top",
        ".gq",
        ".work",
        ".support"
    )

    // URL shorteners
    private val shorteners = listOf(

        "bit.ly",
        "tinyurl.com",
        "goo.gl",
        "t.co",
        "is.gd"
    )

    // Fake banking keywords
    private val bankingKeywords = listOf(

        "paypal",
        "bank",
        "sbi",
        "icici",
        "hdfc",
        "axis",
        "upi"
    )

    fun analyzeUrl(

        url: String

    ): List<String> {

        val reasons = mutableListOf<String>()

        val lowerUrl = url.lowercase()

        // Suspicious TLD
        if (
            suspiciousTlds.any {
                lowerUrl.contains(it)
            }
        ) {

            reasons.add(
                "Suspicious domain extension detected"
            )
        }

        // URL shorteners
        if (
            shorteners.any {
                lowerUrl.contains(it)
            }
        ) {

            reasons.add(
                "Shortened URL detected"
            )
        }

        // IP address URLs
        val ipRegex = Regex(

            """https?://\d+\.\d+\.\d+\.\d+"""
        )

        if (
            ipRegex.containsMatchIn(lowerUrl)
        ) {

            reasons.add(
                "IP-based URL detected"
            )
        }

        // Banking impersonation
        if (

            bankingKeywords.any {
                lowerUrl.contains(it)
            }

            &&

            suspiciousTlds.any {
                lowerUrl.contains(it)
            }
        ) {

            reasons.add(
                "Possible banking impersonation"
            )
        }

        return reasons
    }
}