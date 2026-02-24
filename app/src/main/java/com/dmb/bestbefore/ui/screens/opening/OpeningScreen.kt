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
        // Shared Background
        IOSAnimatedBubbleBackgroundO()

        // Center Content (Matches LoginScreen Initial State)
        
        // 1. Sphere (Center)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(280.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF0080FF), // Blue
                            Color(0xFF00CC66)  // Green
                        )
                    )
                )
        )

        // 2. Title (Center - same as LoginScreen Initial)
        Text(
            text = "BestBefore",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )

        // 3. Instruction Text (Below Center - same as LoginScreen Initial)
        Text(
            text = "touch to explore your memory\nswipe for Artists",
            fontSize = 16.sp,
            color = Color.LightGray, // Matched LoginScreen
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 180.dp)
        )
    }
}

// Duplicated from LoginScreen.kt to ensure seamless transition
@Composable
fun IOSAnimatedBubbleBackgroundO() {
    val infiniteTransition = rememberInfiniteTransition(label = "bubble_bg_o")

    val orb1Angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb1"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
             animation = tween(durationMillis = 5000, easing = FastOutSlowInEasing),
             repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val dimension = size.minDimension

        // Background dark gradient
        drawRect(
            brush = Brush.verticalGradient(
               colors = listOf(Color(0xFF051120), Color.Black)
            )
        )

        val color1 = Color(0xFF0D59F2) // Blue
        val color2 = Color(0xFF00D972) // Green
        
        val shiftX = 100f * cos(orb1Angle * Math.PI / 180).toFloat()
        val shiftY = 100f * sin(orb1Angle * Math.PI / 180).toFloat()
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color1.copy(alpha=0.6f), Color.Transparent),
                center = Offset(centerX + shiftX, centerY + shiftY),
                radius = dimension * 0.8f * pulse
            ),
            center = Offset(centerX + shiftX, centerY + shiftY),
            radius = dimension * 0.8f * pulse
        )
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color2.copy(alpha=0.5f), Color.Transparent),
                center = Offset(centerX - shiftX, centerY - shiftY),
                radius = dimension * 0.7f * pulse
            ),
             center = Offset(centerX - shiftX, centerY - shiftY),
             radius = dimension * 0.7f * pulse
        )

        // Vignette
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.4f),
                    Color.Black.copy(alpha = 0.8f)
                ),
                center = Offset(centerX, centerY),
                radius = size.maxDimension * 0.8f
            )
        )
        
        val bubbleColors = listOf(
             Color(0xFFF223E8), // Pink
             Color(0xFFFF9933), // Orange
             Color(0xFF4DF2F2), // Cyan
             Color(0xFFE64DF2)  // Purple
        )
        
        val timeRatio = (orb1Angle % 360) / 360f
        
        drawCircle(
            brush = Brush.radialGradient(colors = listOf(bubbleColors[0].copy(alpha=0.4f), Color.Transparent)),
            center = Offset(size.width * 0.2f, size.height * 0.2f + (50 * sin(timeRatio * 6.28)).toFloat()),
            radius = 60f
        )
         drawCircle(
             brush = Brush.radialGradient(colors = listOf(bubbleColors[1].copy(alpha=0.4f), Color.Transparent)),
             center = Offset(size.width * 0.8f, size.height * 0.8f - (40 * cos(timeRatio * 6.28)).toFloat()),
             radius = 50f
        )
        drawCircle(
             brush = Brush.radialGradient(colors = listOf(bubbleColors[2].copy(alpha=0.4f), Color.Transparent)),
             center = Offset(size.width * 0.1f, size.height * 0.8f + (30 * sin(timeRatio * 6.28)).toFloat()),
             radius = 40f
        )
    }
}
