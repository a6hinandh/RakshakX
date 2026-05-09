package com.security.rakshakx.call.callanalysis

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

class CallStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallStateReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        Log.d(TAG, "Call state changed: $state, Number: $phoneNumber")

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                // Incoming call detected - show widget!
                Log.d(TAG, "Incoming call from: $phoneNumber")
                showCallOverlayWidget(context, phoneNumber ?: "Unknown")
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Call answered
                Log.d(TAG, "Call answered")
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                // Call ended
                Log.d(TAG, "Call ended")
                notifyCallEnded(context)
            }
        }
    }

    private fun showCallOverlayWidget(context: Context, phoneNumber: String) {
        // Use the new bubble manager instead of a full activity
        OverlayBubbleManager.showBubble(context, phoneNumber)
    }

    private fun notifyCallEnded(context: Context) {
        // Hide the bubble when call ends
        OverlayBubbleManager.hideBubble(context)
    }
}

