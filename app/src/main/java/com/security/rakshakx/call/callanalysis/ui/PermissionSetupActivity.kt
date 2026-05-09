package com.security.rakshakx.call.callanalysis.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.security.rakshakx.R
import com.security.rakshakx.call.callanalysis.DeviceCapabilityChecker
import com.security.rakshakx.call.callanalysis.PermissionManager

/**
 * PermissionSetupActivity
 *
 * Guides user through permission setup with device-specific info.
 */
class PermissionSetupActivity : AppCompatActivity() {

    private lateinit var permissionManager: PermissionManager
    private lateinit var capabilityChecker: DeviceCapabilityChecker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create simple layout programmatically since XML might not exist
        val layout = createPermissionLayout()
        setContentView(layout)

        permissionManager = PermissionManager(this)
        capabilityChecker = DeviceCapabilityChecker(this)

        val tvStatus = layout.findViewById<TextView>(1)
        val tvCapabilities = layout.findViewById<TextView>(2)
        val btnGrant = layout.findViewById<Button>(3)
        val btnContinue = layout.findViewById<Button>(4)

        // Show missing permissions
        val missing = permissionManager.getMissingPermissions()
        if (missing.isEmpty()) {
            tvStatus.text = "✓ All permissions granted"
            btnGrant.isEnabled = false
        } else {
            tvStatus.text = "Missing permissions:\n${missing.joinToString("\n") { "• $it" }}"
        }

        // Show device capabilities
        tvCapabilities.text = capabilityChecker.getCapabilityReport()

        btnGrant.setOnClickListener {
            if (!permissionManager.hasOverlayPermission()) {
                permissionManager.requestOverlayPermission(this)
            }
            if (!permissionManager.hasAllPermissions()) {
                permissionManager.requestPermissions(this)
            }
        }

        btnContinue.setOnClickListener {
            val mode = permissionManager.getOperationMode()
            // TODO: Save mode to preferences and proceed to main activity
            finish()
        }
    }

    private fun createPermissionLayout(): android.widget.LinearLayout {
        return android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(64, 64, 64, 64)
            gravity = android.view.Gravity.CENTER

            // Shield emoji
            addView(TextView(this@PermissionSetupActivity).apply {
                text = "🛡️"
                textSize = 80f
                gravity = android.view.Gravity.CENTER
            })

            // Title
            addView(TextView(this@PermissionSetupActivity).apply {
                text = "RakshakX Setup"
                textSize = 28f
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                setPadding(0, 0, 0, 64)
            })

            // Permission status (id = 1)
            addView(TextView(this@PermissionSetupActivity).apply {
                id = 1
                text = "Checking permissions..."
                textSize = 16f
                setPadding(0, 0, 0, 48)
            })

            // Device capabilities (id = 2)
            addView(TextView(this@PermissionSetupActivity).apply {
                id = 2
                text = "Checking device capabilities..."
                textSize = 14f
                setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
                setPadding(32, 32, 32, 32)
                typeface = android.graphics.Typeface.MONOSPACE
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 64)
                }
            })

            // Grant permissions button (id = 3)
            addView(Button(this@PermissionSetupActivity).apply {
                id = 3
                text = "Grant Permissions"
                textSize = 18f
                setPadding(32, 32, 32, 32)
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 32)
                }
            })

            // Continue button (id = 4)
            addView(Button(this@PermissionSetupActivity).apply {
                id = 4
                text = "Continue"
                textSize = 18f
                setPadding(32, 32, 32, 32)
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
            })
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Refresh status after permissions granted
        recreate()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Refresh status after overlay permission
        recreate()
    }
}

