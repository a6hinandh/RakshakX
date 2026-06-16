package com.security.rakshakx.onboarding

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.security.rakshakx.MainActivity
import com.security.rakshakx.permissions.PermissionManager
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.theme.*
import kotlinx.coroutines.launch

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
    ) { refreshReadiness() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshReadiness()
        setContent {
            RakshakXTheme {
                OnboardingScreen(
                    readinessState = readinessState,
                    missingRequirements = PermissionManager.getMissingOnboardingRequirements(this),
                    onComplete = {
                        if (!readinessState.minimumDashboardReady) return@OnboardingScreen
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onRequestCorePermissions = { permissionLauncher.launch(PermissionManager.corePermissionsForRuntimeRequest()) },
                    onRequestAccessibility = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    onRequestNotificationAccess = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                    onRequestOverlay = { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }
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

private data class OnboardingStep(
    val title: String,
    val headline: String,
    val body: String,
    val icon: ImageVector,
    val accentColor: Color,
    val secondaryIcon: ImageVector? = null,
    val isWelcomePage: Boolean = false,
    val isPermissionStep: Boolean = false,
    val isGranted: () -> Boolean = { false },
    val onGrant: () -> Unit = {}
)

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
    val colors = LocalRakshakXColors.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = rememberHaptics()

    val steps = listOf(
        OnboardingStep(
            title = "WELCOME",
            headline = "Intelligent protection,\nprivate by design",
            body = "RakshakX detects scams across calls, SMS, email, and web — using AI that runs entirely on your device. Nothing leaves your phone.",
            icon = Icons.Filled.Shield,
            accentColor = RoyalBlue,
            isWelcomePage = true
        ),
        OnboardingStep(
            title = "ON-DEVICE AI",
            headline = "Your data never\nleaves your device",
            body = "RakshakX uses DistilBERT, IndicBERT, and Vosk — advanced neural networks that run locally. No cloud servers, no data collection, no subscriptions.",
            icon = Icons.Filled.Psychology,
            secondaryIcon = Icons.Filled.PhoneAndroid,
            accentColor = Amethyst
        ),
        OnboardingStep(
            title = "CORE PROTECTION",
            headline = "SMS & Call\nprotection",
            body = "RakshakX needs access to SMS and microphone to detect fraud patterns in real time. These signals are analyzed locally and never recorded.",
            icon = Icons.Filled.Sms,
            secondaryIcon = Icons.Filled.Call,
            accentColor = RoyalBlue,
            isPermissionStep = true,
            isGranted = { readinessState.corePermissionsGranted },
            onGrant = onRequestCorePermissions
        ),
        OnboardingStep(
            title = "NOTIFICATION GUARD",
            headline = "Monitor email &\napp notifications",
            body = "By reading notifications, RakshakX can analyze phishing attempts from email apps and messaging services — without accessing your actual accounts.",
            icon = Icons.Filled.NotificationsActive,
            secondaryIcon = Icons.Filled.Email,
            accentColor = Amber,
            isPermissionStep = true,
            isGranted = { readinessState.notificationListenerEnabled },
            onGrant = onRequestNotificationAccess
        ),
        OnboardingStep(
            title = "WEB PROTECTION",
            headline = "Block phishing\nURLs in your browser",
            body = "The accessibility service allows RakshakX to read URLs as you browse and warn you before you visit a known phishing site. No page content is stored.",
            icon = Icons.Filled.Language,
            secondaryIcon = Icons.Filled.GppGood,
            accentColor = Emerald,
            isPermissionStep = true,
            isGranted = { readinessState.accessibilityEnabled },
            onGrant = onRequestAccessibility
        ),
        OnboardingStep(
            title = "SMART OVERLAY",
            headline = "Real-time alerts\nduring calls",
            body = "Draw-over-apps lets RakshakX display a discreet overlay during suspicious calls — so you see warnings without leaving the call screen.",
            icon = Icons.Filled.Layers,
            secondaryIcon = Icons.Filled.ScreenShare,
            accentColor = Crimson,
            isPermissionStep = true,
            isGranted = { readinessState.overlayEnabled },
            onGrant = onRequestOverlay
        ),
    )

    val pagerState = rememberPagerState(pageCount = { steps.size })
    val currentStep = pagerState.currentPage
    val isLastStep = currentStep == steps.lastIndex

    // Haptic on page change
    var previousPage by remember { mutableIntStateOf(0) }
    LaunchedEffect(currentStep) {
        if (currentStep != previousPage) {
            haptics.tick()
            previousPage = currentStep
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Charcoal)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Top bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = steps[currentStep].title,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    label = "stepLabel"
                ) { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "${currentStep + 1} / ${steps.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }

            // ── Progress bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                steps.forEachIndexed { index, _ ->
                    val targetFraction = if (index <= currentStep) 1f else 0f
                    val animatedFraction by animateFloatAsState(
                        targetValue = targetFraction,
                        animationSpec = tween(400, easing = EaseOutCubic),
                        label = "progress_$index"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.border.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedFraction)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            steps[currentStep].accentColor,
                                            steps[currentStep].accentColor.copy(alpha = 0.7f)
                                        )
                                    )
                                )
                        )
                    }
                }
            }

            // ── Pager ──
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = true
            ) { page ->
                OnboardingPage(
                    step = steps[page],
                    isCurrentPage = page == currentStep
                )
            }

            // ── Bottom actions ──
            Column(
                modifier = Modifier.padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val step = steps[currentStep]

                // Permission grant button
                if (step.isPermissionStep && !step.isGranted()) {
                    OutlinedButton(
                        onClick = {
                            haptics.click()
                            step.onGrant()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = step.accentColor),
                        border = ButtonDefaults.outlinedButtonBorder(true).copy(
                            brush = androidx.compose.ui.graphics.SolidColor(step.accentColor.copy(alpha = 0.4f))
                        )
                    ) {
                        Icon(Icons.Filled.Lock, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Grant Access", fontWeight = FontWeight.SemiBold)
                    }
                }

                // Permission granted badge
                if (step.isPermissionStep && step.isGranted()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.safeBg)
                            .border(1.dp, colors.safe.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = colors.safe, modifier = Modifier.size(18.dp))
                        Text("Permission granted", style = MaterialTheme.typography.labelMedium, color = colors.safe, fontWeight = FontWeight.Medium)
                    }
                }

                // Primary CTA
                if (isLastStep) {
                    Button(
                        onClick = {
                            haptics.success()
                            onComplete()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = readinessState.minimumDashboardReady,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Emerald,
                            disabledContainerColor = colors.surfaceElevated
                        )
                    ) {
                        Text(
                            if (readinessState.minimumDashboardReady) "Start Protection" else "Grant Required Permissions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        if (readinessState.minimumDashboardReady) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Filled.ArrowForward, null, modifier = Modifier.size(20.dp))
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            haptics.tick()
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(currentStep + 1)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = steps[currentStep].accentColor)
                    ) {
                        Text("Continue", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }
                }

                // Skip option
                if (step.isPermissionStep && !step.isGranted() && !isLastStep) {
                    TextButton(
                        onClick = {
                            haptics.tick()
                            coroutineScope.launch { pagerState.animateScrollToPage(currentStep + 1) }
                        }
                    ) {
                        Text("Skip for now", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                }

                // Privacy footer
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Lock, null, tint = colors.textMuted, modifier = Modifier.size(10.dp))
                    Text("Your data stays on your device", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    step: OnboardingStep,
    isCurrentPage: Boolean
) {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current

    val contentAlpha by animateFloatAsState(
        targetValue = if (isCurrentPage) 1f else 0.5f,
        animationSpec = tween(300),
        label = "contentAlpha"
    )
    val contentScale by animateFloatAsState(
        targetValue = if (isCurrentPage) 1f else 0.92f,
        animationSpec = tween(300),
        label = "contentScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .alpha(contentAlpha)
            .scale(contentScale),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        // ── Visual area ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            // Radial glow
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                step.accentColor.copy(alpha = 0.1f),
                                step.accentColor.copy(alpha = 0.03f),
                                Color.Transparent
                            ),
                            radius = 400f
                        )
                    )
            )

            if (step.isWelcomePage) {
                WelcomeVisual()
            } else {
                IconVisual(
                    icon = step.icon,
                    secondaryIcon = step.secondaryIcon,
                    accentColor = step.accentColor,
                    isPlaying = isCurrentPage
                )
            }
        }

        // ── Text ──
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(
                text = step.headline,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp
            )
            Text(
                text = step.body,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun WelcomeVisual() {
    val context = LocalContext.current
    val logoBitmap = remember {
        context.assets.open("RXlogo.png").use { BitmapFactory.decodeStream(it) }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "welcomeAnim")

    // Slow breathing scale on the logo
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    // Rotating outer ring
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing)
        ),
        label = "ringRotation"
    )

    // Pulsing glow
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(contentAlignment = Alignment.Center) {
        // Outer rotating dashed ring
        Box(
            modifier = Modifier
                .size(240.dp)
                .rotate(ringRotation)
                .border(
                    width = 1.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            RoyalBlue.copy(alpha = 0.4f),
                            Color.Transparent,
                            Emerald.copy(alpha = 0.3f),
                            Color.Transparent,
                            RoyalBlue.copy(alpha = 0.4f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Inner glow circle
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            RoyalBlue.copy(alpha = glowAlpha),
                            Color.Transparent
                        )
                    )
                )
        )

        // Logo
        Image(
            bitmap = logoBitmap.asImageBitmap(),
            contentDescription = "RakshakX Logo",
            modifier = Modifier
                .size(160.dp)
                .scale(breathScale)
        )
    }
}

