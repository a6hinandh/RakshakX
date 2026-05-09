package com.rakshakx.callanalysis

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.rakshakx.R

class SafeCallActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safe_call)

        // Get intent extras
        val phoneNumber = intent.getStringExtra("phone_number") ?: "Unknown"
        val riskScore = intent.getFloatExtra("risk_score", 0f)

        // Display phone number
        findViewById<TextView>(R.id.tvPhoneNumberSafe).text = phoneNumber

        // UI coloring based on risk
        val rootLayout = findViewById<android.view.View>(R.id.rootLayoutSafe)
        val tvRiskScore = findViewById<TextView>(R.id.tvRiskScoreSafe)
        val riskPercentage = (riskScore * 100).toInt()
        tvRiskScore.text = "$riskPercentage%"

        if (riskScore >= 0.3f) {
            // Medium risk scenario
            rootLayout.setBackgroundColor(getColor(R.color.orange_warning))
            tvRiskScore.setTextColor(android.graphics.Color.WHITE)
        } else {
            // Low risk scenario
            rootLayout.setBackgroundColor(getColor(R.color.green_safe))
            tvRiskScore.setTextColor(android.graphics.Color.WHITE)
        }

        // Auto-dismiss after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 3000)
    }
}