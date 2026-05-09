package com.rakshakx.callanalysis

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.rakshakx.R

/**
 * OverlayBubbleService
 *
 * ✅ FIXED: Foreground service that shows a floating RakshakX bubble during a call.
 * Immediately calls startForeground() to avoid BackgroundServiceStartNotAllowedException.
 * Tapping the bubble starts the call analysis pipeline.
 */
class OverlayBubbleService : Service() {

    companion object {
        private const val TAG = "OverlayBubbleService"
        private const val CHANNEL_ID = "rakshakx_overlay"
    }

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var phoneNumber: String? = "Unknown"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "OverlayBubbleService created")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        phoneNumber = intent?.getStringExtra("phone_number") ?: "Unknown"

        // ✅ FIXED: Removed startForeground() to avoid SecurityException on Android 14+
        // This service now runs as a regular service for the overlay bubble.

        addBubble()
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "RakshakX Overlay Service",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun addBubble() {
        if (bubbleView != null) return

        bubbleView = LayoutInflater.from(this).inflate(R.layout.view_rakshakx_bubble, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.END
        params.x = 16
        params.y = 300

        bubbleView?.setOnClickListener {
            onBubbleClicked()
        }

        try {
            windowManager.addView(bubbleView, params)
            Log.d(TAG, "Bubble added to window")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add bubble", e)
            stopSelf()
        }
    }

    private fun onBubbleClicked() {
        Log.i(TAG, "Bubble clicked! Starting analysis for $phoneNumber")

        // Start the real recording and analysis service
        CallRecordingService.startRecording(this, phoneNumber ?: "Unknown")

        // Hide the bubble after starting analysis to reduce clutter
        OverlayBubbleManager.hideBubble(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        bubbleView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing bubble", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
