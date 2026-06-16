package com.security.rakshakx

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.security.rakshakx.onboarding.OnboardingActivity
import com.security.rakshakx.permissions.PermissionManager
import com.security.rakshakx.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RakshakXTheme {
                SplashScreen {
                    val readiness = PermissionManager.getReadinessState(this@SplashActivity)
                    val target = if (readiness.minimumDashboardReady)
                        MainActivity::class.java
                    else
                        OnboardingActivity::class.java
                    startActivity(Intent(this@SplashActivity, target))
                    finish()
                    @Suppress("DEPRECATION")
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
            }
        }
    }
}

@Composable
private fun SplashScreen(onFinished: () -> Unit) {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current

    val logoBitmap = remember {
        context.assets.open("RXlogo.png").use { BitmapFactory.decodeStream(it) }
    }

    val logoRotation = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.3f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    val glowAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(600, easing = EaseOutCubic)) }
        launch { logoScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 300f)) }
        launch { logoRotation.animateTo(360f, tween(2000, easing = EaseOutCubic)) }
        launch {
            delay(300)
            glowAlpha.animateTo(0.6f, tween(500, easing = EaseOutCubic))
        }

        delay(500)
        textAlpha.animateTo(1f, tween(500, easing = EaseOutCubic))
        delay(150)
        taglineAlpha.animateTo(1f, tween(400, easing = EaseOutCubic))
        delay(700)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        NavyDeep.copy(alpha = 0.3f),
                        Charcoal
                    ),
                    radius = 600f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Glow ring behind logo
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .alpha(glowAlpha.value)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    RoyalBlue.copy(alpha = 0.15f),
                                    RoyalBlue.copy(alpha = 0f)
                                )
                            )
                        )
                )

                Image(
                    bitmap = logoBitmap.asImageBitmap(),
                    contentDescription = "RakshakX",
                    modifier = Modifier
                        .size(140.dp)
                        .alpha(logoAlpha.value)
                        .scale(logoScale.value)
                        .rotate(logoRotation.value)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "RakshakX",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Intelligent Protection",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
                modifier = Modifier.alpha(taglineAlpha.value)
            )
        }
    }
}
