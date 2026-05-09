package com.security.rakshakx.integration

import android.content.Context
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.json.JSONObject
import java.nio.LongBuffer

class IndicBertClassifier(private val context: Context) {

    private val TAG = "IndicBertClassifier"
    private var modelPath = "rakshakx_model/indicbert/model.onnx"
    private var vocabPath = "rakshakx_model/indicbert/vocab.txt"

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var vocab: Map<String, Int> = emptyMap()

    private var maxSeqLen = 128
    private var clsTokenId = 2
    private var sepTokenId = 3
    private var padTokenId = 0
    private var unkTokenId = 1

    private var labels = listOf("SAFE", "SCAM", "SUSPICIOUS")

    fun initialize(config: JSONObject? = null, globalLabels: List<String>? = null) {
        config?.let {
            modelPath = it.optString("path", modelPath)
            vocabPath = it.optString("vocab", vocabPath)
            maxSeqLen = it.optInt("max_seq_len", maxSeqLen)
        }
        globalLabels?.let { labels = it }

        try {
            ortEnv = OrtEnvironment.getEnvironment()
            val modelBytes = context.assets.open(modelPath).readBytes()
            ortSession = ortEnv!!.createSession(modelBytes)
            vocab = loadVocab()
            Log.d(TAG, "IndicBERT initialized successfully from $modelPath. Labels: $labels")
        } catch (e: Exception) {
            Log.e(TAG, "IndicBERT init failed: ${e.message}")
        }
    }

    fun classify(text: String, channel: String = "generic"): ModelResult {
        val session = ortSession ?: return fallbackResult(channel)
        val env     = ortEnv    ?: return fallbackResult(channel)

        return try {
            val (inputIds, attentionMask, tokenTypeIds) = tokenize(text)

            val inputIdsTensor = OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(inputIds),
                longArrayOf(1, maxSeqLen.toLong())
            )
            val attentionMaskTensor = OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(attentionMask),
                longArrayOf(1, maxSeqLen.toLong())
            )
            val tokenTypeIdsTensor = OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(tokenTypeIds),
                longArrayOf(1, maxSeqLen.toLong())
            )

            val inputs = mapOf(
                "input_ids"      to inputIdsTensor,
                "attention_mask" to attentionMaskTensor,
                "token_type_ids" to tokenTypeIdsTensor
            )

            val output = session.run(inputs)
            val logits = (output[0].value as Array<*>)[0] as FloatArray

            val probs      = softmax(logits)
            val maxIdx     = probs.indices.maxByOrNull { probs[it] } ?: 0
            val label      = labels.getOrElse(maxIdx) { "SAFE" }
            val confidence = probs[maxIdx]

            inputIdsTensor.close()
            attentionMaskTensor.close()
            tokenTypeIdsTensor.close()
            output.close()

            ModelResult(
                isScam     = label == "SCAM" || label == "SUSPICIOUS",
                confidence = confidence,
                label      = label,
                modelUsed  = "indicbert",
                channel    = channel
            )
        } catch (e: Exception) {
            Log.e(TAG, "IndicBERT inference failed: ${e.message}")
            fallbackResult(channel)
        }
    }

    fun release() {
        ortSession?.close()
        ortEnv?.close()
        ortSession = null
        ortEnv = null
    }

    // ─── Tokenizer ────────────────────────────────────────────────────────────
    // IndicBERT uses SentencePiece-style subword tokenization

    private fun tokenize(text: String): Triple<LongArray, LongArray, LongArray> {
        val cleanText = text.trim()
        val subwords  = sentencePieceTokenize(cleanText)

        val inputIds      = LongArray(maxSeqLen) { padTokenId.toLong() }
        val attentionMask = LongArray(maxSeqLen) { 0L }
        val tokenTypeIds  = LongArray(maxSeqLen) { 0L }

        // [CLS]
        inputIds[0]      = clsTokenId.toLong()
        attentionMask[0] = 1L

        var pos = 1
        for (token in subwords) {
            if (pos >= maxSeqLen - 1) break
            inputIds[pos]      = vocab.getOrDefault(token, unkTokenId).toLong()
            attentionMask[pos] = 1L
            pos++
        }

        // [SEP]
        if (pos < maxSeqLen) {
            inputIds[pos]      = sepTokenId.toLong()
            attentionMask[pos] = 1L
        }

        return Triple(inputIds, attentionMask, tokenTypeIds)
    }

    private fun sentencePieceTokenize(text: String): List<String> {
        val tokens = mutableListOf<String>()
        val words  = text.split(Regex("\\s+"))

        for (word in words) {
            // SentencePiece prefix is U+2581 ' '
            val withPrefix = "\u2581$word"
            if (vocab.containsKey(withPrefix)) {
                tokens.add(withPrefix)
            } else if (vocab.containsKey(word)) {
                tokens.add(word)
            } else {
                // Character-level decomposition as fallback
                for (ch in word) {
                    val charStr = ch.toString()
                    if (vocab.containsKey(charStr)) {
                        tokens.add(charStr)
                    } else {
                        // Try with prefix for character
                        val charWithPrefix = "\u2581$charStr"
                        tokens.add(if (vocab.containsKey(charWithPrefix)) charWithPrefix else "<unk>")
                    }
                }
            }
        }
        return tokens
    }

    private fun loadVocab(): Map<String, Int> {
        return try {
            val map = mutableMapOf<String, Int>()
            context.assets.open(vocabPath).bufferedReader().useLines { lines ->
                lines.forEachIndexed { index, line ->
                    val parts = line.trim().split("\t")
                    val token = parts[0]
                    map[token] = index
                    
                    // Automatically identify special tokens
                    when (token) {
                        "[CLS]", "<s>", "<cls>" -> clsTokenId = index
                        "[SEP]", "</s>", "<sep>" -> sepTokenId = index
                        "[PAD]", "<pad>" -> padTokenId = index
                        "[UNK]", "<unk>" -> unkTokenId = index
                    }
                }
            }
            Log.d(TAG, "IndicBERT vocab loaded: ${map.size} tokens. CLS=$clsTokenId, SEP=$sepTokenId")
            map
        } catch (e: Exception) {
            Log.e(TAG, "IndicBERT vocab load failed: ${e.message}")
            emptyMap()
        }
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val max  = logits.max()!!
        val exps = logits.map { Math.exp((it - max).toDouble()).toFloat() }
        val sum  = exps.sum()
        return exps.map { it / sum }.toFloatArray()
    }

    private fun fallbackResult(channel: String) = ModelResult(
        isScam     = false,
        confidence = 0f,
        label      = "SAFE",
        modelUsed  = "indicbert_unavailable",
        channel    = channel
    )
}