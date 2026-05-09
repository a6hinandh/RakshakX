package com.rakshakx.ai.loader

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.MappedByteBuffer

data class SmsClassifierHandle(
    val interpreter: Interpreter,
    val inputVectorSize: Int,
    val outputVectorSize: Int
)

class ModelLoader(
    private val context: Context
) {
    fun loadInterpreter(modelFile: String): Interpreter {
        val modelBuffer = loadModelFile(modelFile)
        return Interpreter(modelBuffer)
    }

    private fun loadModelFile(modelFile: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFile)
        fileDescriptor.use { fd ->
            val inputStream = fd.createInputStream()
            inputStream.channel.use { channel ->
                return channel.map(
                    java.nio.channels.FileChannel.MapMode.READ_ONLY,
                    fd.startOffset,
                    fd.declaredLength
                )
            }
        }
    }

    fun loadSmsClassifier(): SmsClassifierHandle {
        // Placeholder model path. Replace with real quantized IndicBERT-compatible TFLite artifact later.
        val interpreter = loadInterpreter("models/sms_classifier.tflite")
        return SmsClassifierHandle(
            interpreter = interpreter,
            inputVectorSize = 128,
            outputVectorSize = 1
        )
    }
}
