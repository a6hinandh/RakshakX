package com.rakshakx.callanalysis

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
import com.rakshakx.R
import com.rakshakx.callanalysis.ml.DummyFraudTextModel
import com.rakshakx.callanalysis.ml.FraudTextModel
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
                // STRICT RECORD_AUDIO CHECK BEFORE STARTING PCM
                if (ContextCompat.checkSelfPermission(
                        this@CallRecordingService,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.e("CallRecordingService", "RECORD_AUDIO not granted, cannot start PCM recording")
                    updateNotification(phoneNumber, "Analysis unavailable - mic permission missing")
                    stopSelf()
                    return@launch
                }

                // PCM Recording for Whisper
                val audioPath = recorder.startPcmRecording()
                if (audioPath == null) {
                    updateNotification(phoneNumber, "Analysis unavailable - recording failed")
                    stopSelf()
                    return@launch
                }

                // Update notification
                updateNotification(phoneNumber, "Recording & Analyzing...")

                // Record for 10 seconds (optimized for initial call analysis)
                delay(10_000)
                recorder.stopRecording()

                // Transcribe
                updateNotification(phoneNumber, "Transcribing audio (offline)...")
                val pcmData = recorder.getPcmData() ?: throw Exception("Failed to read PCM data")

                // Add timeout for transcription
                val transcript = withTimeout(10_000) {
                    transcriber.transcribePcm(pcmData)
                }

                // Hybrid Fraud Analysis
                updateNotification(phoneNumber, "Detecting fraud patterns...")

                val mlResult = mlClassifier.computeMLResult(transcript)

                // Log ML score for debugging and monitoring
                Log.d(
                    "RakshakX",
                    "ML score=${String.format("%.2f", mlResult.score)}, " +
                    "reasons=${mlResult.reasons.joinToString("; ")}, " +
                    "label=${mlResult.label}"
                )

                val (hybridScore, explanation) = classifier.computeHybridScore(transcript, mlResult)
                val decision = engine.decideAction(hybridScore)

                // Show result notification and/or high-risk warning
                if (hybridScore >= RiskConfig.THRESHOLD_HIGH) {
                    FraudAlertActivity.showHighRiskWarning(
                        this@CallRecordingService,
                        phoneNumber,
                        hybridScore,
                        explanation
                    )
                }

                // Show result notification with score and transcript snippet
                val riskLevel = RiskConfig.getRiskLevel(hybridScore)
                FraudNotificationHelper.showFraudResultNotification(
                    context = this@CallRecordingService,
                    hybridScore = hybridScore,
                    transcript = transcript,
                    riskLevel = riskLevel
                )

                // Persist result
                saveCallRecord(phoneNumber, transcript, hybridScore, explanation, decision)

            } catch (e: Exception) {
                Log.e("CallRecordingService", "Analysis failed", e)
                updateNotification(phoneNumber, "Analysis unavailable - error occurred")
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
        val repository = com.rakshakx.callanalysis.data.CallAnalysisRepository(this)
        val record = com.rakshakx.callanalysis.data.CallRecord(
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