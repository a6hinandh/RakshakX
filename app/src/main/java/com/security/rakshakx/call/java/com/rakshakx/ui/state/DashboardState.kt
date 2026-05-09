package com.rakshakx.ui.state

import com.rakshakx.core.storage.RiskScoreEntity

data class DashboardState(
    val isMonitoring: Boolean = false,
    val latestRiskScore: Float = 0f,
    val statusText: String = "Idle",
    val riskyContacts: List<RiskScoreEntity> = emptyList(),
    val smsPermissionGranted: Boolean = false,
    val callLogPermissionGranted: Boolean = false,
    val isDefaultSmsApp: Boolean = false,
    val notificationAccessEnabled: Boolean = false,
    val backgroundMonitoringEnabled: Boolean = false,
    val smsDemoInput: String = "",
    val smsDemoResult: String? = null,
    val callDemoInput: String = "",
    val callDemoResult: String? = null,
    val linkDemoInput: String = "",
    val linkDemoResult: String? = null,
    val showAiDebug: Boolean = true,
    val lastModelScores: List<Float> = emptyList(),
    val isAiModelLoaded: Boolean = false
)
