package com.security.rakshakx.call.callanalysis

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
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
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.security.rakshakx.R

class OverlayBubbleService : Service() {

    companion object {
        private const val TAG = "OverlayBubbleService"
        private const val CHANNEL_ID = "rakshakx_overlay"
        private const val NOTIFICATION_ID = 2002
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var audioManager: AudioManager
    private var phoneNumber: String = "Unknown"
    private var isIntercepting = false

    private val transcriptReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val transcript = intent?.getStringExtra(CallOverlayActivity.EXTRA_TRANSCRIPT) ?: ""
            updateTranscriptUi(transcript)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        
        val filter = IntentFilter(CallOverlayActivity.ACTION_TRANSCRIPT_UPDATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(transcriptReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(transcriptReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        phoneNumber = intent?.getStringExtra("phone_number") ?: "Unknown"

        // Run as foreground service to avoid being killed
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RakshakX Shield Active")
            .setContentText("Monitoring for call fraud...")
            .setSmallIcon(R.drawable.ic_recording)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)

        showOverlay()
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        if (overlayView != null) return

        // Use a themed context to resolve attributes like ?attr/selectableItemBackgroundBorderless
        val themedContext = androidx.appcompat.view.ContextThemeWrapper(this, R.style.Theme_RakshakX)
        overlayView = LayoutInflater.from(themedContext).inflate(R.layout.activity_call_overlay, null)

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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay", e)
            stopSelf()
        }
    }

    private fun setupViews(view: View) {
        val tvPhone = view.findViewById<TextView>(R.id.tvPhoneNumber)
        val btnDismiss = view.findViewById<ImageButton>(R.id.btnDismiss)
        val swIntercept = view.findViewById<Switch>(R.id.swIntercept)
        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        val llAnalysis = view.findViewById<LinearLayout>(R.id.llAnalysis)
        val tvSpeakerBadge = view.findViewById<TextView>(R.id.tvSpeakerBadge)

        tvPhone.text = if (phoneNumber == "null" || phoneNumber == "Unknown") "Private Number" else phoneNumber

        btnDismiss.setOnClickListener {
            stopSelf()
        }

        swIntercept.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startInterception(tvStatus, llAnalysis, tvSpeakerBadge)
            } else {
                stopInterception(tvStatus, llAnalysis, tvSpeakerBadge)
            }
        }
    }

    private fun startInterception(
        tvStatus: TextView,
        llAnalysis: LinearLayout,
        tvSpeakerBadge: TextView
    ) {
        isIntercepting = true
        tvStatus.text = "Listening..."
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.cyan_400))
        llAnalysis.visibility = View.VISIBLE
        llAnalysis.findViewById<TextView>(R.id.tvTranscript)?.text = "Listening..."
        tvSpeakerBadge.visibility = View.VISIBLE

        // Force Speakerphone
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true
        
        CallRecordingService.startRecording(this, phoneNumber)
    }

    private fun stopInterception(
        tvStatus: TextView,
        llAnalysis: LinearLayout,
        tvSpeakerBadge: TextView
    ) {
        isIntercepting = false
        audioManager.isSpeakerphoneOn = false
        audioManager.mode = AudioManager.MODE_NORMAL
        
        tvStatus.text = "Ready for interception"
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
        llAnalysis.visibility = View.GONE
        tvSpeakerBadge.visibility = View.GONE
        
        CallRecordingService.stopRecording(this)
    }

    private fun updateTranscriptUi(transcript: String) {
        overlayView?.let {
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

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "RakshakX Overlay",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(transcriptReceiver)
        if (isIntercepting) {
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_NORMAL
            CallRecordingService.stopRecording(this)
        }
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}


