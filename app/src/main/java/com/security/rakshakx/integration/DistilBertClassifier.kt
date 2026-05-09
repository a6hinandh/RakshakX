package com.security.rakshakx.integration

import android.content.Context
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.json.JSONObject
import java.nio.LongBuffer

class DistilBertClassifier(private val context: Context) {

    private val TAG = "DistilBertClassifier"
    private var modelPath = "rakshakx_model/distilbert/model.onnx"
    private var vocabPath = "rakshakx_model/distilbert/vocab.txt"

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var vocab: Map<String, Int> = emptyMap()

    private var maxSeqLen = 128
    private val CLS_TOKEN = "[CLS]"
    private val SEP_TOKEN = "[SEP]"
    private val PAD_TOKEN = "[PAD]"
    private val UNK_TOKEN = "[UNK]"

    // Labels from training
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
            Log.d(TAG, "DistilBERT initialized successfully from $modelPath. Labels: $labels")
        } catch (e: Exception) {
            Log.e(TAG, "DistilBERT init failed: ${e.message}")
        }
    }

    fun classify(text: String, channel: String = "generic"): ModelResult {
        val session = ortSession ?: return fallbackResult(channel)
        val env     = ortEnv    ?: return fallbackResult(channel)

        return try {
            val (inputIds, attentionMask) = tokenize(text)

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

            val inputs = mapOf(
                "input_ids"      to inputIdsTensor,
                "attention_mask" to attentionMaskTensor
            )

            val output = session.run(inputs)
            val logits = (output[0].value as Array<*>)[0] as FloatArray

            val probs    = softmax(logits)
            val maxIdx   = probs.indices.maxByOrNull { probs[it] } ?: 0
            val label    = labels.getOrElse(maxIdx) { "SAFE" }
            val confidence = probs[maxIdx]

            inputIdsTensor.close()
            attentionMaskTensor.close()
            output.close()

            ModelResult(
                isScam     = label == "SCAM" || label == "SUSPICIOUS",
                confidence = confidence,
                label      = label,
                modelUsed  = "distilbert",
                channel    = channel
            )
        } catch (e: Exception) {
            Log.e(TAG, "DistilBERT inference failed: ${e.message}")
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

    private fun tokenize(text: String): Pair<LongArray, LongArray> {
        val cleanText = text.lowercase().trim()
        val words     = cleanText.split(Regex("\\s+"))

        val tokens = mutableListOf<String>()
        tokens.add(CLS_TOKEN)

        for (word in words) {
            val wordPieces = wordpieceTokenize(word)
            tokens.addAll(wordPieces)
            if (tokens.size >= maxSeqLen - 1) break
        }
        tokens.add(SEP_TOKEN)

        val inputIds     = LongArray(maxSeqLen) { vocab[PAD_TOKEN]?.toLong() ?: 0L }
        val attentionMask = LongArray(maxSeqLen) { 0L }

        for (i in tokens.indices.take(maxSeqLen)) {
            inputIds[i]      = vocab.getOrDefault(tokens[i], vocab[UNK_TOKEN] ?: 100).toLong()
            attentionMask[i] = 1L
        }

        return Pair(inputIds, attentionMask)
    }

    private fun wordpieceTokenize(word: String): List<String> {
        if (vocab.containsKey(word)) return listOf(word)

        val tokens = mutableListOf<String>()
        var start  = 0
        while (start < word.length) {
            var end   = word.length
            var found = false
            while (start < end) {
                val substr = if (start == 0) word.substring(start, end)
                else "##" + word.substring(start, end)
                if (vocab.containsKey(substr)) {
                    tokens.add(substr)
                    start = end
                    found = true
                    break
                }
                end--
            }
            if (!found) {
                tokens.add(UNK_TOKEN)
                break
            }
        }
        return tokens
    }

    private fun loadVocab(): Map<String, Int> {
        return try {
            val map = mutableMapOf<String, Int>()
            context.assets.open(vocabPath).bufferedReader().useLines { lines ->
                lines.forEachIndexed { index, line -> map[line.trim()] = index }
            }
            Log.d(TAG, "Vocab loaded: ${map.size} tokens")
            map
        } catch (e: Exception) {
            Log.e(TAG, "Vocab load failed: ${e.message}")
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
        modelUsed  = "distilbert_unavailable",
        channel    = channel
    )
}