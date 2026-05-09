package com.security.rakshakx

import android.app.Application
import android.util.Log
import com.security.rakshakx.call.callanalysis.CallStateMonitor
import com.security.rakshakx.call.callanalysis.OverlayBubbleManager

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

    override fun onCreate() {
        super.onCreate()
        Log.d("RakshakXApplication", "Application onCreate")

        // Do NOT start telephony listening here.
        // Just prepare a monitor instance; Activities will start/stop it safely.
        callStateMonitor = CallStateMonitor(
            context = this,
            listener = object : CallStateMonitor.CallStateListener {
                override fun onCallStarted(phoneNumber: String?) {
                    Log.i("RakshakXApplication", "Call started detected")
                    OverlayBubbleManager.showBubble(this@RakshakXApplication, phoneNumber)
                }

                override fun onCallEnded() {
                    Log.i("RakshakXApplication", "Call ended detected")
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
    }
}

