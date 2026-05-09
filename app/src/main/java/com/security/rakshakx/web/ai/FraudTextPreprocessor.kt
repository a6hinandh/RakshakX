package com.security.rakshakx.web.ai

import android.content.Context
import java.util.Locale

class FraudTextPreprocessor(
    context: Context,
    private val maxLength: Int = 128,
    vocabAssetPath: String = DEFAULT_VOCAB_PATH
) {
    private val vocab = loadVocab(context, vocabAssetPath)

    fun toModelInput(text: String): ModelInput {
        val tokens = tokenize(text)
        val ids = IntArray(maxLength)
        val mask = IntArray(maxLength)

        var index = 0
        ids[index] = vocab[CLS_TOKEN] ?: 101
        mask[index] = 1
        index++

        for (token in tokens) {
            if (index >= maxLength - 1) break
            ids[index] = vocab[token] ?: (vocab[UNK_TOKEN] ?: 100)
            mask[index] = 1
            index++
        }

        if (index < maxLength) {
            ids[index] = vocab[SEP_TOKEN] ?: 102
            mask[index] = 1
        }

        return ModelInput(ids, mask)
    }

    fun buildText(
        title: String,
        visibleText: String,
        url: String,
        dnsDomain: String,
        dnsReasons: List<String>,
        fieldHints: List<String>
    ): String {
        return buildString {
            append("title: ").append(title).append('\n')
            append("url: ").append(url).append('\n')
            append("dns: ").append(dnsDomain).append('\n')
            if (dnsReasons.isNotEmpty()) {
                append("dns_flags: ").append(dnsReasons.joinToString(" | ")).append('\n')
            }
            if (fieldHints.isNotEmpty()) {
                append("form_hints: ").append(fieldHints.joinToString(" | ")).append('\n')
            }
            append("text: ").append(visibleText)
        }
    }

    private fun tokenize(text: String): List<String> {
        val normalized = text.lowercase(Locale.US)
        val cleaned = normalized.replace(Regex("[^a-z0-9]+"), " ")
        return cleaned.split(' ').filter { it.isNotBlank() }
    }

    private fun loadVocab(context: Context, assetPath: String): Map<String, Int> {
        return context.assets.open(assetPath).bufferedReader().useLines { lines ->
            lines.withIndex().associate { it.value.trim() to it.index }
        }
    }

    companion object {
        private const val CLS_TOKEN = "[CLS]"
        private const val SEP_TOKEN = "[SEP]"
        private const val UNK_TOKEN = "[UNK]"
        private const val DEFAULT_VOCAB_PATH = "models/vocab.txt"
    }
}
