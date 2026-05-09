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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        phoneNumber = intent.getStringExtra("phone_number") ?: "Unknown"
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        registerReceiver(transcriptReceiver, IntentFilter(ACTION_TRANSCRIPT_UPDATE))
        
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
        tvSpeakerBadge.visibility = View.VISIBLE

        // 1. Force Speakerphone
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true
        
        // 2. Start Recording Service
        CallRecordingService.startRecording(this, phoneNumber)
        
        Toast.makeText(this, "Speakerphone ON - Intercepting...", Toast.LENGTH_SHORT).show()
    }

    private fun stopInterception(tvSpeakerBadge: TextView) {
        if (!isIntercepting) return
        
        isIntercepting = false
        audioManager.isSpeakerphoneOn = false
        audioManager.mode = AudioManager.MODE_NORMAL

        tvSpeakerBadge.visibility = View.GONE
        
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
        floatingView?.findViewById<TextView>(R.id.tvSpeakerBadge)?.let { badge ->
            stopInterception(badge)
        }
        removeFloatingWidget()
    }
}

