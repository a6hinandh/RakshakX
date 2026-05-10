package com.security.rakshakx.call.callanalysis

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.security.rakshakx.R
import android.os.Handler
import android.os.Looper
class OverlayBubbleService : Service() {

    companion object {
        private const val TAG = "OverlayBubbleService"
        private const val CHANNEL_ID = "rakshakx_overlay"
        private const val NOTIFICATION_ID = 2002
    }

    private lateinit var windowManager: WindowManager
    private lateinit var audioManager: AudioManager

    private var overlayView: View? = null
    private var phoneNumber: String = "Unknown"
    private var isIntercepting = false

    // =========================
    // Transcript Receiver
    // =========================
    private val transcriptReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val transcript =
                intent?.getStringExtra(CallOverlayActivity.EXTRA_TRANSCRIPT) ?: ""

            Log.d("RAKSHAK_DEBUG", "Transcript received = $transcript")

            updateTranscriptUi(transcript)
        }
    }

    // =========================
    // Risk Receiver
    // =========================
    private val analysisReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val score =
                intent?.getFloatExtra(CallOverlayActivity.EXTRA_RISK_SCORE, -1f)
                    ?: -1f

            val label =
                intent?.getStringExtra(CallOverlayActivity.EXTRA_RISK_LABEL)
                    ?: ""

            Log.d(
                "RAKSHAK_DEBUG",
                "Risk score = $score label = $label"
            )

            updateRiskUi(score, label)
        }
    }

    // =========================
    // Capture Status Receiver
    // =========================
    private val captureReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val status =
                intent?.getStringExtra(CallOverlayActivity.EXTRA_CAPTURE_STATUS)
                    ?: "Capture: idle"

            Log.d("RAKSHAK_DEBUG", "Capture status = $status")

            updateCaptureStatus(status)
        }
    }

    // =========================
    // Capture Source Receiver
    // =========================
    private val captureSourceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val source =
                intent?.getStringExtra(CallOverlayActivity.EXTRA_CAPTURE_SOURCE)
                    ?: "Source: --"

            Log.d("RAKSHAK_DEBUG", "Capture source = $source")

            updateCaptureSource(source)
        }
    }

    // =========================
    // Service Created
    // =========================
    override fun onCreate() {
        super.onCreate()

        Log.d("RAKSHAK_DEBUG", "OverlayBubbleService created")

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        createNotificationChannel()

        val filter = IntentFilter(CallOverlayActivity.ACTION_TRANSCRIPT_UPDATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            registerReceiver(
                transcriptReceiver,
                filter,
                Context.RECEIVER_EXPORTED
            )

            registerReceiver(
                analysisReceiver,
                IntentFilter(CallOverlayActivity.ACTION_ANALYSIS_UPDATE),
                Context.RECEIVER_EXPORTED
            )

            registerReceiver(
                captureReceiver,
                IntentFilter(CallOverlayActivity.ACTION_CAPTURE_UPDATE),
                Context.RECEIVER_EXPORTED
            )

            registerReceiver(
                captureSourceReceiver,
                IntentFilter(CallOverlayActivity.ACTION_CAPTURE_SOURCE_UPDATE),
                Context.RECEIVER_EXPORTED
            )

        } else {

            registerReceiver(transcriptReceiver, filter)

            registerReceiver(
                analysisReceiver,
                IntentFilter(CallOverlayActivity.ACTION_ANALYSIS_UPDATE)
            )

            registerReceiver(
                captureReceiver,
                IntentFilter(CallOverlayActivity.ACTION_CAPTURE_UPDATE)
            )

            registerReceiver(
                captureSourceReceiver,
                IntentFilter(CallOverlayActivity.ACTION_CAPTURE_SOURCE_UPDATE)
            )
        }

        Log.d("RAKSHAK_DEBUG", "All receivers registered")
    }

    // =========================
    // Service Started
    // =========================
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        phoneNumber =
            intent?.getStringExtra("phone_number") ?: "Unknown"

        Log.d("RAKSHAK_DEBUG", "Overlay service started")
        Log.d("RAKSHAK_DEBUG", "Phone number = $phoneNumber")

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RakshakX Shield Active")
            .setContentText("Monitoring for call fraud...")
            .setSmallIcon(R.drawable.ic_recording)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        showOverlay()

        return START_NOT_STICKY
    }

    // =========================
    // Show Overlay
    // =========================
    private fun showOverlay() {

        Log.d("RAKSHAK_DEBUG", "Showing overlay")

        if (overlayView != null) {
            Log.d("RAKSHAK_DEBUG", "Overlay already exists")
            return
        }

        val themedContext =
            androidx.appcompat.view.ContextThemeWrapper(
                this,
                R.style.Theme_RakshakX
            )

        overlayView = LayoutInflater
            .from(themedContext)
            .inflate(R.layout.activity_call_overlay, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP
        params.y = 100

        setupViews(overlayView!!)

        try {

            windowManager.addView(overlayView, params)

            Log.d(
                "RAKSHAK_DEBUG",
                "Overlay added successfully"
            )

        } catch (e: Exception) {

            Log.e(
                "RAKSHAK_DEBUG",
                "Failed to add overlay",
                e
            )

            stopSelf()
        }
    }

    // =========================
    // Setup Views
    // =========================
    private fun setupViews(view: View) {

        val tvPhone =
            view.findViewById<TextView>(R.id.tvPhoneNumber)

        val btnDismiss =
            view.findViewById<ImageButton>(R.id.btnDismiss)

        val swIntercept =
            view.findViewById<Switch>(R.id.swIntercept)

        val tvStatus =
            view.findViewById<TextView>(R.id.tvStatus)

        val llAnalysis =
            view.findViewById<LinearLayout>(R.id.llAnalysis)

        val tvSpeakerBadge =
            view.findViewById<TextView>(R.id.tvSpeakerBadge)

        tvPhone.text =
            if (phoneNumber == "null" || phoneNumber == "Unknown")
                "Private Number"
            else
                phoneNumber

        btnDismiss.setOnClickListener {

            Log.d("RAKSHAK_DEBUG", "Overlay dismissed")

            stopSelf()
        }

        swIntercept.setOnCheckedChangeListener { _, isChecked ->

            Log.d(
                "RAKSHAK_DEBUG",
                "Intercept toggled = $isChecked"
            )

            if (isChecked) {

                startInterception(
                    tvStatus,
                    llAnalysis,
                    tvSpeakerBadge
                )

            } else {

                stopInterception(
                    tvStatus,
                    llAnalysis,
                    tvSpeakerBadge
                )
            }
        }
    }

    // =========================
    // Start Interception
    // =========================
    private fun startInterception(
        tvStatus: TextView,
        llAnalysis: LinearLayout,
        tvSpeakerBadge: TextView
    ) {

        Log.d("RAKSHAK_DEBUG", "Starting interception")

        isIntercepting = true

        tvStatus.text = "Listening..."

        tvStatus.setTextColor(
            ContextCompat.getColor(this, R.color.cyan_400)
        )

        llAnalysis.visibility = View.VISIBLE

        llAnalysis.findViewById<TextView>(R.id.tvTranscript)?.text =
            "Listening..."

        llAnalysis.findViewById<TextView>(R.id.tvRiskStatus)?.text =
            "Risk: --"

        llAnalysis.findViewById<TextView>(R.id.tvCaptureStatus)?.text =
            "Capture: speakerphone + mic active"

        llAnalysis.findViewById<TextView>(R.id.tvCaptureSource)?.text =
            "Source: --"

        tvSpeakerBadge.visibility = View.VISIBLE

        enableSpeakerphone()

        Log.d("RAKSHAK_DEBUG", "Starting recording service")

        try {

            CallRecordingService.startRecording(this, phoneNumber)

            Log.d(
                "RAKSHAK_DEBUG",
                "Recording service start request sent"
            )

        } catch (e: Exception) {

            Log.e(
                "RAKSHAK_DEBUG",
                "Failed to start recording service",
                e
            )
        }
    }

    // =========================
    // Stop Interception
    // =========================
    private fun stopInterception(
        tvStatus: TextView,
        llAnalysis: LinearLayout,
        tvSpeakerBadge: TextView
    ) {

        Log.d("RAKSHAK_DEBUG", "Stopping interception")

        isIntercepting = false

        disableSpeakerphone()

        tvStatus.text = "Ready for interception"

        tvStatus.setTextColor(
            ContextCompat.getColor(this, R.color.text_muted)
        )

        llAnalysis.visibility = View.GONE

        tvSpeakerBadge.visibility = View.GONE

        CallRecordingService.stopRecording(this)
    }

    // =========================
    // Update Transcript UI
    // =========================
    private fun updateTranscriptUi(transcript: String) {

        overlayView?.let {

            val tvTranscript =
                it.findViewById<TextView>(R.id.tvTranscript)

            tvTranscript.text =
                if (transcript.isBlank())
                    "Listening..."
                else
                    transcript

            if (transcript.isNotBlank()) {
                triggerTranscriptHaptic()
            }
        }
    }

    // =========================
    // Update Risk UI
    // =========================
    private fun updateRiskUi(score: Float, label: String) {

        overlayView?.let {

            val tvRisk =
                it.findViewById<TextView>(R.id.tvRiskStatus)

            if (score < 0f) {

                tvRisk.text = "Risk: --"
                return
            }

            val percent =
                (score * 100).toInt().coerceIn(0, 100)

            val tag =
                if (label.isBlank()) "UNKNOWN" else label

            tvRisk.text = "Risk: ${percent}% ($tag)"
        }
    }

    private fun updateCaptureStatus(text: String) {
        overlayView
            ?.findViewById<TextView>(R.id.tvCaptureStatus)
            ?.text = text
    }

    private fun updateCaptureSource(text: String) {
        overlayView
            ?.findViewById<TextView>(R.id.tvCaptureSource)
            ?.text = text
    }

    // =========================
    // Speakerphone
    // =========================

    private fun enableSpeakerphone() {

        try {

            Log.d(
                "RAKSHAK_DEBUG",
                "Enabling speakerphone"
            )

            audioManager.mode =
                AudioManager.MODE_IN_COMMUNICATION

            audioManager.isMicrophoneMute = false

            audioManager.adjustStreamVolume(
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.ADJUST_RAISE,
                0
            )

            audioManager.setStreamVolume(
                AudioManager.STREAM_VOICE_CALL,
                audioManager.getStreamMaxVolume(
                    AudioManager.STREAM_VOICE_CALL
                ),
                0
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                val devices =
                    audioManager.availableCommunicationDevices

                val speaker =
                    devices.firstOrNull {
                        it.type ==
                                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    }

                if (speaker != null) {

                    val success =
                        audioManager.setCommunicationDevice(
                            speaker
                        )

                    Log.d(
                        "RAKSHAK_DEBUG",
                        "Communication device success = $success"
                    )
                }

            } else {

                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = true
            }

            Handler(Looper.getMainLooper()).postDelayed({

                Log.d(
                    "RAKSHAK_DEBUG",
                    "Audio mode = ${audioManager.mode}"
                )

                Log.d(
                    "RAKSHAK_DEBUG",
                    "Speaker state = ${audioManager.isSpeakerphoneOn}"
                )

            }, 1500)

        } catch (e: Exception) {

            Log.e(
                "RAKSHAK_DEBUG",
                "Speaker enable failed",
                e
            )
        }
    }

    private fun disableSpeakerphone() {

        Log.d("RAKSHAK_DEBUG", "Disabling speakerphone")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        }

        audioManager.isSpeakerphoneOn = false
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    // =========================
    // Haptic
    // =========================
    private fun triggerTranscriptHaptic() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val effect =
                VibrationEffect.createOneShot(
                    30,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                val vibratorManager =
                    getSystemService(VIBRATOR_MANAGER_SERVICE)
                            as VibratorManager

                vibratorManager.defaultVibrator.vibrate(effect)

            } else {

                val vibrator =
                    getSystemService(VIBRATOR_SERVICE) as Vibrator

                vibrator.vibrate(effect)
            }

        } else {

            @Suppress("DEPRECATION")
            val vibrator =
                getSystemService(VIBRATOR_SERVICE) as Vibrator

            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }

    // =========================
    // Notification Channel
    // =========================
    private fun createNotificationChannel() {

        val channel = NotificationChannel(
            CHANNEL_ID,
            "RakshakX Overlay",
            NotificationManager.IMPORTANCE_LOW
        )

        val manager =
            getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(channel)
    }

    // =========================
    // Cleanup
    // =========================
    override fun onDestroy() {

        super.onDestroy()

        Log.d("RAKSHAK_DEBUG", "OverlayBubbleService destroyed")

        try {
            unregisterReceiver(transcriptReceiver)
        } catch (e: Exception) {
            Log.e(
                "RAKSHAK_DEBUG",
                "Failed to unregister transcriptReceiver",
                e
            )
        }

        try {
            unregisterReceiver(analysisReceiver)
        } catch (e: Exception) {
            Log.e(
                "RAKSHAK_DEBUG",
                "Failed to unregister analysisReceiver",
                e
            )
        }

        try {
            unregisterReceiver(captureReceiver)
        } catch (e: Exception) {
            Log.e(
                "RAKSHAK_DEBUG",
                "Failed to unregister captureReceiver",
                e
            )
        }

        try {
            unregisterReceiver(captureSourceReceiver)
        } catch (e: Exception) {
            Log.e(
                "RAKSHAK_DEBUG",
                "Failed to unregister captureSourceReceiver",
                e
            )
        }

        if (isIntercepting) {

            disableSpeakerphone()

            CallRecordingService.stopRecording(this)
        }

        overlayView?.let {

            try {

                windowManager.removeView(it)

                Log.d(
                    "RAKSHAK_DEBUG",
                    "Overlay removed successfully"
                )

            } catch (e: Exception) {

                Log.e(
                    "RAKSHAK_DEBUG",
                    "Failed to remove overlay",
                    e
                )
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}