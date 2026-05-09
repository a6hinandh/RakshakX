package com.rakshakx.callanalysis.ml

data class MlFraudResult(
    val probabilityFraud: Float,
    val label: String = "unknown",
    val reasons: List<String> = emptyList()
)

interface FraudTextModel {
    fun predictFraud(text: String): MlFraudResult
}