package com.security.rakshakx.call.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.security.rakshakx.call.ui.state.DashboardState

@Composable
fun DashboardScreen(
    state: DashboardState,
    onRequestPermissions: () -> Unit,
    onRequestDefaultSmsRole: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onToggleBackgroundMonitoring: (Boolean) -> Unit,
    onSmsInputChange: (String) -> Unit,
    onScanSms: () -> Unit,
    onCallInputChange: (String) -> Unit,
    onScanCall: () -> Unit,
    onLinkInputChange: (String) -> Unit,
    onScanLink: () -> Unit,
    onOpenCallAnalysis: () -> Unit,
    onDebugShowLastCall: () -> Unit,
    onStartHackathonMode: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "RakshakX Monitoring",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(text = "Status: ${state.statusText}")
        Text(text = "Latest Risk Score: ${state.latestRiskScore}")
        Text(text = "SMS permission: ${if (state.smsPermissionGranted) "Granted" else "Missing"}")
        Text(text = "Call log permission: ${if (state.callLogPermissionGranted) "Granted" else "Missing"}")
        Text(text = "Default SMS app: ${if (state.isDefaultSmsApp) "Yes" else "No"}")
        Text(text = "Notification access: ${if (state.notificationAccessEnabled) "Enabled" else "Disabled"}")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRequestPermissions) {
            Text("Request permissions")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRequestDefaultSmsRole) {
            Text("Request default SMS role")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRequestNotificationAccess) {
            Text("Enable notification access")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onOpenCallAnalysis) {
            Text("Open Call Analysis")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onDebugShowLastCall) {
            Text("Debug: Log last call")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onStartHackathonMode) {
            Text("🎤 Start Hackathon Mode")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Background monitoring")
        Switch(
            checked = state.backgroundMonitoringEnabled,
            onCheckedChange = onToggleBackgroundMonitoring
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("SMS Scanner", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.smsDemoInput,
            onValueChange = onSmsInputChange,
            label = { Text("SMS text") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onScanSms) {
            Text("Scan SMS")
        }
        if (state.smsDemoResult != null) {
            Text(text = state.smsDemoResult)
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text("Call Scanner", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.callDemoInput,
            onValueChange = onCallInputChange,
            label = { Text("Phone number") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onScanCall) {
            Text("Scan Call")
        }
        if (state.callDemoResult != null) {
            Text(text = state.callDemoResult)
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text("Link Scanner", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.linkDemoInput,
            onValueChange = onLinkInputChange,
            label = { Text("URL") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onScanLink) {
            Text("Scan Link")
        }
        if (state.linkDemoResult != null) {
            Text(text = state.linkDemoResult)
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (state.riskyContacts.isEmpty()) {
            Text("No events captured yet.")
        } else {
            Text("Top risky contacts:")
            LazyColumn {
                items(state.riskyContacts) { contact ->
                    Text(
                        text = "${contact.phoneNumber} | ${contact.lastEventType} | score=${"%.2f".format(contact.riskScore)}"
                    )
                }
            }
        }

        if (state.showAiDebug) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "AI Debug",
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = "Model loaded: ${if (state.isAiModelLoaded) "Yes" else "No"}")
            val scoreText = if (state.lastModelScores.isEmpty()) {
                "No model scores yet."
            } else {
                state.lastModelScores.joinToString(", ") { "%.2f".format(it) }
            }
            Text(text = "Recent AI scores: $scoreText")
        }
    }
}


