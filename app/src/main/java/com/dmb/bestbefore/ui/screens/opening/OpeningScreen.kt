package com.dmb.bestbefore.ui.screens.opening

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OpeningScreen(
    onNavigateToMain: () -> Unit
) {
    // Auto-navigate to login after 2.5 seconds
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500L)
        onNavigateToMain()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onNavigateToMain() } // Keep for instant skip
    ) {
        // Animated background
        AnimatedOrbBackground()

        // Center content
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main orb circle
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0D59F2),
                                Color(0xFF00D972)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Inner content can be added here
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "BestBefore",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "touch to explore your memory\nswipe for Artists",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun AnimatedOrbBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_animation")

    val orb1Angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb1"
    )

    val orb2Angle by infiniteTransition.animateFloat(
        initialValue = 180f,
        targetValue = 540f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb2"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        // Main glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF0D59F2).copy(alpha = 0.3f),
                    Color(0xFF00D972).copy(alpha = 0.1f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = size.minDimension * 0.5f * pulseScale
            ),
            radius = size.minDimension * 0.5f * pulseScale,
            center = Offset(centerX, centerY)
        )

        // Floating orb 1
        val orb1X = centerX + 180f * cos(orb1Angle * Math.PI / 180).toFloat()
        val orb1Y = centerY + 180f * sin(orb1Angle * Math.PI / 180).toFloat()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFF223E8).copy(alpha = 0.6f),
                    Color(0xFFFF9933).copy(alpha = 0.2f),
                    Color.Transparent
                )
            ),
            radius = 60f,
            center = Offset(orb1X, orb1Y)
        )

        // Floating orb 2
        val orb2X = centerX - 220f * cos(orb2Angle * Math.PI / 180).toFloat()
        val orb2Y = centerY + 140f * sin(orb2Angle * Math.PI / 180).toFloat()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF4DF2F2).copy(alpha = 0.5f),
                    Color(0xFFE64DF2).copy(alpha = 0.2f),
                    Color.Transparent
                )
            ),
            radius = 80f,
            center = Offset(orb2X, orb2Y)
        )
    }
}
