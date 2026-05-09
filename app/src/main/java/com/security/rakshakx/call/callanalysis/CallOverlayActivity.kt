package com.security.rakshakx.call.callanalysis

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
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
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.security.rakshakx.R

class CallOverlayActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CallOverlayActivity"
        const val ACTION_TRANSCRIPT_UPDATE = "com.security.rakshakx.TRANSCRIPT_UPDATE"
        const val EXTRA_TRANSCRIPT = "extra_transcript"
        const val ACTION_ANALYSIS_UPDATE = "com.security.rakshakx.ANALYSIS_UPDATE"
        const val EXTRA_RISK_SCORE = "extra_risk_score"
        const val EXTRA_RISK_LABEL = "extra_risk_label"
        const val EXTRA_RISK_REASON = "extra_risk_reason"
        const val ACTION_CAPTURE_UPDATE = "com.security.rakshakx.CAPTURE_UPDATE"
        const val EXTRA_CAPTURE_STATUS = "extra_capture_status"
        const val ACTION_CAPTURE_SOURCE_UPDATE = "com.security.rakshakx.CAPTURE_SOURCE_UPDATE"
        const val EXTRA_CAPTURE_SOURCE = "extra_capture_source"
    }

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private lateinit var audioManager: AudioManager
    private lateinit var phoneNumber: String
    
    private var isIntercepting = false

    private val transcriptReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val transcript = intent?.getStringExtra(EXTRA_TRANSCRIPT) ?: ""
            updateTranscriptUi(transcript)
        }
    }

    private val analysisReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val score = intent?.getFloatExtra(EXTRA_RISK_SCORE, -1f) ?: -1f
            val label = intent?.getStringExtra(EXTRA_RISK_LABEL) ?: ""
            updateRiskUi(score, label)
        }
    }

    private val captureReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getStringExtra(EXTRA_CAPTURE_STATUS) ?: "Capture: idle"
            updateCaptureStatus(status)
        }
    }

    private val captureSourceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val source = intent?.getStringExtra(EXTRA_CAPTURE_SOURCE) ?: "Source: --"
            updateCaptureSource(source)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        phoneNumber = intent.getStringExtra("phone_number") ?: "Unknown"
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        registerOverlayReceiver(transcriptReceiver, ACTION_TRANSCRIPT_UPDATE)
        registerOverlayReceiver(analysisReceiver, ACTION_ANALYSIS_UPDATE)
        registerOverlayReceiver(captureReceiver, ACTION_CAPTURE_UPDATE)
        registerOverlayReceiver(captureSourceReceiver, ACTION_CAPTURE_SOURCE_UPDATE)
        
        showFloatingWidget()
    }

    private fun showFloatingWidget() {
        if (floatingView != null) return

        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.activity_call_overlay, null)

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

        setupWidgetViews(floatingView!!)

        try {
            windowManager.addView(floatingView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add floating widget", e)
            finish()
        }
    }

    private fun setupWidgetViews(view: View) {
        val tvPhone = view.findViewById<TextView>(R.id.tvPhoneNumber)
        val btnDismiss = view.findViewById<ImageButton>(R.id.btnDismiss)
        val swIntercept = view.findViewById<Switch>(R.id.swIntercept)
        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        val llAnalysis = view.findViewById<LinearLayout>(R.id.llAnalysis)
        val tvSpeakerBadge = view.findViewById<TextView>(R.id.tvSpeakerBadge)

        tvPhone.text = phoneNumber

        btnDismiss.setOnClickListener {
            stopInterception(tvSpeakerBadge)
            removeFloatingWidget()
            finish()
        }

        swIntercept.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startInterception(tvStatus, llAnalysis, tvSpeakerBadge)
            } else {
                stopInterception(tvSpeakerBadge)
                tvStatus.text = "Ready for interception"
                llAnalysis.visibility = View.GONE
            }
        }
    }

    private fun startInterception(
        tvStatus: TextView,
        llAnalysis: LinearLayout,
        tvSpeakerBadge: TextView
    ) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_LONG).show()
            return
        }

        isIntercepting = true
        tvStatus.text = "Listening..."
        tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_light))
        llAnalysis.visibility = View.VISIBLE
        llAnalysis.findViewById<TextView>(R.id.tvTranscript)?.text = "Listening..."
        llAnalysis.findViewById<TextView>(R.id.tvRiskStatus)?.text = "Risk: --"
        llAnalysis.findViewById<TextView>(R.id.tvCaptureStatus)?.text = "Capture: speakerphone + mic active"
        llAnalysis.findViewById<TextView>(R.id.tvCaptureSource)?.text = "Source: --"
        tvSpeakerBadge.visibility = View.VISIBLE

        // 1. Force Speakerphone
        val speakerEnabled = enableSpeakerphone()
        
        // 2. Start Recording Service
        CallRecordingService.startRecording(this, phoneNumber)
        
        if (speakerEnabled) {
            Toast.makeText(this, "Speakerphone ON - Intercepting...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Enable speakerphone for better capture", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopInterception(tvSpeakerBadge: TextView) {
        if (!isIntercepting) return
        
        isIntercepting = false
        disableSpeakerphone()

        tvSpeakerBadge.visibility = View.GONE
        updateCaptureStatus("Capture: idle")
        
        CallRecordingService.stopRecording(this)
    }

    private fun updateTranscriptUi(transcript: String) {
        floatingView?.let {
            val tvTranscript = it.findViewById<TextView>(R.id.tvTranscript)
            tvTranscript.text = if (transcript.isBlank()) "Listening..." else transcript
            if (transcript.isNotBlank()) {
                triggerTranscriptHaptic()
            }
        }
    }

    private fun updateRiskUi(score: Float, label: String) {
        floatingView?.let {
            val tvRisk = it.findViewById<TextView>(R.id.tvRiskStatus)
            if (score < 0f) {
                tvRisk.text = "Risk: --"
                return
            }
            val percent = (score * 100).toInt().coerceIn(0, 100)
            val tag = if (label.isBlank()) "UNKNOWN" else label
            tvRisk.text = "Risk: ${percent}% ($tag)"
        }
    }

    private fun updateCaptureStatus(text: String) {
        floatingView?.findViewById<TextView>(R.id.tvCaptureStatus)?.text = text
    }

    private fun updateCaptureSource(text: String) {
        floatingView?.findViewById<TextView>(R.id.tvCaptureSource)?.text = text
    }

    private fun enableSpeakerphone(): Boolean {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val speaker = audioManager.availableCommunicationDevices
                .firstOrNull { it.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            if (speaker != null) {
                audioManager.setCommunicationDevice(speaker)
            }
        }
        return audioManager.isSpeakerphoneOn
    }

    private fun disableSpeakerphone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        }
        audioManager.isSpeakerphoneOn = false
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    private fun registerOverlayReceiver(receiver: BroadcastReceiver, action: String) {
        val filter = IntentFilter(action)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
    }

    private fun triggerTranscriptHaptic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(effect)
            } else {
                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(effect)
            }
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }

    private fun removeFloatingWidget() {
        floatingView?.let {
            try {
                windowManager.removeView(it)
                floatingView = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove widget", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(transcriptReceiver)
        unregisterReceiver(analysisReceiver)
        unregisterReceiver(captureReceiver)
        unregisterReceiver(captureSourceReceiver)
        floatingView?.findViewById<TextView>(R.id.tvSpeakerBadge)?.let { badge ->
            stopInterception(badge)
        }
        removeFloatingWidget()
    }
}

