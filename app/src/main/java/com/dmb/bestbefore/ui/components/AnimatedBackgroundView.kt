package com.dmb.bestbefore.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedBackgroundView(
    modifier: Modifier = Modifier,
    baseColor: Color? = null // Allow passing a selected color for the room
) {
    var orb1Angle by remember { mutableFloatStateOf(0f) }
    var orb2Angle by remember { mutableFloatStateOf(180f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRatio by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAnimation"
    )

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(20)
            orb1Angle += 1.0f
            orb2Angle += 0.8f
            if (orb1Angle >= 360f) orb1Angle -= 360f
            if (orb2Angle >= 360f) orb2Angle -= 360f
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2, height / 2)
            val minDimension = minOf(width, height)
            val maxDimension = maxOf(width, height)

            // Main center glow
            val centerColor1 = baseColor?.copy(alpha = 0.4f) ?: Color(0.05f, 0.35f, 0.95f, 0.4f)
            val centerColor2 = baseColor?.copy(alpha = 0.2f) ?: Color(0.0f, 0.85f, 0.45f, 0.2f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(centerColor1, centerColor2, Color.Transparent),
                    center = center,
                    radius = (minDimension * 0.5f) * pulseRatio
                ),
                radius = (minDimension * 0.5f) * pulseRatio,
                center = center
            )

            // Floating orb 1
            val orb1RadiusX = width * 0.4f // ~180
            val orb1RadiusY = height * 0.3f
            val orb1Size = 70f.dp.toPx()
            val orb1OffsetX = center.x + orb1RadiusX * cos(Math.toRadians(orb1Angle.toDouble())).toFloat()
            val orb1OffsetY = center.y + orb1RadiusY * sin(Math.toRadians(orb1Angle.toDouble())).toFloat()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0.95f, 0.14f, 0.91f, 0.5f),
                        Color(1.0f, 0.6f, 0.2f, 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(orb1OffsetX, orb1OffsetY),
                    radius = orb1Size
                ),
                radius = orb1Size,
                center = Offset(orb1OffsetX, orb1OffsetY)
            )

            // Floating orb 2
            val orb2RadiusX = width * 0.45f // ~200
            val orb2RadiusY = height * 0.35f
            val orb2Size = 90f.dp.toPx()
            val orb2OffsetX = center.x + orb2RadiusX * cos(Math.toRadians(orb2Angle.toDouble())).toFloat()
            val orb2OffsetY = center.y + orb2RadiusY * sin(Math.toRadians(orb2Angle.toDouble())).toFloat()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0.3f, 0.95f, 0.95f, 0.5f),
                        Color(0.9f, 0.3f, 0.95f, 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(orb2OffsetX, orb2OffsetY),
                    radius = orb2Size
                ),
                radius = orb2Size,
                center = Offset(orb2OffsetX, orb2OffsetY)
            )

            // Subtle vignette overlay
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.2f),
                        Color.Black.copy(alpha = 0.4f)
                    ),
                    center = center,
                    radius = maxDimension * 0.6f
                ),
                radius = maxDimension * 0.6f,
                center = center
            )
        }
    }
}
