package com.security.rakshakx.onboarding

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.security.rakshakx.permissions.PermissionManager
import com.security.rakshakx.MainActivity

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OnboardingScreen(
                onComplete = {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
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
}

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
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
        
        PermissionItem("SMS & Call Protection", "Required to detect scam messages and calls")
        PermissionItem("Web Protection", "Accessibility service for browser threat detection", onClick = onRequestAccessibility)
        PermissionItem("Notification Guard", "Scan emails and app alerts for phishing", onClick = onRequestNotificationAccess)
        
        Spacer(Modifier.weight(1f))
        
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Go to Dashboard")
        }
    }
}

@Composable
fun PermissionItem(title: String, description: String, onClick: (() -> Unit)? = null) {
    Card(onClick = { onClick?.invoke() }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}
