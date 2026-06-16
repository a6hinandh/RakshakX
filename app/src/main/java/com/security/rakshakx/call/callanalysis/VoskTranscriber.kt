package com.security.rakshakx.call.callanalysis

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class VoskTranscriber(
    private val context: Context
) {

    companion object {

        private const val MODEL_DIR =
            "model-en-us"
    }

    // ==========================================
    // VOSK COMPONENTS
    // ==========================================
    private var model: Model? = null

    private var recognizer: Recognizer? = null

    // ==========================================
    // INITIALIZATION
    // ==========================================
    @Synchronized
    fun initialize(): Boolean {

        if (recognizer != null) {

            return true
        }

        return try {

            Log.d(
                "RAKSHAK_DEBUG",
                "Initializing Vosk..."
            )

            val modelPath =
                copyModelToInternalStorage(
                    MODEL_DIR
                )

            Log.d(
                "RAKSHAK_DEBUG",
                "Model path = $modelPath"
            )

            val m =
                Model(modelPath)

            model = m

            recognizer =
                Recognizer(
                    m,
                    16000.0f
                ).apply {

                    setWords(true)
                }

            Log.d(
                "RAKSHAK_DEBUG",
                "Vosk initialized successfully"
            )

            true

        } catch (e: UnsatisfiedLinkError) {

            Log.e(
                "RAKSHAK_DEBUG",
                "UnsatisfiedLinkError: Native JNA library not found. Ensure jna-android dependency is properly configured.",
                e
            )

            cleanup()

            false

        } catch (e: Exception) {

            Log.e(
                "RAKSHAK_DEBUG",
                "Vosk init failed",
                e
            )

            cleanup()

            false
        }
    }

    // ==========================================
    // CLEANUP HELPER
    // ==========================================
    private fun cleanup() {

        try {

            recognizer?.close()
        } catch (_: Exception) {
        }

        try {

            model?.close()
        } catch (_: Exception) {
        }

        recognizer = null

        model = null
    }

    // ==========================================
    // CHECK READY
    // ==========================================
    fun isReady(): Boolean {

        return recognizer != null
    }

    // ==========================================
    // PROCESS AUDIO
    // ==========================================
    fun processAudio(
        audioData: ByteArray,
        length: Int
    ): String {

        return try {

            val rec =
                recognizer ?: return ""

            val isFinal =
                rec.acceptWaveForm(
                    audioData,
                    length
                )

            if (isFinal) {

                val result =
                    rec.result

                val text =
                    JSONObject(result)
                        .optString(
                            "text",
                            ""
                        )

                Log.d(
                    "RAKSHAK_DEBUG",
                    "Vosk FINAL = $text"
                )

                text

            } else {

                val partial =
                    rec.partialResult

                val text =
                    JSONObject(partial)
                        .optString(
                            "partial",
                            ""
                        )

                Log.d(
                    "RAKSHAK_DEBUG",
                    "Vosk PARTIAL = $text"
                )

                text
            }

        } catch (e: Exception) {

            Log.e(
                "RAKSHAK_DEBUG",
                "Audio processing failed",
                e
            )

            ""
        }
    }

    fun isModelAvailable(): Boolean {
        return try {
            val assets = context.assets.list(MODEL_DIR)
            !assets.isNullOrEmpty()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun transcribe(audioPath: String): String = withContext(Dispatchers.IO) {
        try {
            val audioFile = File(audioPath)
            if (!audioFile.exists()) {
                Log.e("RAKSHAK_DEBUG", "Audio file not found: $audioPath")
                return@withContext ""
            }

            if (!initialize()) {
                Log.e("RAKSHAK_DEBUG", "Failed to initialize Vosk for file transcription")
                return@withContext ""
            }

            val tempRecognizer = Recognizer(model!!, 16000.0f).apply { setWords(true) }
            val buffer = ByteArray(4096)
            val fullText = StringBuilder()

            FileInputStream(audioFile).use { fis ->
                // Skip WAV header if present
                if (audioPath.endsWith(".wav", ignoreCase = true)) {
                    fis.skip(44)
                }
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    if (tempRecognizer.acceptWaveForm(buffer, bytesRead)) {
                        val result = JSONObject(tempRecognizer.result).optString("text", "")
                        if (result.isNotBlank()) {
                            if (fullText.isNotEmpty()) fullText.append(" ")
                            fullText.append(result)
                        }
                    }
                }
            }

            val finalResult = JSONObject(tempRecognizer.finalResult).optString("text", "")
            if (finalResult.isNotBlank()) {
                if (fullText.isNotEmpty()) fullText.append(" ")
                fullText.append(finalResult)
            }

            tempRecognizer.close()
            Log.d("RAKSHAK_DEBUG", "File transcription complete: ${fullText.length} chars")
            fullText.toString()
        } catch (e: Exception) {
            Log.e("RAKSHAK_DEBUG", "File transcription failed", e)
            ""
        }
    }

    // ==========================================
    // RELEASE
    // ==========================================
    @Synchronized
    fun release() {

        try {

            try {

                recognizer?.close()
            } catch (e: Exception) {

                Log.e(
                    "RAKSHAK_DEBUG",
                    "Recognizer close failed",
                    e
                )
            }

            recognizer = null

            try {

                model?.close()
            } catch (e: Exception) {

                Log.e(
                    "RAKSHAK_DEBUG",
                    "Model close failed",
                    e
                )
            }

            model = null

            Log.d(
                "RAKSHAK_DEBUG",
                "Vosk released"
            )
        } catch (e: Exception) {

            Log.e(
                "RAKSHAK_DEBUG",
                "Release failed",
                e
            )
        }
    }

    // ==========================================
    // COPY MODEL
    // ==========================================
    private fun copyModelToInternalStorage(
        assetFolder: String
    ): String {

        val outDir =
            File(
                context.filesDir,
                assetFolder
            )

        if (outDir.exists()) {

            Log.d(
                "RAKSHAK_DEBUG",
                "Model already copied"
            )

            return outDir.absolutePath
        }

        outDir.mkdirs()

        copyAssetsRecursively(
            assetFolder,
            outDir
        )

        Log.d(
            "RAKSHAK_DEBUG",
            "Model copied successfully"
        )

        return outDir.absolutePath
    }

    // ==========================================
    // RECURSIVE COPY
    // ==========================================
    private fun copyAssetsRecursively(
        assetPath: String,
        outDir: File
    ) {

        val assets =
            context.assets.list(assetPath)
                ?: return

        for (asset in assets) {

            val fullPath =
                "$assetPath/$asset"

            val subAssets =
                context.assets.list(fullPath)

            if (subAssets.isNullOrEmpty()) {

                val outFile =
                    File(outDir, asset)

                context.assets.open(fullPath)
                    .use { input ->

                        FileOutputStream(outFile)
                            .use { output ->

                                input.copyTo(output)
                            }
                    }

            } else {

                val subDir =
                    File(outDir, asset)

                subDir.mkdirs()

                copyAssetsRecursively(
                    fullPath,
                    subDir
                )
            }
        }
    }
}