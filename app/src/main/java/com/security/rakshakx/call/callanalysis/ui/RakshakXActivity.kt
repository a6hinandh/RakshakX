package com.security.rakshakx.call.callanalysis.ui

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.security.rakshakx.R
import com.security.rakshakx.RakshakXApplication
import com.security.rakshakx.call.callanalysis.CallAudioRecorder
import com.security.rakshakx.call.callanalysis.CallRecordingProbe
import com.security.rakshakx.call.callanalysis.DeviceCapabilities
import com.security.rakshakx.call.callanalysis.AndroidSpeechTranscriber
import com.security.rakshakx.call.callanalysis.FraudIntentClassifier
import com.security.rakshakx.call.callanalysis.PreActionDecisionEngine
import com.security.rakshakx.call.callanalysis.RiskConfig
import com.security.rakshakx.notifications.CallFraudNotifications
import com.security.rakshakx.call.callanalysis.WhisperLiteTranscriber
import com.security.rakshakx.call.callanalysis.data.CallAnalysisRepository
import com.security.rakshakx.call.callanalysis.data.CallRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity for RakshakX call analysis: lists recent calls, allows manual analysis.
 */
class RakshakXActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvCapabilityStatus: TextView
    private lateinit var adapter: CallRecordAdapter
    private lateinit var repository: CallAnalysisRepository
    private val recorder = CallAudioRecorder(this)
    private val transcriber by lazy { WhisperLiteTranscriber(this) }
    private val liveSpeechTranscriber by lazy { AndroidSpeechTranscriber(this) }
    private val classifier = FraudIntentClassifier()
    private val engine = PreActionDecisionEngine()

    // RECORD_AUDIO permission
    private val requestRecordAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                ensureCapabilityChecked()
            } else {
                Toast.makeText(
                    this,
                    "RECORD_AUDIO permission required for analysis",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // POST_NOTIFICATIONS permission (Android 13+)
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(
                    this,
                    "Notification permission not granted. Alerts may be hidden.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // READ_CALL_LOG permission
    private val requestCallLogPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                loadRecentCalls()
            } else {
                Toast.makeText(
                    this,
                    "Call log permission denied. Cannot display recent calls.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // READ_PHONE_STATE permission (for call-state monitoring)
    private val requestReadPhoneStatePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCallStateMonitorIfPossible()
            } else {
                // App still works (manual analysis), just no real-call bubble
                Toast.makeText(
                    this,
                    "READ_PHONE_STATE permission denied. Real-call overlay will be disabled.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rakshak_x)

        repository = CallAnalysisRepository(this)
        recyclerView = findViewById(R.id.recyclerView)
        tvCapabilityStatus = findViewById(R.id.tvCapabilityStatus)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = CallRecordAdapter(emptyList()) { phoneNumber ->
            analyzeCall(phoneNumber)
        }
        recyclerView.adapter = adapter

        findViewById<MaterialButton>(R.id.btnLiveAudioDebug).setOnClickListener {
            showLiveAudioDebugDialog()
        }

        checkAndRequestPermissions()
        ensureReadPhoneStatePermission()

        // Only load call logs if permission is already granted
        if (hasCallLogPermission()) {
            loadRecentCalls()
        }
    }

    override fun onResume() {
        super.onResume()
        checkCapabilityAndShowStatus()
    }

    private fun checkCapabilityAndShowStatus() {
        if (DeviceCapabilities.hasRunProbe(this)) {
            val supported = DeviceCapabilities.supportsCallRecording(this)
            if (!supported) {
                tvCapabilityStatus.visibility = android.view.View.VISIBLE
                tvCapabilityStatus.text =
                    "⚠️ Your device blocks in-call audio capture. Live analysis may be limited."
            } else {
                tvCapabilityStatus.visibility = android.view.View.VISIBLE
                tvCapabilityStatus.text = "✅ Live call analysis is supported on this device."
                tvCapabilityStatus.setBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
                tvCapabilityStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
            }
        } else {
            tvCapabilityStatus.visibility = android.view.View.GONE
        }
    }

    private fun showLiveAudioDebugDialog() {
        val whisperAvailable = transcriber.isModelAvailable()
        val speechRecognizerAvailable = liveSpeechTranscriber.isAvailable()
        val micRoutingAvailable = hasRecordAudioPermission() && isSpeakerRoutingAvailable()
        val callRecordingAllowed = DeviceCapabilities.supportsCallRecording(this)

        val message = buildString {
            appendLine("Whisper model: ${if (whisperAvailable) "available" else "missing"}")
            appendLine("SpeechRecognizer: ${if (speechRecognizerAvailable) "available" else "unavailable"}")
            appendLine("Speaker routing: ${if (micRoutingAvailable) "available" else "limited"}")
            appendLine("Call capture probe: ${if (callRecordingAllowed) "supported" else "blocked"}")
            appendLine()
            appendLine("If Whisper is missing, the app falls back to SpeechRecognizer.")
        }

        AlertDialog.Builder(this)
            .setTitle("Live Audio Debug")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun isSpeakerRoutingAvailable(): Boolean {
        val audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.availableCommunicationDevices.any { device ->
                device.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }
        } else {
            true
        }
    }

    /**
     * Ensure call recording capability is checked.
     * Should be called after RECORD_AUDIO permission is granted.
     */
    private fun ensureCapabilityChecked() {
        if (DeviceCapabilities.hasRunProbe(this)) return

        lifecycleScope.launch(Dispatchers.IO) {
            val probe = CallRecordingProbe()
            val result = probe.runProbe()
            DeviceCapabilities.setSupportsCallRecording(
                this@RakshakXActivity,
                result.success && result.hasSignal
            )
            DeviceCapabilities.setHasRunProbe(this@RakshakXActivity, true)

            withContext(Dispatchers.Main) {
                checkCapabilityAndShowStatus()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        // RECORD_AUDIO
        if (!hasRecordAudioPermission()) {
            requestRecordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        // READ_CALL_LOG
        if (!hasCallLogPermission()) {
            requestCallLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
        }

        // POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasPostNotificationsPermission()
        ) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun ensureReadPhoneStatePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCallStateMonitorIfPossible()
        } else {
            requestReadPhoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        }
    }

    private fun startCallStateMonitorIfPossible() {
        val app = application as? RakshakXApplication
        app?.callStateMonitor?.start()
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasCallLogPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasPostNotificationsPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun loadRecentCalls() {
        // Check permission before attempting to read call logs
        if (!hasCallLogPermission()) {
            Log.w("RakshakXActivity", "READ_CALL_LOG permission not granted")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val calls = getRecentCallLogs()
                withContext(Dispatchers.Main) {
                    adapter.updateCalls(calls)
                }
            } catch (e: SecurityException) {
                Log.e("RakshakXActivity", "Failed to read call logs - permission denied", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@RakshakXActivity,
                        "Permission denied for reading call logs",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("RakshakXActivity", "Failed to read call logs", e)
            }
        }
    }

    private fun getRecentCallLogs(): List<String> {
        // Double-check permission (defensive programming)
        if (!hasCallLogPermission()) {
            Log.w("RakshakXActivity", "Cannot get call logs without permission")
            return emptyList()
        }

        val calls = mutableListOf<String>()
        try {
            val cursor: Cursor? = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER),
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )
            cursor?.use {
                var count = 0
                while (it.moveToNext() && count < 20) {
                    val number = it.getString(0) ?: "Unknown"
                    calls.add(number)
                    count++
                }
            }
        } catch (e: SecurityException) {
            Log.e("RakshakXActivity", "SecurityException reading call log", e)
        } catch (e: Exception) {
            Log.e("RakshakXActivity", "Error reading call log", e)
        }
        return calls
    }

    private fun analyzeCall(phoneNumber: String) {
        // Ensure audio permission before starting
        if (!hasRecordAudioPermission()) {
            Toast.makeText(
                this,
                "RECORD_AUDIO permission not granted. Cannot analyze call.",
                Toast.LENGTH_SHORT
            ).show()
            checkAndRequestPermissions()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("RakshakXActivity", "Starting analysis for: $phoneNumber")

                // Record audio (manual demo-style)
                val audioPath = recorder.startRecording()
                if (audioPath == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@RakshakXActivity,
                            "Failed to start recording",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                // Simulate short delay for recording
                delay(5000)
                recorder.stopRecording()

                // Transcribe
                val transcript = transcriber.transcribe(audioPath)

                // Classify and decide
                val riskScore = classifier.computeRiskScore(transcript)
                val decision = engine.decideAction(riskScore)

                // Save
                val record = CallRecord(
                    phoneNumber = phoneNumber,
                    transcript = transcript,
                    riskScore = riskScore,
                    action = decision.action.name,
                    reason = decision.reason,
                    timestamp = System.currentTimeMillis()
                )
                repository.saveCallRecord(record)

                // Show warning notification if high/medium risk using RiskConfig threshold
                if (riskScore >= RiskConfig.THRESHOLD_SAFE_ROUTING) {
                    CallFraudNotifications.showRiskNotification(
                        this@RakshakXActivity,
                        record,
                        decision
                    )
                }

                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    if (hasCallLogPermission()) {
                        loadRecentCalls()
                    }
                    Toast.makeText(
                        this@RakshakXActivity,
                        "Analysis complete: ${decision.action}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                Log.d(
                    "RakshakXActivity",
                    "Analysis complete: Risk=$riskScore, Action=${decision.action}"
                )
            } catch (e: Exception) {
                Log.e("RakshakXActivity", "Analysis failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@RakshakXActivity,
                        "Analysis failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    
}


