package com.security.rakshakx.call.callanalysis

import android.Manifest
import android.app.*
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
import com.security.rakshakx.notifications.CallFraudNotifications
import com.security.rakshakx.call.callanalysis.ml.DummyFraudTextModel
import com.security.rakshakx.call.callanalysis.ml.FraudTextModel
import kotlinx.coroutines.*

/**
 * CallRecordingService
 *
 * Foreground service for call recording and analysis.
 * Required on Android 12+ to avoid background restrictions.
 * Shows persistent notification while recording.
 */
class CallRecordingService : Service() {

    companion object {
        private const val TAG = "CallRecordingService"
        private const val CHANNEL_ID = "rakshakx_recording"
        private const val NOTIFICATION_ID = 2001

        const val ACTION_START_RECORDING = "START_RECORDING"
        const val ACTION_STOP_RECORDING = "STOP_RECORDING"
        const val ACTION_DEV_TEST = "DEV_TEST"
        const val EXTRA_PHONE_NUMBER = "phone_number"

        fun startRecording(context: Context, phoneNumber: String) {
            val intent = Intent(context, CallRecordingService::class.java).apply {
                action = ACTION_START_RECORDING
                putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopRecording(context: Context) {
            val intent = Intent(context, CallRecordingService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
            context.startService(intent)
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var recorder: CallAudioRecorder
    private lateinit var transcriber: WhisperTranscriber
    private lateinit var classifier: FraudIntentClassifier
    private lateinit var mlClassifier: FraudMLClassifier
    private lateinit var engine: PreActionDecisionEngine

    // ML model dependencies (pluggable for testing)
    private val fraudTextModel: FraudTextModel by lazy { DummyFraudTextModel() }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        recorder = CallAudioRecorder(this)
        transcriber = WhisperTranscriber(this)
        classifier = FraudIntentClassifier()
        mlClassifier = FraudMLClassifier(fraudTextModel)  // Inject pluggable model
        engine = PreActionDecisionEngine()

        Log.d(TAG, "CallRecordingService initialized with DummyFraudTextModel")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: "Unknown"
                val notification = createNotification(phoneNumber, "Recording...")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                startRecordingAndAnalysis(phoneNumber)
            }
            ACTION_STOP_RECORDING -> {
                stopRecordingAndFinish()
            }
            ACTION_DEV_TEST -> {
                Log.i(TAG, "DEV_TEST action received - running ML test...")
                runDevFraudTest()
                // Stop immediately after test
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startRecordingAndAnalysis(phoneNumber: String) {
        scope.launch(Dispatchers.IO) {
            try {
                if (ContextCompat.checkSelfPermission(this@CallRecordingService, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    stopSelf()
                    return@launch
                }

                // Update notification
                updateNotification(phoneNumber, "Listening for fraud patterns...")

                // Continuous Interception Loop
                while (isActive) {
                    // 1. Record a 5-second chunk
                    val audioPath = recorder.startPcmRecording() ?: break
                    delay(5000)
                    recorder.stopRecording()

                    // 2. Transcribe Chunk
                    val pcmData = recorder.getPcmData() ?: continue
                    val transcript = try {
                        withTimeout(5000) { transcriber.transcribePcm(pcmData) }
                    } catch (_: Exception) { "" }

                    if (transcript.isNotBlank()) {
                        // 3. Broadcast to Overlay
                        val intent = Intent(CallOverlayActivity.ACTION_TRANSCRIPT_UPDATE).apply {
                            putExtra(CallOverlayActivity.EXTRA_TRANSCRIPT, transcript)
                        }
                        sendBroadcast(intent)

                        // 4. Hybrid Analysis
                        val mlResult = mlClassifier.computeMLResult(transcript)
                        val (hybridScore, explanation) = classifier.computeHybridScore(transcript, mlResult)

                        Log.d(TAG, "Live Analysis: score=$hybridScore, transcript=$transcript")

                        // 5. Trigger Alert if High Risk
                        if (hybridScore >= RiskConfig.THRESHOLD_HIGH) {
                            withContext(Dispatchers.Main) {
                                FraudAlertActivity.showHighRiskWarning(
                                    this@CallRecordingService,
                                    phoneNumber,
                                    hybridScore,
                                    explanation
                                )
                            }
                            // Persist result
                            saveCallRecord(phoneNumber, transcript, hybridScore, explanation, engine.decideAction(hybridScore))
                            break // Stop loop after alert
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Live Interception failed", e)
            } finally {
                stopSelf()
            }
        }
    }

    private suspend fun saveCallRecord(
        phoneNumber: String,
        transcript: String,
        score: Float,
        explanation: String,
        decision: FraudDecision
    ) {
        val repository = com.security.rakshakx.call.callanalysis.data.CallAnalysisRepository(this)
        val record = com.security.rakshakx.call.callanalysis.data.CallRecord(
            phoneNumber = phoneNumber,
            transcript = transcript,
            riskScore = score,
            action = decision.action.name,
            reason = explanation,
            timestamp = System.currentTimeMillis()
        )
        repository.saveCallRecord(record)
    }

    private fun stopRecordingAndFinish() {
        recorder.stopRecording()
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Recording & Analysis",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when RakshakX is recording and analyzing calls"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(phoneNumber: String, status: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RakshakX - Analyzing Call")
            .setContentText("$status ($phoneNumber)")
            .setSmallIcon(R.drawable.ic_recording)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(phoneNumber: String, status: String) {
        val notification = createNotification(phoneNumber, status)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Dev hook for testing ML classifier without real call recording.
     * Call this to quickly verify ML inference is working.
     */
    fun runDevFraudTest() {
        scope.launch(Dispatchers.IO) {
            val testTranscript = "sir your kyc is expiring today please share otp to avoid account block"
            val mlResult = mlClassifier.computeMLResult(testTranscript)

            Log.d(
                "RakshakX",
                "Dev ML test: score=${String.format("%.2f", mlResult.score)}, " +
                "reasons=${mlResult.reasons.joinToString("; ")}, " +
                "label=${mlResult.label}"
            )

            withContext(Dispatchers.Main) {
                Log.i(TAG, "Dev test completed. Check logcat for ML output.")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

