package com.rakshakx.callanalysis

import android.content.Context
import android.util.Log
import com.rakshakx.callanalysis.ml.DummyFraudTextModel
import com.rakshakx.callanalysis.ml.FraudTextModel
import com.rakshakx.callanalysis.ml.MlFraudResult

data class MLResult(
    val score: Float,
    val label: String,
    val reasons: List<String>
)

/**
 * Wrapper around the Tier-1 ML classifier.
 * Delegates to a pluggable FraudTextModel (dummy today, IndicBERT TFLite later).
 */
class FraudMLClassifier(
    private val model: FraudTextModel
) {

    /**
     * Secondary constructor to allow initialization with Context.
     * Currently uses DummyFraudTextModel, but will eventually use a TFLite model.
     */
    constructor(context: Context) : this(DummyFraudTextModel())


    companion object {
        private const val TAG = "FraudMLClassifier"
    }

    /**
     * Compute ML-based fraud result including score, label and reasons.
     */
    fun computeMLResult(transcript: String): MLResult {
        return try {
            if (transcript.isBlank()) {
                MLResult(
                    score = 0.1f,
                    label = "unknown",
                    reasons = listOf("Empty transcript")
                )
            } else {
                val mlResult: MlFraudResult = model.predictFraud(transcript)

                MLResult(
                    score = mlResult.probabilityFraud,
                    label = mlResult.label,
                    reasons = mlResult.reasons
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "ML inference failed", e)
            MLResult(
                score = 0.5f,
                label = "unknown",
                reasons = listOf("Inference error")
            )
        }
    }
}