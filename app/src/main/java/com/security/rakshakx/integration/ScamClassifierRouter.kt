package com.security.rakshakx.integration

import android.content.Context
import android.util.Log
import com.security.rakshakx.sms.RiskEngine
import org.json.JSONObject

class ScamClassifierRouter(private val context: Context) {

    private val TAG = "ScamClassifierRouter"

    private val distilBert = DistilBertClassifier(context)
    private val indicBert = IndicBertClassifier(context)

    // Config loaded from assets/rakshakx_model/model_config.json
    private var confidenceThreshold = 0.75f
    private var hinglishThreshold = 0.65f
    private var languageDetectionThreshold = 0.15f
    private val indicLanguages = mutableSetOf<String>()

    // Model specific configs
    private var distilBertConfig = JSONObject()
    private var indicBertConfig = JSONObject()
    private var labelsList = listOf("SAFE", "SCAM", "SUSPICIOUS")

    init {
        loadConfig()
        distilBert.initialize(distilBertConfig, labelsList)
        indicBert.initialize(indicBertConfig, labelsList)
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    fun classify(text: String, channel: String = "generic"): ModelResult {
        Log.d(TAG, "─── Hybrid Classification Started [$channel] ───")
        
        if (text.isBlank()) {
            return ModelResult(isScam = false, confidence = 0f, label = "SAFE", modelUsed = "none", channel = channel)
        }

        // 1. Calculate Keyword Rules Score (0-100)
        val ruleScore = try {
            RiskEngine.calculate(text)
        } catch (e: Exception) {
            0
        }
        val ruleWeight = 0.40f

        // 2. ML Pipeline Logic
        // Step 0: Extract actual content
        val messageLines = text.lines()
        val analysisContent = if (messageLines.size > 1 && 
            (messageLines[0].startsWith("From:") || messageLines[0].startsWith("Caller:") || messageLines[0].startsWith("URL:"))) {
            messageLines.drop(1).joinToString(" ")
        } else {
            text
        }

        // Step 1: Run DistilBERT
        var mlResult = try {
            distilBert.classify(text, channel)
        } catch (e: Exception) {
            Log.e(TAG, "DistilBERT failed: ${e.message}")
            null
        }

        // Step 2: Route to IndicBERT if low confidence + Indic language
        if (mlResult == null || (mlResult.confidence < confidenceThreshold)) {
            val detectedLang = detectLanguage(analysisContent)
            if (detectedLang in indicLanguages && detectedLang != "en") {
                Log.d(TAG, "Low ML confidence, routing to IndicBERT ($detectedLang)")
                try {
                    mlResult = indicBert.classify(text, channel)
                } catch (e: Exception) {
                    Log.e(TAG, "IndicBERT failed: ${e.message}")
                }
            }
        }

        // 3. Hybrid Calculation
        val mlConfidence = mlResult?.confidence ?: 0f
        val mlWeight = 0.60f
        
        // If ML says it's SAFE, we treat its scam-confidence as low for the average
        val mlScamProb = if (mlResult?.label == "SCAM" || mlResult?.label == "SUSPICIOUS") mlConfidence else (1.0f - mlConfidence)
        
        val finalScore = (mlScamProb * mlWeight) + ((ruleScore / 100f) * ruleWeight)
        
        // Alert threshold: if finalScore >= 0.40 (40%), we mark as SCAM
        val isScam = finalScore >= 0.40f
        val finalLabel = when {
            finalScore >= 0.70f -> "SCAM"
            finalScore >= 0.40f -> "SUSPICIOUS"
            else -> "SAFE"
        }

        val result = ModelResult(
            isScam = isScam,
            confidence = mlConfidence,
            label = finalLabel,
            modelUsed = mlResult?.modelUsed ?: "none",
            channel = channel,
            ruleScore = ruleScore,
            finalScore = finalScore
        )

        Log.d(TAG, "Hybrid Result: $result")
        return result
    }

    fun release() {
        distilBert.release()
        indicBert.release()
    }

    // ─── Language Detection (Unicode block ranges, zero dependencies) ─────────

    private fun detectLanguage(text: String): String {
        if (text.isBlank()) return "en"

        // Filter out non-letter chars for accurate ratio calculation
        val filtered = text.filter { it.isLetter() }
        val total = filtered.length.toFloat()
        if (total == 0f) return "en"

        var devanagari = 0  // Hindi, Marathi, Nepali, Sanskrit
        var tamil = 0
        var telugu = 0
        var kannada = 0
        var malayalam = 0
        var bengali = 0
        var gujarati = 0
        var gurmukhi = 0   // Punjabi
        var odia = 0
        var urdu = 0       // Arabic script

        for (ch in filtered) {
            when (ch) {
                in '\u0900'..'\u097F' -> devanagari++
                in '\u0B80'..'\u0BFF' -> tamil++
                in '\u0C00'..'\u0C7F' -> telugu++
                in '\u0C80'..'\u0CFF' -> kannada++
                in '\u0D00'..'\u0D7F' -> malayalam++
                in '\u0980'..'\u09FF' -> bengali++
                in '\u0A80'..'\u0AFF' -> gujarati++
                in '\u0A00'..'\u0A7F' -> gurmukhi++
                in '\u0B00'..'\u0B7F' -> odia++
                in '\u0600'..'\u06FF' -> urdu++
            }
        }

        val threshold = languageDetectionThreshold
        val minChars = 6 // Minimum characters in script to detect language

        return when {
            devanagari / total > threshold || devanagari >= minChars -> "hi"
            tamil      / total > threshold || tamil      >= minChars -> "ta"
            telugu     / total > threshold || telugu     >= minChars -> "te"
            kannada    / total > threshold || kannada    >= minChars -> "kn"
            malayalam  / total > threshold || malayalam  >= minChars -> "ml"
            bengali    / total > threshold || bengali    >= minChars -> "bn"
            gujarati   / total > threshold || gujarati   >= minChars -> "gu"
            gurmukhi   / total > threshold || gurmukhi   >= minChars -> "pa"
            odia       / total > threshold || odia       >= minChars -> "or"
            urdu       / total > threshold || urdu       >= minChars -> "ur"
            else -> "en"
        }
    }

    // ─── Config Loader ────────────────────────────────────────────────────────

    private fun loadConfig() {
        try {
            val json = context.assets
                .open("rakshakx_model/model_config.json")
                .bufferedReader()
                .use { it.readText() }

            val config = JSONObject(json)

            confidenceThreshold = config.optDouble("distilbert_confidence_threshold", 0.75).toFloat()
            hinglishThreshold   = config.optDouble("hinglish_threshold", 0.65).toFloat()
            languageDetectionThreshold = config.optDouble("language_detection_threshold", 0.15).toFloat()

            val models = config.optJSONObject("models")
            if (models != null) {
                distilBertConfig = models.optJSONObject("distilbert") ?: JSONObject()
                indicBertConfig  = models.optJSONObject("indicbert")  ?: JSONObject()
            }

            val labelsArr = config.optJSONArray("labels")
            if (labelsArr != null) {
                val list = mutableListOf<String>()
                for (i in 0 until labelsArr.length()) {
                    list.add(labelsArr.getString(i))
                }
                labelsList = list
            }

            val langs = config.optJSONArray("indic_languages")
            if (langs != null) {
                for (i in 0 until langs.length()) {
                    indicLanguages.add(langs.getString(i))
                }
            } else {
                indicLanguages.addAll(
                    listOf("hi", "ta", "te", "kn", "ml", "mr", "bn", "gu", "pa", "ur", "or")
                )
            }

            Log.d(TAG, "Config loaded: threshold=$confidenceThreshold, indicLangs=$indicLanguages, labels=$labelsList")
        } catch (e: Exception) {
            Log.w(TAG, "model_config.json not found, using defaults: ${e.message}")
            confidenceThreshold = 0.75f
            hinglishThreshold   = 0.65f
            languageDetectionThreshold = 0.15f
            indicLanguages.addAll(
                listOf("hi", "ta", "te", "kn", "ml", "mr", "bn", "gu", "pa", "ur", "or")
            )
        }
    }
}
