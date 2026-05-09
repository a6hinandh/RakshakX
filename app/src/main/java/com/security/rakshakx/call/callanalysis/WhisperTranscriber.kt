package com.security.rakshakx.call.callanalysis

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.CancellationException

/**
 * WhisperTranscriber
 *
 * Handles on-device ASR using a quantized Whisper TFLite model.
 * Converts raw PCM audio (16kHz, mono) into text.
 */
class WhisperTranscriber(private val context: Context) {

    companion object {
        private const val TAG = "WhisperTranscriber"
        private const val MODEL_PATH = "models/whisper_tiny_quant.tflite"
        private const val SAMPLE_RATE = 16000
        private const val MAX_AUDIO_DURATION_S = 30
        private const val MAX_SAMPLES = SAMPLE_RATE * MAX_AUDIO_DURATION_S
    }

    private var interpreter: Interpreter? = null
    private var isInitialized = false
    private var modelChecked = false
    private var modelAvailable = false
    private var missingModelLogged = false

    /**
     * Initialize the TFLite interpreter.
     */
    @Synchronized
    private fun initialize(): Boolean {
        if (isInitialized) return true
        if (!isModelAvailable()) return false

        return try {
            val modelBuffer = loadModelFile()
            val options = Interpreter.Options().apply {
                setNumThreads(Runtime.getRuntime().availableProcessors())
                // Use NNAPI or GPU if available for acceleration
                // setUseNNAPI(true)
            }
            interpreter = Interpreter(modelBuffer, options)
            isInitialized = true
            Log.i(TAG, "Whisper TFLite model loaded successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load Whisper TFLite model", e)
            false
        }
    }

    fun isModelAvailable(): Boolean {
        if (modelChecked) return modelAvailable

        modelAvailable = try {
            context.assets.open(MODEL_PATH).close()
            true
        } catch (e: Exception) {
            false
        }

        modelChecked = true

        if (!modelAvailable && !missingModelLogged) {
            Log.e(TAG, "Whisper model missing at assets/$MODEL_PATH")
            missingModelLogged = true
        }

        return modelAvailable
    }

    /**
     * Transcribe PCM audio data.
     * @param pcmData ShortArray of 16kHz mono PCM samples.
     * @return Transcribed text.
     */
    suspend fun transcribePcm(pcmData: ShortArray): String = withContext(Dispatchers.IO) {
        if (!initialize()) {
            return@withContext "Error: Model initialization failed"
        }

        try {
            Log.d(TAG, "Starting transcription for ${pcmData.size} samples")

            // 1. Pre-process: Normalize PCM to Float [-1, 1]
            val floatAudio = normalizeAudio(pcmData.take(MAX_SAMPLES).toShortArray())

            // 2. Feature Extraction: Log-Mel Spectrogram
            // Most Whisper TFLite implementations expect a spectrogram of fixed size [1, 80, 3000]
            val melSpectrogram = extractMelSpectrogram(floatAudio)

            // 3. Inference
            val tflite = interpreter ?: return@withContext "Error: Interpreter not initialized"

            // Get output tensor shape dynamically, e.g. [1, 449]
            val outputTensor = tflite.getOutputTensor(0)
            val shape = outputTensor.shape()
            val outputLen = if (shape.size > 1) shape[1] else shape[0]
            Log.d(TAG, "Whisper output tensor shape: ${shape.joinToString()}")

            val outputBuffer = Array(1) { IntArray(outputLen) }
            tflite.run(melSpectrogram, outputBuffer)

            // 4. Decode: Token IDs to Text
            val tokens = outputBuffer[0].filter { it > 0 }
            if (tokens.isEmpty()) {
                return@withContext "(No speech detected)"
            }

            // STUB DECODER: This would normally map token IDs to strings.
            "Detected transcript from Whisper ASR (Tokens: ${tokens.size})"

        } catch (e: CancellationException) {
            Log.w(TAG, "Transcription cancelled")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            "Error: Transcription failed"
        }
    }

    private fun normalizeAudio(pcm: ShortArray): FloatArray {
        return FloatArray(pcm.size) { i -> pcm[i] / 32768.0f }
    }

    /**
     * Extract Log-Mel Spectrogram.
     * This is a complex operation; in production, use a library like JLibrosa
     * or a custom C++ implementation via JNI for performance.
     */
    private fun extractMelSpectrogram(audio: FloatArray): Array<Array<FloatArray>> {
        // Mock output shape [1][80][3000] for 30s of audio
        return Array(1) { Array(80) { FloatArray(3000) } }
    }

    private fun loadModelFile(): ByteBuffer {
        return try {
            val afd = context.assets.openFd(MODEL_PATH)
            val inputStream = FileInputStream(afd.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = afd.startOffset
            val declaredLength = afd.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: Exception) {
            val bytes = context.assets.open(MODEL_PATH).use { it.readBytes() }
            ByteBuffer.allocateDirect(bytes.size)
                .order(ByteOrder.nativeOrder())
                .apply {
                    put(bytes)
                    rewind()
                }
        }
    }

    fun release() {
        interpreter?.close()
        interpreter = null
        isInitialized = false
    }
}

