package com.security.rakshakx.email.analyzer

object UrlAnalyzer {

    private val suspiciousDomains = listOf(

        ".xyz",
        ".tk",
        "bit.ly",
        "tinyurl",
        "secure-login",
        "verify-now"
    )

    fun extractUrls(text: String): List<String> {

        val regex = Regex(
            "(http|https)://[a-zA-Z0-9./?=_-]+"
        )

        return regex.findAll(text)
            .map { it.value }
            .toList()
    }

    fun hasSuspiciousUrl(urls: List<String>): Boolean {

        return urls.any { url ->

            suspiciousDomains.any {
                url.contains(it, true)
            }
        }
    }

    fun hasMultipleLinks(urls: List<String>): Boolean {

        return urls.size >= 2
    }
}