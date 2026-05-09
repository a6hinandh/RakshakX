package com.security.rakshakx.call.callanalysis

import android.content.Context
import android.os.Build
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log

/**
 * CallStateMonitor
 *
 * Listens for real-time telephony state changes.
 * Used to trigger the RakshakX overlay bubble during active calls.
 */
class CallStateMonitor(
    private val context: Context,
    private val listener: CallStateListener
) {

    interface CallStateListener {
        fun onCallStarted(phoneNumber: String?)
        fun onCallEnded()
    }

    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private var isMonitoring = false
    private var lastState = TelephonyManager.CALL_STATE_IDLE

    // Modern API (Android 12+)
    private var telephonyCallback: Any? = null

    fun start() {
        if (isMonitoring) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleStateChange(state)
                }
            }
            telephonyManager.registerTelephonyCallback(context.mainExecutor, callback)
            telephonyCallback = callback
        } else {
            // Fallback for older APIs
            @Suppress("DEPRECATION")
            telephonyManager.listen(object : android.telephony.PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleStateChange(state)
                }
            }, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
        }

        isMonitoring = true
        Log.d("CallStateMonitor", "Call state monitoring started")
    }

    private fun handleStateChange(state: Int) {
        Log.d("CallStateMonitor", "Call state changed: $state (Previous: $lastState)")
        
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (lastState == TelephonyManager.CALL_STATE_RINGING || lastState == TelephonyManager.CALL_STATE_IDLE) {
                    Log.i("CallStateMonitor", "Call Answered / Outgoing Started")
                    listener.onCallStarted(null) // Number detection requires extra permissions/logic
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (lastState == TelephonyManager.CALL_STATE_OFFHOOK || lastState == TelephonyManager.CALL_STATE_RINGING) {
                    Log.i("CallStateMonitor", "Call Ended")
                    listener.onCallEnded()
                }
            }
            TelephonyManager.CALL_STATE_RINGING -> {
                Log.i("CallStateMonitor", "Incoming Call Ringing")
                // Bubble can also be shown here if desired
            }
        }
        lastState = state
    }

    fun stop() {
        if (!isMonitoring) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (telephonyCallback as? TelephonyCallback)?.let {
                telephonyManager.unregisterTelephonyCallback(it)
            }
        } else {
            @Suppress("DEPRECATION")
            telephonyManager.listen(null, android.telephony.PhoneStateListener.LISTEN_NONE)
        }

        isMonitoring = false
        Log.d("CallStateMonitor", "Call state monitoring stopped")
    }
}


