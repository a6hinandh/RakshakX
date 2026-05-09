package com.security.rakshakx

import android.app.Application
import android.util.Log
import com.security.rakshakx.call.callanalysis.CallStateMonitor
import com.security.rakshakx.call.callanalysis.OverlayBubbleManager
import com.security.rakshakx.integration.ScamClassifierRouter

/**
 * RakshakXApplication
 *
 * Application class to initialize global singletons and provide
 * an application context for components like the overlay manager.
 *
 * IMPORTANT:
 * - Do NOT start CallStateMonitor here, because TelephonyManager
 *   registration can throw SecurityException if READ_PHONE_STATE
 *   is not yet granted.
 * - CallStateMonitor should be started from an Activity AFTER
 *   runtime permissions are granted.
 */
class RakshakXApplication : Application() {

    // Optional: keep a reference that Activities can use
    var callStateMonitor: CallStateMonitor? = null
        private set

    companion object {
        private const val TAG = "RakshakXApplication"

        // Global singleton router — all channel detectors use this
        var scamRouter: ScamClassifierRouter? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate")

        // ── ScamClassifierRouter (DistilBERT + IndicBERT) ────────────────────
        // Initialized here so all channel detectors (SMS, Email, Call, Web)
        // share one instance. Models load from assets at startup.
        scamRouter = ScamClassifierRouter(applicationContext)

        // ── CallStateMonitor ─────────────────────────────────────────────────
        // Do NOT start telephony listening here.
        // Just prepare a monitor instance; Activities will start/stop it safely.
        callStateMonitor = CallStateMonitor(
            context = this,
            listener = object : CallStateMonitor.CallStateListener {

                override fun onCallStarted(phoneNumber: String?) {
                    Log.i(TAG, "Call started detected")
                    OverlayBubbleManager.showBubble(
                        this@RakshakXApplication,
                        phoneNumber
                    )
                }

                override fun onCallEnded() {
                    Log.i(TAG, "Call ended detected")
                    OverlayBubbleManager.hideBubble(this@RakshakXApplication)
                }
            }
        )

        // DO NOT call callStateMonitor?.start() here.
        // Start it from an Activity after READ_PHONE_STATE is granted.
    }

    override fun onTerminate() {
        super.onTerminate()

        callStateMonitor?.stop()
        scamRouter?.release()
    }
}