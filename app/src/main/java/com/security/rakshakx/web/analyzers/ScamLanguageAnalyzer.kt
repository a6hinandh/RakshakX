package com.security.rakshakx.web.analyzers

class ScamLanguageAnalyzer {
    data class ScamLanguageResult(
        val score: Int,
        val matches: List<String>
    )

    private data class PhrasePattern(
        val regex: Regex,
        val weight: Int,
        val label: String
    )

    private val patternsByLocale = mutableMapOf<String, MutableList<PhrasePattern>>()

    init {
        registerDefaults()
    }

    fun analyze(text: String, locale: String = DEFAULT_LOCALE): ScamLanguageResult {
        val normalized = text.lowercase()
        val patterns = patternsByLocale[locale] ?: patternsByLocale[DEFAULT_LOCALE].orEmpty()

        var score = 0
        val matches = mutableSetOf<String>()
        for (pattern in patterns) {
            if (pattern.regex.containsMatchIn(normalized)) {
                score += pattern.weight
                matches.add(pattern.label)
            }
        }

        return ScamLanguageResult(score.coerceAtMost(40), matches.toList())
    }

    fun addPattern(locale: String, regex: String, weight: Int, label: String) {
        val list = patternsByLocale.getOrPut(locale) { mutableListOf() }
        list.add(PhrasePattern(Regex(regex, RegexOption.IGNORE_CASE), weight, label))
    }

    private fun registerDefaults() {
        val list = patternsByLocale.getOrPut(DEFAULT_LOCALE) { mutableListOf() }
        list.addAll(
            listOf(
                PhrasePattern(Regex("verify( immediately| now| account)?", RegexOption.IGNORE_CASE), 8, "verify immediately"),
                PhrasePattern(Regex("account (suspended|locked|disabled)", RegexOption.IGNORE_CASE), 10, "account suspended"),
                PhrasePattern(Regex("urgent action required", RegexOption.IGNORE_CASE), 9, "urgent action required"),
                PhrasePattern(Regex("security alert|unusual activity", RegexOption.IGNORE_CASE), 7, "security alert"),
                PhrasePattern(Regex("claim (refund|reward|prize)", RegexOption.IGNORE_CASE), 8, "claim refund"),
                PhrasePattern(Regex("kyc (update|verification)", RegexOption.IGNORE_CASE), 9, "kyc update"),
                PhrasePattern(Regex("bank verification|banking verification", RegexOption.IGNORE_CASE), 9, "bank verification"),
                PhrasePattern(Regex("payment (failed|declined|pending)", RegexOption.IGNORE_CASE), 8, "payment failed"),
                PhrasePattern(Regex("gift (reward|voucher|card)", RegexOption.IGNORE_CASE), 6, "gift reward"),
                PhrasePattern(Regex("support call|call support|helpline", RegexOption.IGNORE_CASE), 7, "support call")
            )
        )
    }

    companion object {
        private const val DEFAULT_LOCALE = "en"
    }
}
