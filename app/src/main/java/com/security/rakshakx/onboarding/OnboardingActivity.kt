package com.security.rakshakx.onboarding

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.security.rakshakx.MainActivity
import com.security.rakshakx.permissions.PermissionManager

class OnboardingActivity : ComponentActivity() {
    private var readinessState by mutableStateOf(
        PermissionManager.ReadinessState(
            corePermissionsGranted = false,
            notificationListenerEnabled = false,
            accessibilityEnabled = false,
            smsReady = false,
            callReady = false,
            emailReady = false,
            webReady = false,
            minimumDashboardReady = false
        )
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshReadiness()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshReadiness()
        setContent {
            OnboardingScreen(
                readinessState = readinessState,
                missingRequirements = PermissionManager.getMissingOnboardingRequirements(this),
                onComplete = {
                    if (!readinessState.minimumDashboardReady) return@OnboardingScreen
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                },
                onRequestCorePermissions = {
                    permissionLauncher.launch(PermissionManager.corePermissionsForRuntimeRequest())
                },
                onRequestAccessibility = {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                onRequestNotificationAccess = {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshReadiness()
    }

    private fun refreshReadiness() {
        readinessState = PermissionManager.getReadinessState(this)
    }
}

@Composable
fun OnboardingScreen(
    readinessState: PermissionManager.ReadinessState,
    missingRequirements: List<String>,
    onComplete: () -> Unit,
    onRequestCorePermissions: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestNotificationAccess: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Welcome to RakshakX", style = MaterialTheme.typography.headlineLarge)
        Text("Privacy-first autonomous fraud interception.")

        Spacer(Modifier.height(32.dp))

        PermissionItem(
            title = "SMS & Call Protection",
            description = "Required to detect scam messages and calls",
            granted = readinessState.corePermissionsGranted,
            onClick = onRequestCorePermissions
        )
        PermissionItem(
            title = "Web Protection",
            description = "Accessibility service for browser threat detection",
            granted = readinessState.accessibilityEnabled,
            onClick = onRequestAccessibility
        )
        PermissionItem(
            title = "Notification Guard",
            description = "Scan emails and app alerts for phishing",
            granted = readinessState.notificationListenerEnabled,
            onClick = onRequestNotificationAccess
        )

        if (missingRequirements.isNotEmpty()) {
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text("Before dashboard, complete:")
                    missingRequirements.forEach { req ->
                        Text("- $req", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth(),
            enabled = readinessState.minimumDashboardReady
        ) {
            Text(
                if (readinessState.minimumDashboardReady) {
                    "Go to Dashboard"
                } else {
                    "Complete Required Access First"
                }
            )
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    granted: Boolean,
    onClick: (() -> Unit)? = null
) {
    Card(onClick = { onClick?.invoke() }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            Text(
                if (granted) "Status: Granted" else "Status: Required",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
