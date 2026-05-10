package com.security.rakshakx.call.callanalysis

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
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

        } catch (e: Exception) {

            Log.e(
                "RAKSHAK_DEBUG",
                "Vosk init failed",
                e
            )

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

            false
        }
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