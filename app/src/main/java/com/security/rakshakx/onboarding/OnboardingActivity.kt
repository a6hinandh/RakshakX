package com.security.rakshakx.onboarding

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.security.rakshakx.MainActivity
import com.security.rakshakx.permissions.PermissionManager
import android.net.Uri

class OnboardingActivity : ComponentActivity() {
    private var readinessState by mutableStateOf(
        PermissionManager.ReadinessState(
            corePermissionsGranted = false,
            notificationListenerEnabled = false,
            accessibilityEnabled = false,
            overlayEnabled = false,
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
            RakshakTheme {
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
                    },
                    onRequestOverlay = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                    }
                )
            }
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
fun RakshakTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF4776E6),
            secondary = Color(0xFF8E54E9),
            background = Color(0xFF0F172A),
            surface = Color(0xFF1E293B)
        ),
        typography = Typography(
            headlineLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                letterSpacing = (-0.5).sp
            ),
            titleMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            ),
            bodyMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        ),
        content = content
    )
}

@Composable
fun OnboardingScreen(
    readinessState: PermissionManager.ReadinessState,
    missingRequirements: List<String>,
    onComplete: () -> Unit,
    onRequestCorePermissions: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onRequestOverlay: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Spacer(Modifier.height(40.dp))
            
            Text(
                "Welcome to RakshakX",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
            
            Text(
                "Privacy-first autonomous fraud interception. Secure your digital life with real-time AI protection.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(40.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    PermissionItem(
                        title = "Core Protection",
                        description = "Required to detect scam messages and calls in real-time.",
                        icon = Icons.Default.Shield,
                        granted = readinessState.corePermissionsGranted,
                        onClick = onRequestCorePermissions
                    )
                }
                item {
                    PermissionItem(
                        title = "Web Protection",
                        description = "Accessibility service for browser-level threat detection.",
                        icon = Icons.Default.Public,
                        granted = readinessState.accessibilityEnabled,
                        onClick = onRequestAccessibility
                    )
                }
                item {
                    PermissionItem(
                        title = "Notification Guard",
                        description = "Scan app alerts and emails for sophisticated phishing.",
                        icon = Icons.Default.Notifications,
                        granted = readinessState.notificationListenerEnabled,
                        onClick = onRequestNotificationAccess
                    )
                }
                item {
                    PermissionItem(
                        title = "Smart Overlay",
                        description = "Allow RakshakX to display security alerts over other apps.",
                        icon = Icons.Default.Layers,
                        granted = readinessState.overlayEnabled,
                        onClick = onRequestOverlay
                    )
                }

                if (missingRequirements.isNotEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF334155).copy(alpha = 0.5f)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "Pending Requirements:",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                                Spacer(Modifier.height(8.dp))
                                missingRequirements.forEach { req ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = Color(0xFF94A3B8)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            req,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp)),
                enabled = readinessState.minimumDashboardReady,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4776E6),
                    disabledContainerColor = Color(0xFF334155)
                )
            ) {
                Text(
                    if (readinessState.minimumDashboardReady) {
                        "Get Started"
                    } else {
                        "Grant Access to Continue"
                    },
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                "RakshakX never uploads your private data to any server.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    icon: ImageVector,
    granted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (granted) Color(0xFF1E293B) else Color(0xFF334155).copy(alpha = 0.3f),
        border = if (granted) BorderStroke(1.dp, Color(0xFF4776E6).copy(alpha = 0.3f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (granted) Color(0xFF4776E6).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (granted) Color(0xFF4776E6) else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            if (granted) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Action Required",
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
