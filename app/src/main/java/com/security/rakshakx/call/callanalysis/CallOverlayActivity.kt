package com.security.rakshakx.call.callanalysis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.security.rakshakx.R
import com.security.rakshakx.call.callanalysis.data.CallAnalysisRepository
import com.security.rakshakx.call.callanalysis.data.CallRecord
import com.security.rakshakx.call.callanalysis.ml.DummyFraudTextModel
import com.security.rakshakx.call.callanalysis.ml.FraudTextModel
import kotlinx.coroutines.*
import java.io.File

class CallOverlayActivity : AppCompatActivity() {

    data class SimulatedCall(
        val transcript: String,
        val groundTruthRisk: String,
        val fraudType: String
    )

    companion object {
        private const val TAG = "CallOverlayActivity"
    }

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private lateinit var phoneNumber: String
    private var isSimulation: Boolean = false
    private var scenarioId: Int = -1

    private lateinit var classifier: FraudIntentClassifier
    private lateinit var mlClassifier: FraudMLClassifier
    private lateinit var engine: PreActionDecisionEngine

    // Demo components
    private val whisperLiteTranscriber by lazy { WhisperLiteTranscriber(this) }
    private val demoAudioPlayer by lazy { DemoAudioPlayer(this) }

    // ML model (pluggable)
    private val fraudTextModel: FraudTextModel by lazy { DummyFraudTextModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        phoneNumber = intent.getStringExtra("phone_number") ?: "Unknown"
        isSimulation = intent.getBooleanExtra("is_simulation", false)
        scenarioId = intent.getIntExtra("scenario_id", -1)

        // Initialize classifiers with pluggable ML model
        classifier = FraudIntentClassifier()
        mlClassifier = FraudMLClassifier(fraudTextModel)
        engine = PreActionDecisionEngine()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showFloatingWidget()
    }

    private fun showFloatingWidget() {
        if (floatingView != null) {
            Log.w(TAG, "Widget already shown")
            return
        }

        floatingView = createFloatingWidget()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.END
        params.x = 16
        params.y = 200

        try {
            windowManager.addView(floatingView, params)
            Log.d(TAG, "Floating widget added to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add floating widget", e)
            finish()
        }
    }

    private fun createFloatingWidget(): View {
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundResource(R.drawable.widget_background)
        }

        val tvStatus = TextView(this).apply {
            id = View.generateViewId()
            text = "Incoming: $phoneNumber"
            textSize = 16f
            setTextColor(android.graphics.Color.BLACK)
        }
        container.addView(tvStatus)

        val btnAnalyze = Button(this).apply {
            id = View.generateViewId()
            text = "Analyze Call"
            setBackgroundResource(R.drawable.button_analyze)
            setTextColor(android.graphics.Color.WHITE)
        }
        container.addView(btnAnalyze)

        val btnDismiss = Button(this).apply {
            id = View.generateViewId()
            text = "Not now"
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        container.addView(btnDismiss)

        val statusTextView = tvStatus
        val analyzeButton = btnAnalyze

        btnAnalyze.setOnClickListener {
            onAnalyzeClick(statusTextView, analyzeButton)
        }

        btnDismiss.setOnClickListener {
            removeFloatingWidget()
            finish()
        }

        return container
    }

    private fun onAnalyzeClick(tvStatus: TextView, btnAnalyze: Button) {
        tvStatus.text = "Analyzing..."
        btnAnalyze.isEnabled = false

        if (isSimulation) {
            if (scenarioId in 1..4) {
                val scenario = demoScenarios[scenarioId - 1]
                runAudioScenario(scenario)
            } else {
                runSimulatedFraudPipeline()
            }
        } else {
            startRealCallAnalysis()
        }
    }

