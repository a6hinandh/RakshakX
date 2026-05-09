package com.rakshakx.ai.inference

import android.util.Log
import com.rakshakx.ai.loader.ModelLoader
import com.rakshakx.ai.loader.SmsClassifierHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class FraudInferenceEngine(
    private val modelLoader: ModelLoader
) {
    private val maxDebugScores = 8

    @Volatile
    private var smsClassifierHandle: SmsClassifierHandle? = null
    private val recentScoresState = MutableStateFlow<List<Float>>(emptyList())
    private val modelLoadedState = MutableStateFlow(false)

    suspend fun classifySms(message: String): Float = withContext(Dispatchers.Default) {
        val score = runCatching {
            val handle = getOrLoadHandle() ?: return@withContext 0.0f
            val inputVector = preprocessSms(message, handle.inputVectorSize)
            val output = Array(1) { FloatArray(handle.outputVectorSize) }
            handle.interpreter.run(arrayOf(inputVector), output)
            output[0][0].coerceIn(0.0f, 1.0f)
        }.getOrElse { error ->
            Log.e("FraudInferenceEngine", "SMS classifier inference failed", error)
            0.0f
        }
        appendDebugScore(score)
        score
    }

    fun isModelLoaded(): Boolean = smsClassifierHandle != null
    fun observeRecentScores(): StateFlow<List<Float>> = recentScoresState.asStateFlow()
    fun observeModelLoaded(): StateFlow<Boolean> = modelLoadedState.asStateFlow()

    private fun getOrLoadHandle(): SmsClassifierHandle? {
        val existing = smsClassifierHandle
        if (existing != null) return existing

        return synchronized(this) {
            val doubleCheck = smsClassifierHandle
            if (doubleCheck != null) {
                doubleCheck
            } else {
                runCatching { modelLoader.loadSmsClassifier() }
                    .onFailure { Log.e("FraudInferenceEngine", "Failed to load SMS classifier", it) }
                    .getOrNull()
                    ?.also {
                        smsClassifierHandle = it
                        modelLoadedState.value = true
                    }
            }
        }
    }

    private fun preprocessSms(message: String, vectorSize: Int): FloatArray {
        val normalized = message.lowercase().trim()
        val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        val vector = FloatArray(vectorSize)
        tokens.take(vectorSize).forEachIndexed { index, token ->
            val bucket = (token.hashCode().toUInt().toLong() % 1000L).toFloat() / 1000f
            vector[index] = bucket
        }
        return vector
    }

    private fun appendDebugScore(score: Float) {
        val clipped = score.coerceIn(0.0f, 1.0f)
        val current = recentScoresState.value
        val updated = (listOf(clipped) + current).take(maxDebugScores)
        recentScoresState.value = updated
    }
}
