package com.security.rakshakx.call.callanalysis

import android.content.Context
import android.content.Intent

/**
 * OverlayBubbleManager
 *
 * Helper to start and stop the OverlayBubbleService.
 */
object OverlayBubbleManager {

    fun showBubble(context: Context, phoneNumber: String?) {
        val intent = Intent(context, OverlayBubbleService::class.java).apply {
            putExtra("phone_number", phoneNumber)
        }
        context.startService(intent)
    }

    fun hideBubble(context: Context) {
        val intent = Intent(context, OverlayBubbleService::class.java)
        context.stopService(intent)
    }
}