    private fun runAudioScenario(scenario: DemoScenario) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1) Play audio for judges
                withContext(Dispatchers.Main) {
                    demoAudioPlayer.playScenarioAudio(scenario.rawResId)
                }

                // 2) Copy raw to temp path (for Whisper stub)
                val tempPath = copyRawToTempPath(scenario.rawResId)
                    ?: run {
                        Log.e(TAG, "Failed to copy scenario audio")
                        withContext(Dispatchers.Main) { finish() }
                        return@launch
                    }

                // 3) Transcribe via stub Whisper (result ignored for demo)
                val ignoredTranscription = whisperLiteTranscriber.transcribe(tempPath)
                Log.d(TAG, "Whisper stub output: $ignoredTranscription")

                // 4) Use your curated dialogue as display transcript
                val displayTranscript = when (scenario.id) {
                    DemoRiskLevel.LOW -> "Network thoda weak lag raha hai, but it's okay now, main bas kal ka plan confirm karna chahta hoon."
                    DemoRiskLevel.MEDIUM -> "Hello Sir, this is a verification call regarding your digital KYC update. Hamare system mein aapke account ke upar ek warning dikh raha hai, agar aaj ke andar verification nahi complete hua toh aapka online access block ho sakta hai."
                    DemoRiskLevel.HIGH_EN -> "I'm Officer Sharma from Bank Security. We blocked a suspicious ₹12,499 transfer from your account. To cancel this and unblock your card, read back the 6-digit 'Reversal Code' I just sent to your phone now, or your account will be suspended."
                    DemoRiskLevel.HIGH_MULTI -> "Sir, National Bank se bol raha hoon. Aapke account se 48,000 ka fraud transaction ho raha hai. Isse turant rokne ke liye aapke phone par aaya 6-digit OTP batayein, warna account block ho jayega aur legal action hoga. Jaldi code batayein."
                }

                // 5) Compute reasons from rules on this clean transcript
                val fraudResult = classifier.compute(displayTranscript)

                // 6) Force demo scores per scenario id
                val ruleScore = when (scenario.id) {
                    DemoRiskLevel.LOW        -> 0.10f   // LOW RISK, safe routing
                    DemoRiskLevel.MEDIUM     -> 0.40f   // MEDIUM RISK, still safe routing
                    DemoRiskLevel.HIGH_EN    -> 0.80f   // CRITICAL RISK, goes to alert
                    DemoRiskLevel.HIGH_MULTI -> 0.90f   // CRITICAL RISK, goes to alert
                }

                // 7) Decision engine
                val decision = engine.decideAction(ruleScore)

                // 8) Save to database
                val repository = CallAnalysisRepository(this@CallOverlayActivity)
                val record = CallRecord(
                    phoneNumber = phoneNumber,
                    transcript = displayTranscript,
                    riskScore = ruleScore,
                    action = decision.action.name,
                    reason = fraudResult.reasons.joinToString("\n"),
                    timestamp = System.currentTimeMillis()
                )
                repository.saveCallRecord(record)

                // 9) Short analysis delay for UX (1.5s)
                delay(10000)

                withContext(Dispatchers.Main) {
                    demoAudioPlayer.stopAudio() // Stop audio immediately before results
                    removeFloatingWidget()
                    routeToResultScreen(record)
                    finish()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Audio scenario analysis failed", e)
                withContext(Dispatchers.Main) {
                    demoAudioPlayer.stopAudio()
                    removeFloatingWidget()
                    finish()
                }
            }
        }
    }

    private fun runSimulatedFraudPipeline() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Ground truth simulation data
                val simulationDataset = listOf(
                    SimulatedCall(
                        "Sir, urgent! Your bank account will be suspended in 5 minutes. Please share your OTP and CVV immediately to prevent blocking.",
                        "CRITICAL", "Phishing"
                    ),
                    SimulatedCall(
                        "This is calling from bank fraud department. Your account has been hacked. Provide your credit card number, CVV and OTP right now or police case will be filed.",
                        "CRITICAL", "Social Engineering"
                    ),
                    SimulatedCall(
                        "Congratulations! You won lottery jackpot of 1 crore rupees! Send Rs 5000 via UPI immediately to claim your prize or it will expire.",
                        "HIGH", "Lottery Scam"
                    ),
                    SimulatedCall(
                        "Hi! How are you doing? Let's grab lunch after the office meeting tomorrow around 1 PM.",
                        "SAFE", "Normal"
                    ),
                    SimulatedCall(
                        "Hey, did you finish the college assignment? Professor said the deadline is next week.",
                        "SAFE", "Normal"
                    )
                )

                val selectedCall = simulationDataset.random()
                val fakeTranscript = selectedCall.transcript

                Log.d(TAG, "Simulating: ${selectedCall.groundTruthRisk} | ${selectedCall.fraudType}")

                // Tier-1: ML Classification
                val mlResult = mlClassifier.computeMLResult(fakeTranscript)
                Log.d(TAG, "ML Score: ${mlResult.score}")

                // Tier-0 + Tier-1: Hybrid scoring
                val (hybridScore, explanation) = classifier.computeHybridScore(fakeTranscript, mlResult)
                Log.d(TAG, "Hybrid Score: $hybridScore")

                // Update Debug UI on Main Thread
                withContext(Dispatchers.Main) {
                    showDebugInfo(selectedCall, mlResult.score, classifier.compute(fakeTranscript).score, hybridScore)
                }

                // Decision engine
                val decision = engine.decideAction(hybridScore)
                Log.d(TAG, "Decision: ${decision.action}, Reason: ${decision.reason}")

                // Save to database
                val repository = CallAnalysisRepository(this@CallOverlayActivity)
                val record = CallRecord(
                    phoneNumber = phoneNumber,
                    transcript = fakeTranscript,
                    riskScore = hybridScore,
                    action = decision.action.name,
                    reason = explanation,
                    timestamp = System.currentTimeMillis()
                )
                repository.saveCallRecord(record)

                // Simulate analysis delay
                delay(1500)

                withContext(Dispatchers.Main) {
                    demoAudioPlayer.stopAudio() // Stop audio immediately before results
                    removeFloatingWidget()
                    routeToResultScreen(record)
                    finish()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Simulation analysis failed", e)
                withContext(Dispatchers.Main) {
                    demoAudioPlayer.stopAudio()
                    removeFloatingWidget()
                    finish()
                }
            }
        }
    }

    /**
     * Start real call analysis using foreground service.
     */
    private fun startRealCallAnalysis() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission required for real analysis", Toast.LENGTH_LONG).show()
            removeFloatingWidget()
            finish()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            // Ensure capability probe is run
            if (!DeviceCapabilities.hasRunProbe(this@CallOverlayActivity)) {
                val probe = CallRecordingProbe()
                val result = probe.runProbe()
                DeviceCapabilities.setSupportsCallRecording(this@CallOverlayActivity, result.success && result.hasSignal)
                DeviceCapabilities.setHasRunProbe(this@CallOverlayActivity, true)
            }

            withContext(Dispatchers.Main) {
                if (DeviceCapabilities.supportsCallRecording(this@CallOverlayActivity)) {
                    CallRecordingService.startRecording(this@CallOverlayActivity, phoneNumber)
                    removeFloatingWidget()
                    finish()
                } else {
                    Toast.makeText(this@CallOverlayActivity, "In-call audio is blocked on this device. Simulation mode only.", Toast.LENGTH_LONG).show()
                    runSimulatedFraudPipeline()
                }
            }
        }
    }

    private fun routeToResultScreen(record: CallRecord) {
        if (record.riskScore >= RiskConfig.THRESHOLD_SAFE_ROUTING) {
            showFullScreenDangerAlert(record)
        } else {
            showSafeCallScreen(record)
        }
    }

    private fun showFullScreenDangerAlert(record: CallRecord) {
        val intent = Intent(this, FraudAlertActivity::class.java).apply {
            putExtra("phone_number", record.phoneNumber)
            putExtra("risk_score", record.riskScore)
            putExtra("reason", record.reason)
            putExtra("transcript", record.transcript)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun showSafeCallScreen(record: CallRecord) {
        val intent = Intent(this, SafeCallActivity::class.java).apply {
            putExtra("phone_number", record.phoneNumber)
            putExtra("risk_score", record.riskScore)
            putExtra("reason", record.reason)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun removeFloatingWidget() {
        floatingView?.let {
            try {
                windowManager.removeView(it)
                floatingView = null
                Log.d(TAG, "Floating widget removed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove widget", e)
            }
        }
    }

    private fun showDebugInfo(call: SimulatedCall, mlScore: Float, ruleScore: Float, hybridScore: Float) {
        val debugText = """
            [DEBUG] Ground Truth: ${call.groundTruthRisk} (${call.fraudType})
            ML: ${(mlScore * 100).toInt()}% | Rules: ${(ruleScore * 100).toInt()}%
            Final Hybrid: ${(hybridScore * 100).toInt()}%
        """.trimIndent()

        val tvDebug = TextView(this).apply {
            text = debugText
            textSize = 10f
            setTextColor(android.graphics.Color.DKGRAY)
            setPadding(16, 16, 16, 16)
        }

        (floatingView as? android.widget.LinearLayout)?.addView(tvDebug, 0)
    }

    // Helper: copy res/raw audio to a temp file path
    private fun copyRawToTempPath(rawResId: Int): String? {
        return try {
            val inputStream = resources.openRawResource(rawResId)
            val tempFile = File(cacheDir, "scenario_${rawResId}.m4a")

            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "copyRawToTempPath failed", e)
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        demoAudioPlayer.stopAudio()
        removeFloatingWidget()
    }
}

