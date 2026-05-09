package com.security.rakshakx.call.callanalysis

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlin.math.abs

/**
 * CallRecordingProbe
 * 
 * Heuristic detector to verify if the OS/OEM allows capturing audio
 * from the voice call or microphone during an active call.
 */
class CallRecordingProbe {

    companion object {
        private const val TAG = "CallRecordingProbe"
    }

    data class ProbeResult(
        val success: Boolean,
        val hasSignal: Boolean,
        val error: String? = null
    )

    /**
     * Run a short audio capture probe.
     * Note: Requires RECORD_AUDIO permission.
     */
    @SuppressLint("MissingPermission")
    fun runProbe(): ProbeResult {
        val sampleRate = 8000
        val audioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION // Alternative to VOICE_CALL for wider compatibility
        
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (bufferSize <= 0) {
            return ProbeResult(success = false, hasSignal = false, error = "Invalid buffer size")
        }

        var audioRecord: AudioRecord? = null
        return try {
            audioRecord = AudioRecord(
                audioSource,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                return ProbeResult(success = false, hasSignal = false, error = "AudioRecord initialization failed")
            }

            val buffer = ShortArray(bufferSize)
            audioRecord.startRecording()

            // Capture 500ms of audio
            val read = audioRecord.read(buffer, 0, buffer.size)
            audioRecord.stop()

            if (read > 0) {
                // Heuristic: check for non-zero signal (amplitude)
                var sumAbs = 0.0
                for (i in 0 until read) {
                    sumAbs += abs(buffer[i].toInt())
                }
                val avgAbs = sumAbs / read
                Log.d(TAG, "Probe average amplitude: $avgAbs")

                // If avg amplitude is extremely low (e.g., < 10), it's likely silent/blocked
                val hasSignal = avgAbs > 10.0 
                ProbeResult(success = true, hasSignal = hasSignal)
            } else {
                ProbeResult(success = false, hasSignal = false, error = "No data read")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Probe failed", e)
            ProbeResult(success = false, hasSignal = false, error = e.message)
        } finally {
            audioRecord?.release()
        }
    }
}


