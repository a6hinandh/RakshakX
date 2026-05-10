package com.security.rakshakx.core.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * HapticFeedbackManager
 *
 * Provides high-intensity haptic patterns for critical security alerts.
 */
class HapticFeedbackManager(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Triggers a strong, multi-pulse vibration pattern to warn the user.
     */
    fun triggerStrongWarning() {
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Pattern: [Wait, Vibrate, Wait, Vibrate, Wait, Strong Vibrate]
                val timings = longArrayOf(0, 100, 50, 100, 50, 400)
                val amplitudes = intArrayOf(0, 150, 0, 150, 0, 255)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                v.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(longArrayOf(0, 200, 100, 200, 100, 500), -1)
            }
        }
    }

    /**
     * A sharp, quick double-tap for subtle alerts.
     */
    fun triggerDoubleTap() {
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(
                    longArrayOf(0, 40, 60, 40),
                    intArrayOf(0, 200, 0, 200),
                    -1
                )
                v.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(longArrayOf(0, 50, 50, 50), -1)
            }
        }
    }
}
