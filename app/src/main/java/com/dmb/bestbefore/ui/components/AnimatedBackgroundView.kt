package com.dmb.bestbefore.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class GalaxyObject(val id: Int, val size: Float, val radius: Float, val speed: Double, val initialAngle: Double, val opacity: Float)
data class StarObject(val id: Int, val x: Float, val y: Float, val size: Float, val twinkleSpeed: Int)

@Composable
fun AnimatedBackgroundView(theme: String = "default") {
    val isArtistTheme = theme.lowercase() == "artist"
    val bgColor = if (isArtistTheme) Color(red = 0.02f, green = 0.01f, blue = 0.07f) else Color.Black

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val orbAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbAngle"
    )

    val galaxyOrbs = remember {
        List(20) { i ->
            GalaxyObject(
                id = i,
                size = Random.nextFloat() * 80 + 40,
                radius = Random.nextFloat() * 300 + 100,
                speed = Random.nextDouble(0.2, 1.2),
                initialAngle = Random.nextDouble(0.0, 360.0),
                opacity = Random.nextFloat() * 0.3f + 0.1f
            )
        }
    }

    val galaxyStars = remember {
        List(60) { i ->
            StarObject(
                id = i,
                x = Random.nextFloat() * 2f - 0.5f,
                y = Random.nextFloat() * 2f - 0.5f,
                size = Random.nextFloat() * 2 + 1,
                twinkleSpeed = Random.nextInt(1000, 3000)
            )
        }
    }

    val centerGlowColors = when(theme.lowercase()) {
        "artist", "vibrant" -> listOf(Color(0.9f, 0.1f, 0.5f, 0.5f), Color(0.5f, 0.1f, 0.9f, 0.2f), Color.Transparent)
        "cyberpunk", "midnight" -> listOf(Color(0.8f, 0.0f, 0.8f, 0.4f), Color(0.0f, 0.8f, 0.9f, 0.2f), Color.Transparent)
        "glass" -> listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f), Color.Transparent)
        else -> listOf(Color(0.05f, 0.35f, 0.95f, 0.4f), Color(0.0f, 0.85f, 0.45f, 0.2f), Color.Transparent)
    }

    val orb1Colors = if (isArtistTheme) listOf(Color(0.5f, 0.2f, 1.0f), Color(0.2f, 0.8f, 1.0f)) else listOf(Color(0.95f, 0.14f, 0.91f), Color(1.0f, 0.6f, 0.2f))
    val orb2Colors = if (isArtistTheme) listOf(Color(0.8f, 0.1f, 1.0f), Color(0.0f, 0.4f, 1.0f)) else listOf(Color(0.3f, 0.95f, 0.95f), Color(0.9f, 0.3f, 0.95f))

    fun artistOrbColors(id: Int): List<Color> {
        return when (id % 3) {
            0 -> listOf(Color(0.4f, 0.2f, 0.8f), Color(0.2f, 0.6f, 1.0f))
            1 -> listOf(Color(0.6f, 0.1f, 0.9f), Color(0.9f, 0.0f, 0.6f))
            else -> listOf(Color(0.1f, 0.4f, 1.0f), Color(0.0f, 0.9f, 0.8f))
        }
    }

    // Custom variable for pulsing stars
    val timeMillis = remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { timeMillis.value = it }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(bgColor)) {
        val width = maxWidth.value
        val height = maxHeight.value
        val currentSize = androidx.compose.ui.geometry.Size(width, height)

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (isArtistTheme) {
                // Stars
                galaxyStars.forEach { star ->
                    val starTimeScale = (timeMillis.value % star.twinkleSpeed) / star.twinkleSpeed.toFloat()
                    val twinkleAlpha = (sin(starTimeScale * Math.PI) * 0.6 + 0.2).toFloat()
                    drawCircle(
                        color = Color.White.copy(alpha = twinkleAlpha),
                        radius = star.size,
                        center = Offset(size.width * star.x, size.height * star.y)
                    )
                }

                // Galaxy Orbs
                galaxyOrbs.forEach { orb ->
                    val angle = orb.initialAngle + (orbAngle * orb.speed)
                    val rad = angle * Math.PI / 180.0
                    val offsetX = size.width / 2f + orb.radius * cos(rad).toFloat()
                    val offsetY = size.height / 2f + orb.radius * sin(rad).toFloat()
                    
                    if (orb.size > 0) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = artistOrbColors(orb.id).map { it.copy(alpha = orb.opacity) } + Color.Transparent,
                                center = Offset(offsetX, offsetY),
                                radius = orb.size
                            ),
                            radius = orb.size,
                            center = Offset(offsetX, offsetY)
                        )
                    }
                }
            }

            // Center glow
            val endRadius = (minOf(size.width, size.height) * 0.5f) * pulseScale
            if (endRadius > 0) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = centerGlowColors,
                        center = Offset(size.width/2, size.height/2),
                        radius = endRadius
                    ),
                    radius = endRadius,
                    center = Offset(size.width/2, size.height/2)
                )
            }

            // Primary Orbs
            val o1Rad = orbAngle * Math.PI / 180.0
            val o1x = size.width/2 + 180 * cos(o1Rad).toFloat()
            val o1y = size.height/2 + 180 * sin(o1Rad).toFloat()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb1Colors[0].copy(0.5f), orb1Colors[1].copy(0.2f), Color.Transparent),
                    center = Offset(o1x, o1y),
                    radius = 70f
                ),
                radius = 70f,
                center = Offset(o1x, o1y)
            )

            val o2Rad = (180 + orbAngle * 0.8) * Math.PI / 180.0
            val o2x = size.width/2 + 200 * cos(o2Rad).toFloat()
            val o2y = size.height/2 + 200 * sin(o2Rad).toFloat()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb2Colors[0].copy(0.5f), orb2Colors[1].copy(0.2f), Color.Transparent),
                    center = Offset(o2x, o2y),
                    radius = 90f
                ),
                radius = 90f,
                center = Offset(o2x, o2y)
            )

            // Subtle vignette overlay
            val maxR = maxOf(size.width, size.height) * 0.6f
            if (maxR > 0) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(0.2f), Color.Black.copy(0.4f)),
                        center = Offset(size.width/2, size.height/2),
                        radius = maxR
                    ),
                    radius = maxR,
                    center = Offset(size.width/2, size.height/2)
                )
            }
        }
    }
}

@Preview
@Composable
fun AnimatedBackgroundPreviewArtist() {
    AnimatedBackgroundView(theme = "artist")
}
