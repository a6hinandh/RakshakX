package com.security.rakshakx.ui.anim

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.security.rakshakx.ui.theme.LocalRakshakXColors
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════════
// Staggered Entry — items fade + slide in with staggered delays
// ═══════════════════════════════════════════════════════════════

@Composable
fun StaggeredEntry(
    index: Int,
    baseDelayMs: Int = 60,
    durationMs: Int = 350,
    offsetY: Int = 24,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay((index * baseDelayMs).toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(durationMs, easing = EaseOutCubic)) +
                slideIn(tween(durationMs, easing = EaseOutCubic)) { IntOffset(0, offsetY) }
    ) {
        content()
    }
}

// ═══════════════════════════════════════════════════════════════
// Shimmer Placeholder — loading skeleton
// ═══════════════════════════════════════════════════════════════

@Composable
fun ShimmerPlaceholder(
    modifier: Modifier = Modifier,
    height: Dp = 80.dp
) {
    val colors = LocalRakshakXColors.current
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val translateX by shimmerTransition.animateFloat(
        initialValue = -300f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            colors.cardBackground,
            colors.surfaceElevated.copy(alpha = 0.6f),
            colors.cardBackground
        ),
        start = Offset(translateX, 0f),
        end = Offset(translateX + 300f, 0f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(shimmerBrush)
    )
}

// ═══════════════════════════════════════════════════════════════
// Animated Counter — counts from 0 to target
// ═══════════════════════════════════════════════════════════════

@Composable
fun animateIntCounter(target: Int, durationMs: Int = 800): Int {
    val animatedValue by animateIntAsState(
        targetValue = target,
        animationSpec = tween(durationMs, easing = EaseOutCubic),
        label = "counter"
    )
    return animatedValue
}

// ═══════════════════════════════════════════════════════════════
// Breathing Dot — subtle scale pulse for "alive" signals
// ═══════════════════════════════════════════════════════════════

@Composable
fun breathingScale(durationMs: Int = 2000): Float {
    val transition = rememberInfiniteTransition(label = "breathing")
    val scale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )
    return scale
}

// ═══════════════════════════════════════════════════════════════
// Progress Bar Wipe — animates segment fill
// ═══════════════════════════════════════════════════════════════

@Composable
fun animateProgressFraction(
    targetFraction: Float,
    durationMs: Int = 400,
    delayMs: Int = 0
): Float {
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMs, delayMillis = delayMs, easing = EaseOutCubic),
        label = "progressFill"
    )
    return animatedFraction
}
