package com.security.rakshakx.call.callanalysis

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.security.rakshakx.R
import com.security.rakshakx.call.callanalysis.ml.DummyFraudTextModel
import com.security.rakshakx.call.callanalysis.ml.FraudTextModel
import kotlinx.coroutines.*

class CallRecordingService : Service() {

    companion object {

        private const val CHANNEL_ID =
            "rakshakx_recording"

        private const val NOTIFICATION_ID =
            2001

        const val ACTION_START_RECORDING =
            "START_RECORDING"

        const val ACTION_STOP_RECORDING =
            "STOP_RECORDING"

        const val ACTION_DEV_TEST =
            "DEV_TEST"

        const val EXTRA_PHONE_NUMBER =
            "phone_number"

        fun startRecording(
            context: Context,
            phoneNumber: String
        ) {

            val intent =
                Intent(
                    context,
                    CallRecordingService::class.java
                ).apply {

                    action = ACTION_START_RECORDING

                    putExtra(
                        EXTRA_PHONE_NUMBER,
                        phoneNumber
                    )
                }

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                context.startForegroundService(
                    intent
                )

            } else {

                context.startService(intent)
            }
        }

        fun stopRecording(
            context: Context
        ) {

            val intent =
                Intent(
                    context,
                    CallRecordingService::class.java
                ).apply {

                    action = ACTION_STOP_RECORDING
                }

            context.startService(intent)
        }
    }

    // ==========================================
    // Scope
    // ==========================================
    private val scope =
        CoroutineScope(
            Dispatchers.Main +
                    SupervisorJob()
        )

    // ==========================================
    // Components
    // ==========================================
    private lateinit var recorder:
            CallAudioRecorder

    private lateinit var classifier:
            FraudIntentClassifier

    private lateinit var mlClassifier:
            FraudMLClassifier

    private lateinit var engine:
            PreActionDecisionEngine

    // ==========================================
    // ML Model
    // ==========================================
    private val fraudTextModel:
            FraudTextModel by lazy {

        DummyFraudTextModel()
    }

    // ==========================================
    // CREATE
    // ==========================================
    override fun onCreate() {

        super.onCreate()

        Log.d(
            "RAKSHAK_DEBUG",
            "CallRecordingService created"
        )

        createNotificationChannel()

        recorder =
            CallAudioRecorder(this)

        Log.d(
            "RAKSHAK_DEBUG",
            "CallAudioRecorder initialized"
        )

        classifier =
            FraudIntentClassifier()

        mlClassifier =
            FraudMLClassifier(
                fraudTextModel
            )

        engine =
            PreActionDecisionEngine()

        Log.d(
            "RAKSHAK_DEBUG",
            "Fraud pipeline initialized"
        )
    }

    // ==========================================
    // START COMMAND
    // ==========================================
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        Log.d(
            "RAKSHAK_DEBUG",
            "onStartCommand = ${intent?.action}"
        )

        when (intent?.action) {

            ACTION_START_RECORDING -> {

                val phoneNumber =
                    intent.getStringExtra(
                        EXTRA_PHONE_NUMBER
                    ) ?: "Unknown"

                val notification =
                    createNotification(
                        phoneNumber,
                        "Listening..."
                    )

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q
                ) {

                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo
                            .FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )

                } else {

                    startForeground(
                        NOTIFICATION_ID,
                        notification
                    )
                }

                startRealtimeFraudDetection(
                    phoneNumber
                )
            }

            ACTION_STOP_RECORDING -> {

                stopRealtimeDetection()
            }

            ACTION_DEV_TEST -> {

                runDevFraudTest()

                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    // ==========================================
    // REALTIME DETECTION
    // ==========================================
    private fun startRealtimeFraudDetection(
        phoneNumber: String
    ) {

        scope.launch(Dispatchers.IO) {

            try {

                Log.d(
                    "RAKSHAK_DEBUG",
                    "Starting realtime fraud detection"
                )

                if (
                    ContextCompat.checkSelfPermission(
                        this@CallRecordingService,
                        Manifest.permission.RECORD_AUDIO
                    ) !=
                    PackageManager.PERMISSION_GRANTED
                ) {

                    Log.e(
                        "RAKSHAK_DEBUG",
                        "RECORD_AUDIO denied"
                    )

                    stopSelf()

                    return@launch
                }

                Log.d(
                    "RAKSHAK_DEBUG",
                    "RECORD_AUDIO granted"
                )



                broadcastCaptureStatus(
                    "Capture: Listening..."
                )

                broadcastCaptureSource(
                    "Source: Speakerphone + Mic"
                )

                updateNotification(
                    phoneNumber,
                    "Live AI scam detection active"
                )

                // ==================================
                // LIVE AUDIO STREAM
                // ==================================
                recorder.setAudioChunkListener(

                    object :
                        CallAudioRecorder
                        .AudioChunkListener {

                        override fun onAudioChunk(
                            data: ByteArray,
                            length: Int
                        ) {

                            // Audio chunk callback (reserved for future ASR routing)
                            // Currently handled in main loop via getPcmData()
                        }
                    }
                )

                val pcmPath =
                    recorder.startPcmRecording()

                Log.d(
                    "RAKSHAK_DEBUG",
                    "PCM path = $pcmPath"
                )

                if (pcmPath == null) {

                    Log.e(
                        "RAKSHAK_DEBUG",
                        "PCM recording failed"
                    )

                    stopSelf()

                    return@launch
                }



            } catch (e: Exception) {

                Log.e(
                    "RAKSHAK_DEBUG",
                    "Realtime detection failed",
                    e
                )
            }
        }
    }

    // ==========================================
    // STOP
    // ==========================================
    private fun stopRealtimeDetection() {

        try {

            Log.d(
                "RAKSHAK_DEBUG",
                "Stopping realtime detection"
            )

            recorder.stopRecording()

            stopSelf()

        } catch (e: Exception) {

            Log.e(
                "RAKSHAK_DEBUG",
                "Stop failed",
                e
            )
        }
    }

    // ==========================================
    // NOTIFICATION CHANNEL
    // ==========================================
    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Call Recording & Analysis",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {

                    description =
                        "Realtime scam detection"
                }

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(
                channel
            )
        }
    }

    // ==========================================
    // UPDATE NOTIFICATION
    // ==========================================
    private fun updateNotification(
        phoneNumber: String,
        status: String
    ) {

        val notification =
            NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )
                .setContentTitle(
                    "RakshakX AI Protection"
                )
                .setContentText(
                    "$status ($phoneNumber)"
                )
                .setSmallIcon(
                    R.drawable.ic_recording
                )
                .setOngoing(true)
                .setPriority(
                    NotificationCompat.PRIORITY_LOW
                )
                .build()

        getSystemService(
            NotificationManager::class.java
        ).notify(
            NOTIFICATION_ID,
            notification
        )
    }

    // ==========================================
    // CREATE NOTIFICATION
    // ==========================================
    private fun createNotification(
        phoneNumber: String,
        status: String
    ): Notification {

        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle(
                "RakshakX AI Protection"
            )
            .setContentText(
                "$status ($phoneNumber)"
            )
            .setSmallIcon(
                R.drawable.ic_recording
            )
            .setOngoing(true)
            .setPriority(
                NotificationCompat.PRIORITY_LOW
            )
            .build()
    }

    // ==========================================
    // STATUS BROADCAST
    // ==========================================
    private fun broadcastCaptureStatus(
        status: String
    ) {

        val intent =
            Intent(
                CallOverlayActivity
                    .ACTION_CAPTURE_UPDATE
            ).apply {

                putExtra(
                    CallOverlayActivity
                        .EXTRA_CAPTURE_STATUS,
                    status
                )
            }

        sendBroadcast(intent)
    }

    // ==========================================
    // SOURCE BROADCAST
    // ==========================================
    private fun broadcastCaptureSource(
        source: String
    ) {

        val intent =
            Intent(
                CallOverlayActivity
                    .ACTION_CAPTURE_SOURCE_UPDATE
            ).apply {

                putExtra(
                    CallOverlayActivity
                        .EXTRA_CAPTURE_SOURCE,
                    source
                )
            }

        sendBroadcast(intent)
    }

    // ==========================================
    // DEV TEST
    // ==========================================
    private fun runDevFraudTest() {

        scope.launch(Dispatchers.IO) {

            val testTranscript =
                "your bank account will be blocked please share otp"

            val mlResult =
                mlClassifier.computeMLResult(
                    testTranscript
                )

            Log.d(
                "RAKSHAK_DEBUG",
                "Dev score = ${mlResult.score}"
            )
        }
    }

    // ==========================================
    // BIND
    // ==========================================
    override fun onBind(
        intent: Intent?
    ): IBinder? = null

    // ==========================================
    // DESTROY
    // ==========================================
    override fun onDestroy() {

        super.onDestroy()

        Log.d(
            "RAKSHAK_DEBUG",
            "CallRecordingService destroyed"
        )

        recorder.release()

        scope.cancel()
    }
}