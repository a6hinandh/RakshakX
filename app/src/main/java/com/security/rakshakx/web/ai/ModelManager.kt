package com.security.rakshakx.web.ai

import android.content.Context
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.LongBuffer

class ModelManager(private val context: Context) {
    private var tfliteModel: Interpreter? = null
    private var onnxSession: OrtSession? = null
    private var onnxEnv: OrtEnvironment? = null

    fun loadTfliteModel(assetPath: String = DEFAULT_TFLITE_MODEL): OnDeviceFraudModel {
        if (tfliteModel == null) {
            val buffer = loadAssetBuffer(assetPath)
            tfliteModel = Interpreter(buffer)
        }

        return object : OnDeviceFraudModel {
            override fun predict(input: ModelInput): Float? {
                val interpreter = tfliteModel ?: return null
                return runTflite(interpreter, input)
            }
        }
    }

    fun loadOnnxModel(assetPath: String = DEFAULT_ONNX_MODEL): OnDeviceFraudModel {
        if (onnxSession == null) {
            onnxEnv = OrtEnvironment.getEnvironment()
            val modelBytes = loadAssetBytes(assetPath)
            onnxSession = onnxEnv?.createSession(modelBytes)
        }

        return object : OnDeviceFraudModel {
            override fun predict(input: ModelInput): Float? {
                val env = onnxEnv ?: return null
                val session = onnxSession ?: return null
                return runOnnx(env, session, input)
            }
        }
    }

    fun close() {
        tfliteModel?.close()
        tfliteModel = null
        onnxSession?.close()
        onnxSession = null
        onnxEnv?.close()
        onnxEnv = null
    }

    private fun runTflite(interpreter: Interpreter, input: ModelInput): Float? {
        return try {
            val ids = arrayOf(input.inputIds)
            val mask = arrayOf(input.attentionMask)
            val output = Array(1) { FloatArray(1) }
            interpreter.runForMultipleInputsOutputs(arrayOf(ids, mask), mapOf(0 to output))
            output[0][0].coerceIn(0f, 1f)
        } catch (e: Exception) {
            Log.w(TAG, "TFLite inference failed", e)
            null
        }
    }

    private fun runOnnx(env: OrtEnvironment, session: OrtSession, input: ModelInput): Float? {
        return try {
            val idsBuffer = toLongBuffer(input.inputIds)
            val maskBuffer = toLongBuffer(input.attentionMask)
            val ids = OnnxTensor.createTensor(env, idsBuffer, longArrayOf(1, input.inputIds.size.toLong()))
            val mask = OnnxTensor.createTensor(env, maskBuffer, longArrayOf(1, input.attentionMask.size.toLong()))
            val inputs = mutableMapOf<String, OnnxTensor>()
            session.inputNames.forEach { name ->
                when (name) {
                    "input_ids", "inputIds" -> inputs[name] = ids
                    "attention_mask", "attentionMask" -> inputs[name] = mask
                }
            }
            if (inputs.isEmpty()) {
                ids.close()
                mask.close()
                return null
            }
            session.run(inputs).use { result ->
                val first = result[0].value as? Array<FloatArray>
                val value = first?.getOrNull(0)?.getOrNull(0)
                value?.coerceIn(0f, 1f)
            }
        } catch (e: Exception) {
            Log.w(TAG, "ONNX inference failed", e)
            null
        }
    }

    private fun loadAssetBuffer(assetPath: String): ByteBuffer {
        val bytes = loadAssetBytes(assetPath)
        val buffer = ByteBuffer.allocateDirect(bytes.size)
        buffer.order(ByteOrder.nativeOrder())
        buffer.put(bytes)
        buffer.rewind()
        return buffer
    }

    private fun loadAssetBytes(assetPath: String): ByteArray {
        return context.assets.open(assetPath).use { it.readBytes() }
    }

    private fun toLongBuffer(values: IntArray): LongBuffer {
        val longs = LongArray(values.size) { index -> values[index].toLong() }
        return LongBuffer.wrap(longs)
    }

    companion object {
        private const val TAG = "RakshakX-Model"
        private const val DEFAULT_TFLITE_MODEL = "models/fraud_model.tflite"
        private const val DEFAULT_ONNX_MODEL = "models/fraud_model.onnx"
    }
}
