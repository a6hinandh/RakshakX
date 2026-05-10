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
class CallAudioRecorder(
    private val context: Context? = null
) {

    // ==========================================
    // LIVE AUDIO STREAM CALLBACK
    // ==========================================
    interface AudioChunkListener {

        fun onAudioChunk(
            data: ByteArray,
            length: Int
        )
    }

    // ==========================================
    // Variables
    // ==========================================
    private var mediaRecorder: MediaRecorder? = null

    private var audioRecord: AudioRecord? = null

    private var outputFile: File? = null

    private var isRecording = false

    private var isPcmMode = false

    private var audioChunkListener: AudioChunkListener? = null

    companion object {

        private const val TAG = "CallAudioRecorder"

        private const val AUDIO_DIR = "call_audio"

        // REQUIRED FOR VOSK
        private const val SAMPLE_RATE = 16000
    }

    // ==========================================
    // SET LIVE AUDIO LISTENER
    // ==========================================
    fun setAudioChunkListener(
        listener: AudioChunkListener
    ) {

        audioChunkListener = listener
    }

    // ==========================================
    // START PCM RECORDING
    // ==========================================
    @SuppressLint("MissingPermission")
    fun startPcmRecording(): String? {

        return try {

            val audioDir = getAudioDir()

            outputFile =
                File(
                    audioDir,
                    "call_${System.currentTimeMillis()}.pcm"
                )

            val bufferSizeBytes =
                AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

            val bufferSizeShorts =
                (bufferSizeBytes / 2)
                    .coerceAtLeast(1)

            audioRecord =
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSizeBytes
                )

            if (
                audioRecord?.state !=
                AudioRecord.STATE_INITIALIZED
            ) {

                throw Exception(
                    "AudioRecord initialization failed"
                )
            }

            audioRecord?.startRecording()

            isRecording = true

            isPcmMode = true

            Log.d(
                "RAKSHAK_DEBUG",
                "PCM recording started"
            )

            val fos =
                FileOutputStream(outputFile)

            Thread {

                try {

                    val data =
                        ShortArray(bufferSizeShorts)

                    var emptyReads = 0

                    while (isRecording) {

                        val read =
                            audioRecord?.read(
                                data,
                                0,
                                bufferSizeShorts
                            ) ?: 0

                        if (read == 0) {

                            emptyReads++

                            if (emptyReads >= 50) {

                                Log.w(
                                    TAG,
                                    "AudioRecord returned 0 too long"
                                )

                                break
                            }

                            Thread.sleep(10)

                            continue
                        }

                        if (read < 0) {

                            Log.e(
                                TAG,
                                "AudioRecord failed: $read"
                            )

                            break
                        }

                        emptyReads = 0

                        val byteData =
                            pcmToByteArray(
                                data,
                                read
                            )

                        // ==================================
                        // SAVE TO FILE
                        // ==================================
                        fos.write(byteData)

                        // ==================================
                        // LIVE STREAM TO VOSK
                        // Must run on this thread only: Vosk Recognizer is not thread-safe.
                        // Spawning a Thread per chunk caused native crashes / OOM.
                        // ==================================
                        try {

                            audioChunkListener?.onAudioChunk(
                                byteData,
                                byteData.size
                            )
                        } catch (e: Exception) {

                            Log.e(
                                TAG,
                                "Audio chunk callback failed",
                                e
                            )
                        }
                    }

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "PCM recording thread failed",
                        e
                    )

                } finally {

                    try {

                        fos.close()

                    } catch (_: Exception) {
                    }
                }

            }.start()

            Log.d(
                TAG,
                "PCM Recording started: ${outputFile!!.absolutePath}"
            )

            outputFile!!.absolutePath

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to start PCM recording",
                e
            )

            isRecording = false

            null
        }
    }

    // ==========================================
    // GET PCM DATA
    // ==========================================
    fun getPcmData(): ShortArray? {

        val file = outputFile ?: return null

        if (!file.exists()) return null

        return try {

            val bytes =
                file.readBytes()

            val shorts =
                ShortArray(bytes.size / 2)

            ByteBuffer.wrap(bytes)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer()
                .get(shorts)

            shorts

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to read PCM data",
                e
            )

            null
        }
    }

    // ==========================================
    // PCM TO BYTE ARRAY
    // ==========================================
    private fun pcmToByteArray(
        samples: ShortArray,
        size: Int
    ): ByteArray {

        val bytes =
            ByteArray(size * 2)

        for (i in 0 until size) {

            val s =
                samples[i].toInt()

            bytes[i * 2] =
                (s and 0x00FF).toByte()

            bytes[i * 2 + 1] =
                ((s and 0xFF00) shr 8).toByte()
        }

        return bytes
    }

    // ==========================================
    // AUDIO DIRECTORY
    // ==========================================
    private fun getAudioDir(): File {

        val ctx =
            context
                ?: throw IllegalStateException(
                    "Context required"
                )

        val baseDir =
            ctx.getExternalFilesDir(null)
                ?: ctx.filesDir

        return File(baseDir, AUDIO_DIR).apply {

            if (!exists()) {

                mkdirs()
            }
        }
    }

    // ==========================================
    // START NORMAL RECORDING
    // ==========================================
    fun startRecording(): String? {

        return try {

            val audioDir =
                getAudioDir()

            outputFile =
                File(
                    audioDir,
                    "call_${System.currentTimeMillis()}.m4a"
                )

            mediaRecorder =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                    MediaRecorder(
                        context
                            ?: throw Exception(
                                "Context required"
                            )
                    )

                } else {

                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }

            mediaRecorder?.apply {

                setAudioSource(
                    MediaRecorder.AudioSource.MIC
                )

                setOutputFormat(
                    MediaRecorder.OutputFormat.MPEG_4
                )

                setAudioEncoder(
                    MediaRecorder.AudioEncoder.AAC
                )

                setAudioSamplingRate(
                    SAMPLE_RATE
                )

                setAudioChannels(1)

                setAudioEncodingBitRate(
                    64000
                )

                setOutputFile(
                    outputFile!!.absolutePath
                )

                prepare()

                start()
            }

            isRecording = true

            isPcmMode = false

            Log.d(
                TAG,
                "M4A recording started"
            )

            outputFile!!.absolutePath

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to start recording",
                e
            )

            isRecording = false

            null
        }
    }

    // ==========================================
    // STOP RECORDING
    // ==========================================
    fun stopRecording(): String? {

        if (!isRecording) {

            return null
        }

        return try {

            if (isPcmMode) {

                isRecording = false

                try {

                    audioRecord?.stop()

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "Error stopping AudioRecord",
                        e
                    )
                }

                audioRecord?.release()

                audioRecord = null

            } else {

                mediaRecorder?.apply {

                    try {

                        stop()

                    } catch (_: Exception) {
                    }

                    release()
                }

                mediaRecorder = null

                isRecording = false
            }

            val path =
                outputFile?.absolutePath

            Log.d(
                TAG,
                "Recording stopped: $path"
            )

            path

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to stop recording",
                e
            )

            isRecording = false

            null
        }
    }

    // ==========================================
    // STATE
    // ==========================================
    fun isRecording(): Boolean = isRecording

    // ==========================================
    // RELEASE
    // ==========================================
    fun release() {

        stopRecording()
    }
}