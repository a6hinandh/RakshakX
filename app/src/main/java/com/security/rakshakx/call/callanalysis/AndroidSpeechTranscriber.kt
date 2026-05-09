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

/**
 * AndroidSpeechTranscriber
 *
 * Fallback transcription using the platform SpeechRecognizer.
 * This listens to the microphone directly and returns the best result.
 */
class AndroidSpeechTranscriber(private val context: Context) {

    companion object {
        private const val TAG = "AndroidSpeechTranscriber"
    }

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    suspend fun transcribeOnce(timeoutMs: Long = 5000L): String = withContext(Dispatchers.Main) {
        if (!isAvailable()) {
            Log.w(TAG, "Speech recognizer not available on this device")
            return@withContext ""
        }

        suspendCancellableCoroutine { cont ->
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val handler = Handler(Looper.getMainLooper())
            var finished = false

            fun finish(result: String) {
                if (finished) return
                finished = true
                handler.removeCallbacksAndMessages(null)
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
                }

                override fun onBeginningOfSpeech() {
                }

                override fun onRmsChanged(rmsdB: Float) {
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                }

                override fun onEndOfSpeech() {
                }

                override fun onError(error: Int) {
                    Log.w(TAG, "Speech recognizer error: $error")
                    finish("")
                }

                override fun onResults(results: Bundle) {
                    val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    finish(matches?.firstOrNull().orEmpty())
                }

                override fun onPartialResults(partialResults: Bundle) {
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                }
            }

            recognizer.setRecognitionListener(listener)

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            recognizer.startListening(intent)

            handler.postDelayed({
                finish("")
            }, timeoutMs)

            cont.invokeOnCancellation {
                finish("")
            }
        }
    }
}