@Composable
private fun IconVisual(
    icon: ImageVector,
    secondaryIcon: ImageVector?,
    accentColor: Color,
    isPlaying: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "iconAnim")

    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing)
        ),
        label = "iconRingRotation"
    )

    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "orbitAngle"
    )

    Box(contentAlignment = Alignment.Center) {
        // Outer rotating gradient ring
        Box(
            modifier = Modifier
                .size(220.dp)
                .rotate(ringRotation)
                .border(
                    width = 1.5.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            accentColor.copy(alpha = 0.5f),
                            Color.Transparent,
                            accentColor.copy(alpha = 0.2f),
                            Color.Transparent,
                            accentColor.copy(alpha = 0.5f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Inner circle background
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(iconScale)
                .background(
                    Brush.radialGradient(
                        listOf(
                            accentColor.copy(alpha = 0.12f),
                            accentColor.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
                .border(1.dp, accentColor.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(64.dp)
            )
        }

        // Secondary icon orbiting
        if (secondaryIcon != null) {
            val orbitRadius = 100.dp
            val radians = Math.toRadians(orbitAngle.toDouble())
            val offsetX = (orbitRadius.value * kotlin.math.cos(radians)).dp
            val offsetY = (orbitRadius.value * kotlin.math.sin(radians)).dp

            Box(
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .size(44.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                accentColor.copy(alpha = 0.2f),
                                accentColor.copy(alpha = 0.05f)
                            )
                        ),
                        CircleShape
                    )
                    .border(1.dp, accentColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = secondaryIcon,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp)
                )
            }
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
    val colors = LocalRakshakXColors.current
    val haptics = rememberHaptics()
    Surface(
        onClick = {
            haptics.click()
            onClick()
        },
        shape = RoundedCornerShape(14.dp),
        color = if (granted) colors.cardBackground else colors.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (granted) colors.primary.copy(alpha = 0.18f) else colors.border.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (granted) colors.primaryMuted else colors.surfaceElevated,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = if (granted) colors.primary else colors.textMuted, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
            if (granted) {
                Icon(Icons.Default.CheckCircle, "Granted", tint = colors.safe, modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.Default.ChevronRight, "Action Required", tint = colors.textMuted, modifier = Modifier.size(20.dp))
            }
        }
    }
}
