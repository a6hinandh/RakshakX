package com.rakshakx.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rakshakx.core.orchestrator.Orchestrator
import com.rakshakx.ui.screens.DashboardScreen
import com.rakshakx.ui.state.DashboardState
import kotlinx.coroutines.launch

@Composable
fun RakshakXApp(
    orchestrator: Orchestrator,
    smsPermissionGranted: Boolean,
    callLogPermissionGranted: Boolean,
    isDefaultSmsApp: Boolean,
    notificationAccessEnabled: Boolean,
    backgroundMonitoringEnabled: Boolean,
    onRequestPermissions: () -> Unit,
    onRequestDefaultSmsRole: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onToggleBackgroundMonitoring: (Boolean) -> Unit,
    onOpenCallAnalysis: () -> Unit,
    onDebugShowLastCall: () -> Unit,
    onStartHackathonMode: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val riskyContacts = orchestrator.observeTopRiskyContacts().collectAsState(initial = emptyList())
    val lastModelScores = orchestrator.observeRecentModelScores().collectAsState(initial = emptyList())
    val isAiModelLoaded = orchestrator.observeModelLoaded().collectAsState(initial = false)
    var smsDemoInput by rememberSaveable { mutableStateOf("") }
    var smsDemoResult by rememberSaveable { mutableStateOf<String?>(null) }
    var callDemoInput by rememberSaveable { mutableStateOf("") }
    var callDemoResult by rememberSaveable { mutableStateOf<String?>(null) }
    var linkDemoInput by rememberSaveable { mutableStateOf("") }
    var linkDemoResult by rememberSaveable { mutableStateOf<String?>(null) }

    val state = remember(
        riskyContacts.value,
        lastModelScores.value,
        isAiModelLoaded.value,
        smsPermissionGranted,
        callLogPermissionGranted,
        isDefaultSmsApp,
        notificationAccessEnabled,
        backgroundMonitoringEnabled,
        smsDemoInput,
        smsDemoResult,
        callDemoInput,
        callDemoResult,
        linkDemoInput,
        linkDemoResult
    ) {
        val latestRisk = riskyContacts.value.firstOrNull()?.riskScore ?: 0f
        DashboardState(
            isMonitoring = true,
            latestRiskScore = latestRisk,
            statusText = "Local pipeline active",
            riskyContacts = riskyContacts.value,
            smsPermissionGranted = smsPermissionGranted,
            callLogPermissionGranted = callLogPermissionGranted,
            isDefaultSmsApp = isDefaultSmsApp,
            notificationAccessEnabled = notificationAccessEnabled,
            backgroundMonitoringEnabled = backgroundMonitoringEnabled,
            smsDemoInput = smsDemoInput,
            smsDemoResult = smsDemoResult,
            callDemoInput = callDemoInput,
            callDemoResult = callDemoResult,
            linkDemoInput = linkDemoInput,
            linkDemoResult = linkDemoResult,
            showAiDebug = true,
            lastModelScores = lastModelScores.value,
            isAiModelLoaded = isAiModelLoaded.value
        )
    }
    DashboardScreen(
        state = state,
        onRequestPermissions = onRequestPermissions,
        onRequestDefaultSmsRole = onRequestDefaultSmsRole,
        onRequestNotificationAccess = onRequestNotificationAccess,
        onToggleBackgroundMonitoring = onToggleBackgroundMonitoring,
        onSmsInputChange = { smsDemoInput = it },
        onScanSms = {
            coroutineScope.launch {
                val result = orchestrator.scanSmsDemo(smsDemoInput)
                smsDemoResult = result.explanation
            }
        },
        onCallInputChange = { callDemoInput = it },
        onScanCall = {
            coroutineScope.launch {
                val result = orchestrator.scanCallDemo(callDemoInput)
                callDemoResult = result.explanation
            }
        },
        onLinkInputChange = { linkDemoInput = it },
        onScanLink = {
            coroutineScope.launch {
                val result = orchestrator.scanLinkDemo(linkDemoInput)
                linkDemoResult = result.explanation
            }
        },
        onOpenCallAnalysis = onOpenCallAnalysis,
        onDebugShowLastCall = onDebugShowLastCall,
        onStartHackathonMode = onStartHackathonMode
    )
}
