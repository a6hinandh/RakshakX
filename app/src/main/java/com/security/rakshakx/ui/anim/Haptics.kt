package com.security.rakshakx.ui.anim

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

class HapticController(private val view: View, private val context: Context) {

    @Suppress("DEPRECATION")
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        mgr.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private fun perform(constant: Int) {
        val success = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                view.performHapticFeedback(constant)
            } else {
                @Suppress("DEPRECATION")
                view.performHapticFeedback(
                    constant,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING or
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            }
        } catch (_: Exception) {
            false
        }
        if (!success) {
            vibrateMs(20)
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrateMs(ms: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                vibrator.vibrate(ms)
            }
        } catch (_: Exception) { }
    }

    fun tick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            perform(HapticFeedbackConstants.CLOCK_TICK)
        } else {
            perform(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    fun click() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            perform(HapticFeedbackConstants.CONFIRM)
        } else {
            perform(HapticFeedbackConstants.CONTEXT_CLICK)
        }
    }

    fun heavyClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            perform(HapticFeedbackConstants.REJECT)
        } else {
            perform(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    fun success() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            perform(HapticFeedbackConstants.CONFIRM)
        } else {
            perform(HapticFeedbackConstants.CONTEXT_CLICK)
        }
    }

    fun warning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            perform(HapticFeedbackConstants.REJECT)
        } else {
            perform(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    fun toggleOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            perform(HapticFeedbackConstants.TOGGLE_ON)
        } else {
            click()
        }
    }

    fun toggleOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            perform(HapticFeedbackConstants.TOGGLE_OFF)
        } else {
            tick()
        }
    }
}

@Composable
fun rememberHaptics(): HapticController {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(view) { HapticController(view, context) }
}
