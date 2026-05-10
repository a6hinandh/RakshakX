package com.security.rakshakx.call.callanalysis

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class AndroidSpeechTranscriber(
    private val context: Context
) {

    companion object {
        private const val TAG = "AndroidSpeechTranscriber"
    }

    fun isAvailable(): Boolean {

        val available =
            SpeechRecognizer.isRecognitionAvailable(context)

        Log.d(
            "RAKSHAK_DEBUG",
            "SpeechRecognizer available = $available"
        )

        return available
    }

    suspend fun transcribeOnce(
        timeoutMs: Long = 8000L
    ): String = withContext(Dispatchers.Main) {

        if (!isAvailable()) {

            Log.w(
                "RAKSHAK_DEBUG",
                "Speech recognizer unavailable"
            )

            return@withContext ""
        }

        suspendCancellableCoroutine { cont ->

            Log.d(
                "RAKSHAK_DEBUG",
                "Creating SpeechRecognizer"
            )

            val recognizer =
                SpeechRecognizer.createSpeechRecognizer(context)

            val handler =
                Handler(Looper.getMainLooper())

            var finished = false

            fun finish(result: String) {

                if (finished) return

                finished = true

                Log.d(
                    "RAKSHAK_DEBUG",
                    "SpeechRecognizer finish = '$result'"
                )

                handler.removeCallbacksAndMessages(null)

                try {
                    recognizer.stopListening()
                } catch (_: Exception) {
                }

                try {
                    recognizer.cancel()
                } catch (_: Exception) {
                }

                try {
                    recognizer.destroy()
                } catch (_: Exception) {
                }

                if (cont.isActive) {
                    cont.resume(result)
                }
            }

            val listener = object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {

                    Log.d(
                        "RAKSHAK_DEBUG",
                        "onReadyForSpeech"
                    )
                }

                override fun onBeginningOfSpeech() {

                    Log.d(
                        "RAKSHAK_DEBUG",
                        "onBeginningOfSpeech"
                    )
                }

                override fun onRmsChanged(rmsdB: Float) {

                    Log.d(
                        "RAKSHAK_DEBUG",
                        "RMS = $rmsdB"
                    )
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                }

                override fun onEndOfSpeech() {

                    Log.d(
                        "RAKSHAK_DEBUG",
                        "onEndOfSpeech"
                    )
                }

                override fun onError(error: Int) {

                    Log.e(
                        "RAKSHAK_DEBUG",
                        "SpeechRecognizer ERROR = $error"
                    )

                    finish("")
                }

                override fun onResults(results: Bundle) {

                    val matches =
                        results.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    Log.d(
                        "RAKSHAK_DEBUG",
                        "FINAL RESULTS = $matches"
                    )

                    finish(
                        matches?.firstOrNull().orEmpty()
                    )
                }

                override fun onPartialResults(
                    partialResults: Bundle
                ) {

                    val matches =
                        partialResults.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    Log.d(
                        "RAKSHAK_DEBUG",
                        "PARTIAL RESULTS = $matches"
                    )
                }

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {
                }
            }

            recognizer.setRecognitionListener(listener)

            val intent =
                Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                ).apply {

                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )

                    putExtra(
                        RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                        true
                    )

                    putExtra(
                        RecognizerIntent.EXTRA_MAX_RESULTS,
                        3
                    )

                    putExtra(
                        RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                        3000L
                    )

                    putExtra(
                        RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                        3000L
                    )

                    putExtra(
                        RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                        5000L
                    )

                    putExtra(
                        RecognizerIntent.EXTRA_CALLING_PACKAGE,
                        context.packageName
                    )

                    // IMPORTANT:
                    // Removed forced offline mode
                }

            Log.d(
                "RAKSHAK_DEBUG",
                "Starting SpeechRecognizer listening"
            )

            recognizer.startListening(intent)

            handler.postDelayed({

                Log.w(
                    "RAKSHAK_DEBUG",
                    "SpeechRecognizer TIMEOUT"
                )

                finish("")

            }, timeoutMs)

            cont.invokeOnCancellation {

                Log.w(
                    "RAKSHAK_DEBUG",
                    "SpeechRecognizer CANCELLED"
                )

                finish("")
            }
        }
    }
}