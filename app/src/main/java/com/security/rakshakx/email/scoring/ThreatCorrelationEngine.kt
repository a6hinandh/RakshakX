package com.security.rakshakx.email.scoring

object ThreatCorrelationEngine {

    // Local URL frequency map
    private val urlFrequencyMap =

        mutableMapOf<String, Int>()

    fun processUrls(

        urls: List<String>

    ): List<String> {

        val reasons = mutableListOf<String>()

        urls.forEach { url ->

            val count =

                urlFrequencyMap.getOrDefault(
                    url,
                    0
                ) + 1

            urlFrequencyMap[url] = count

            // Repeated phishing detection
            if (count >= 3) {

                reasons.add(

                    "Repeated suspicious URL detected ($count times)"
                )
            }
        }

        return reasons
    }
}