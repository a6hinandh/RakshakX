package com.security.rakshakx.web.ai

data class ModelInput(
    val inputIds: IntArray,
    val attentionMask: IntArray
)

interface OnDeviceFraudModel {
    fun predict(input: ModelInput): Float?
}
