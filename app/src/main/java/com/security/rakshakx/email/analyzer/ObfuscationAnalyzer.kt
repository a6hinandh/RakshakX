package com.security.rakshakx.email.analyzer

object ObfuscationAnalyzer {

    fun hasExcessiveCaps(text: String): Boolean {

        val uppercaseCount = text.count {
            it.isUpperCase()
        }

        return uppercaseCount >= 10
    }

    fun hasSymbolReplacement(text: String): Boolean {

        return listOf(
            "@",
            "$",
            "0",
            "1",
            "3",
            "4",
            "5",
            "7"
        ).any {
            text.contains(it)
        }
    }

    fun hasRepeatedSymbols(text: String): Boolean {

        return Regex("[!?.]{3,}")
            .containsMatchIn(text)
    }
}