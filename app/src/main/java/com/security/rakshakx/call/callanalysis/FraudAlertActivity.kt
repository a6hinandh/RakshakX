package com.security.rakshakx.call.callanalysis

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.security.rakshakx.R

class FraudAlertActivity : AppCompatActivity() {

    companion object {
        fun showHighRiskWarning(context: Context, number: String, riskScore: Float, reason: String) {
            val intent = Intent(context, FraudAlertActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("phone_number", number)
                putExtra("risk_score", riskScore)
                putExtra("reason", reason)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fraud_alert)

        // Get intent extras
        val phoneNumber = intent.getStringExtra("phone_number") ?: "Unknown"
        val riskScore = intent.getFloatExtra("risk_score", 0f)
        val reason = intent.getStringExtra("reason") ?: "Suspicious activity detected"

        // Display phone number
        findViewById<TextView>(R.id.tvPhoneNumber).text = phoneNumber

        // Display risk score
        val tvRiskScore = findViewById<TextView>(R.id.tvRiskScore)
        val riskPercentage = (riskScore * 100).toInt()
        tvRiskScore.text = "$riskPercentage%"

        // Color code based on risk level using RiskConfig
        tvRiskScore.setTextColor(
            when {
                riskScore >= RiskConfig.THRESHOLD_HIGH -> getColor(RiskConfig.SYS_COLOR_HIGH)
                riskScore >= RiskConfig.THRESHOLD_MEDIUM -> getColor(RiskConfig.SYS_COLOR_MEDIUM)
                else -> getColor(RiskConfig.SYS_COLOR_BORDERLINE)
            }
        )

        // Display detailed explanation
        findViewById<TextView>(R.id.tvReason).text = reason

        triggerHapticAlert()

        // Auto-dismiss after 5 seconds (Hackathon demo requirement: no call controls)
        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 5000)
    }

    private fun triggerHapticAlert() {
        val pattern = longArrayOf(0, 150, 80, 200, 80, 300)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(pattern, -1)
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
            vibrator.vibrate(pattern, -1)
        }
    }
}


