package com.security.rakshakx.call.callanalysis

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Real audio recording utility for call analysis.
 * Supports both MediaRecorder (M4A) and AudioRecord (PCM).
 */
class CallAudioRecorder(private val context: Context? = null) {

    private var mediaRecorder: MediaRecorder? = null
    private var audioRecord: AudioRecord? = null
    private var outputFile: File? = null
    private var isRecording = false
    private var isPcmMode = false

    companion object {
        private const val TAG = "CallAudioRecorder"
        private const val AUDIO_DIR = "call_audio"
        private const val SAMPLE_RATE = 16000
    }

    /**
     * Starts recording audio in raw PCM format (16kHz, mono, 16-bit).
     * Useful for real-time analysis like Whisper ASR.
     */
    @SuppressLint("MissingPermission")
    fun startPcmRecording(): String? {
        return try {
            val audioDir = getAudioDir()
            outputFile = File(audioDir, "call_${System.currentTimeMillis()}.pcm")

            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                throw Exception("AudioRecord initialization failed")
            }

            audioRecord?.startRecording()
            isRecording = true
            isPcmMode = true

            // Write to file in a background thread
            val fos = FileOutputStream(outputFile)
            Thread {
                try {
                    val data = ShortArray(bufferSize)
                    while (isRecording) {
                        val read = audioRecord?.read(data, 0, bufferSize) ?: 0
                        if (read <= 0) {
                            Log.e(TAG, "AudioRecord read failed with code $read, stopping recording loop")
                            break
                        }
                        val byteData = pcmToByteArray(data, read)
                        fos.write(byteData)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in PCM recording thread", e)
                } finally {
                    try {
                        fos.close()
                    } catch (_: Exception) {
                    }
                }
            }.start()

            Log.d(TAG, "PCM Recording started: ${outputFile!!.absolutePath}")
            outputFile!!.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start PCM recording", e)
            isRecording = false
            null
        }
    }

    /**
     * Reads PCM data from the recorded file as ShortArray.
     */
    fun getPcmData(): ShortArray? {
        val file = outputFile ?: return null
        if (!file.exists()) return null

        return try {
            val bytes = file.readBytes()
            val shorts = ShortArray(bytes.size / 2)
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
            shorts
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read PCM data", e)
            null
        }
    }

    private fun pcmToByteArray(samples: ShortArray, size: Int): ByteArray {
        val bytes = ByteArray(size * 2)
        for (i in 0 until size) {
            val s = samples[i].toInt()
            bytes[i * 2] = (s and 0x00FF).toByte()
            bytes[i * 2 + 1] = (s and 0xFF00 shr 8).toByte()
        }
        return bytes
    }

    private fun getAudioDir(): File {
        return if (context != null) {
            File(context.getExternalFilesDir(null), AUDIO_DIR).apply {
                if (!exists()) mkdirs()
            }
        } else {
            File(context.filesDir, "call_audio").apply {
                if (!exists()) mkdirs()
            }
        }
    }

    /**
     * Starts recording audio from microphone.
     * @return File path of recorded audio, or null if failed.
     */
    fun startRecording(): String? {
        return try {
            val audioDir = getAudioDir()
            outputFile = File(audioDir, "call_${System.currentTimeMillis()}.m4a")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context ?: throw Exception("Context required"))
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(SAMPLE_RATE)
                setAudioChannels(1)
                setAudioEncodingBitRate(64000)
                setOutputFile(outputFile!!.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            isPcmMode = false
            Log.d(TAG, "M4A Recording started: ${outputFile!!.absolutePath}")
            outputFile!!.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            isRecording = false
            null
        }
    }

    /**
     * Stops recording and finishes.
     */
    fun stopRecording(): String? {
        if (!isRecording) return null

        return try {
            if (isPcmMode) {
                isRecording = false
                try {
                    audioRecord?.stop()
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping AudioRecord", e)
                }
                audioRecord?.release()
                audioRecord = null
            } else {
                mediaRecorder?.apply {
                    try { stop() } catch (e: Exception) {}
                    release()
                }
                mediaRecorder = null
                isRecording = false
            }

            val path = outputFile?.absolutePath
            Log.d(TAG, "Recording stopped: $path")
            path
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            isRecording = false
            null
        }
    }

    fun isRecording(): Boolean = isRecording

    fun release() {
        stopRecording()
    }
}

