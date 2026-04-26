package com.dmb.bestbefore.ui.components

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Reduced counts to cut per-frame draw-call cost for artist theme.
private data class StarObject(val x: Float, val y: Float, val size: Float, val phaseOffset: Float)
private data class GalaxyObject(val size: Float, val radius: Float, val speed: Float, val initialAngle: Float, val opacity: Float)

@Composable
fun AnimatedBackgroundView(theme: String = "default") {
    val isArtistTheme = theme.lowercase() == "artist"
    val bgColor = if (isArtistTheme) Color(red = 0.02f, green = 0.01f, blue = 0.07f) else Color.Black

    // ── ALL animation values driven by frame-time math — zero InfiniteTransition ──
    // Root cause of setRequestedFrameRate(NaN): ANY tween() inside InfiniteTransition
    // returns Float.NaN for targetBasedFrameRate, which propagates to the whole view.
    // Fix: drive every value from a single withInfiniteAnimationFrameMillis loop so the
    // Compose animation system has no tween specs to query → reports frameRate=0.0 (device
    // default) instead of NaN, and the GPU is no longer held at max rate continuously.
    var orbAngle  by remember { mutableFloatStateOf(0f) }
    var pulseScale by remember { mutableFloatStateOf(1f) }
    var globalTwinkle by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isArtistTheme) {
        while (isActive) {
            withInfiniteAnimationFrameMillis { ms ->
                // Orbital rotation: one full revolution per 7200 ms
                orbAngle = ((ms % 7200L).toFloat() / 7200f) * 360f

                // Pulse glow: smooth sine wave, ±5 % scale, period 4000 ms
                pulseScale = 1f + sin((ms % 4000L).toDouble() / 4000.0 * 2.0 * Math.PI).toFloat() * 0.05f

                // Twinkling (artist theme only): 0 → 1 sine, period 2000 ms
                if (isArtistTheme) {
                    globalTwinkle = (sin((ms % 2000L).toDouble() / 2000.0 * 2.0 * Math.PI).toFloat() + 1f) * 0.5f
                }
            }
        }
    }

    // ── Pre-compute immutable data so it is never re-allocated ────────────────
    val centerGlowColors = remember(theme) {
        when (theme.lowercase()) {
            "ocean"    -> listOf(Color(0xFF00C6A2).copy(0.5f), Color(0xFF00C6A2).copy(0.2f), Color.Transparent)
            "sunset"   -> listOf(Color(0xFFE8820C).copy(0.5f), Color(0xFFE8820C).copy(0.2f), Color.Transparent)
            "forest"   -> listOf(Color(0xFF22A84A).copy(0.5f), Color(0xFF22A84A).copy(0.2f), Color.Transparent)
            "cyberpunk"-> listOf(Color(0xFFAA3FD6).copy(0.5f), Color(0xFFAA3FD6).copy(0.2f), Color.Transparent)
            "artist", "vibrant" -> listOf(Color(0.9f, 0.1f, 0.5f, 0.5f), Color(0.5f, 0.1f, 0.9f, 0.2f), Color.Transparent)
            "midnight" -> listOf(Color(0.8f, 0.0f, 0.8f, 0.4f), Color(0.0f, 0.8f, 0.9f, 0.2f), Color.Transparent)
            "glass"    -> listOf(Color.White.copy(0.15f), Color.White.copy(0.05f), Color.Transparent)
            else       -> listOf(Color(0xFF1A7AF8).copy(0.5f), Color(0xFF1A7AF8).copy(0.2f), Color.Transparent)
        }
    }
    val orb1Colors = remember(isArtistTheme) {
        if (isArtistTheme) listOf(Color(0.5f, 0.2f, 1.0f), Color(0.2f, 0.8f, 1.0f))
        else listOf(Color(0.95f, 0.14f, 0.91f), Color(1.0f, 0.6f, 0.2f))
    }
    val orb2Colors = remember(isArtistTheme) {
        if (isArtistTheme) listOf(Color(0.8f, 0.1f, 1.0f), Color(0.0f, 0.4f, 1.0f))
        else listOf(Color(0.3f, 0.95f, 0.95f), Color(0.9f, 0.3f, 0.95f))
    }

    // Artist-specific static data (reduced counts: 30 stars, 8 orbs)
    val galaxyStars = remember {
        List(30) { StarObject(
            x = Random.nextFloat() * 2f - 0.5f,
            y = Random.nextFloat() * 2f - 0.5f,
            size = Random.nextFloat() * 2f + 1f,
            phaseOffset = Random.nextFloat()
        )}
    }
    val galaxyOrbs = remember {
        List(8) { GalaxyObject(
            size = Random.nextFloat() * 80f + 40f,
            radius = Random.nextFloat() * 300f + 100f,
            speed = Random.nextFloat() * 1.0f + 0.2f,
            initialAngle = Random.nextFloat() * 360f,
            opacity = Random.nextFloat() * 0.3f + 0.1f
        )}
    }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {

        // ── LAYER 1: Static — only redraws when screen size changes ──────────
        // No animation state is read here → Canvas.draw() runs once, not every frame.
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Vignette
            val maxR = maxOf(size.width, size.height) * 0.6f
            if (maxR > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.Transparent, Color.Black.copy(0.2f), Color.Black.copy(0.4f)),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = maxR
                    ),
                    radius = maxR,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
            }
        }

        // ── LAYER 2: Center glow — GPU scale, Canvas content is static ───────
        // pulseScale changes drive only the graphicsLayer transform on GPU;
        // the Canvas draw lambda contains no animation state → no CPU redraw.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                    transformOrigin = TransformOrigin.Center
                }
        ) {
            val endRadius = minOf(size.width, size.height) * 0.5f
            if (endRadius > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        centerGlowColors,
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = endRadius
                    ),
                    radius = endRadius,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
            }
        }

        // ── LAYER 3: Orb 1 — GPU rotation, Canvas content is static ─────────
        // orbAngle changes rotate the hardware layer on GPU; Canvas.draw() is
        // only invoked on first composition or screen-size change.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = orbAngle
                    transformOrigin = TransformOrigin.Center
                }
        ) {
            val cx = size.width / 2f + 180f
            val cy = size.height / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(orb1Colors[0].copy(0.5f), orb1Colors[1].copy(0.2f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = 70f
                ),
                radius = 70f,
                center = Offset(cx, cy)
            )
        }

        // ── LAYER 4: Orb 2 — GPU rotation at 0.8× speed, 180° offset ────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = orbAngle * 0.8f + 180f
                    transformOrigin = TransformOrigin.Center
                }
        ) {
            val cx = size.width / 2f + 200f
            val cy = size.height / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(orb2Colors[0].copy(0.5f), orb2Colors[1].copy(0.2f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = 90f
                ),
                radius = 90f,
                center = Offset(cx, cy)
            )
        }

        // ── LAYER 5: Artist-specific galaxy (only for artist theme) ──────────
        // Fewer objects + graphicsLayer hardware layer promotion.
        if (isArtistTheme) {
            val twinkle = globalTwinkle
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer()  // promotes to hardware layer
            ) {
                // Stars with pre-computed positions
                galaxyStars.forEach { star ->
                    val phase = (twinkle + star.phaseOffset) % 1.0f
                    val alpha = (sin(phase * Math.PI) * 0.5 + 0.3).toFloat()
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = star.size,
                        center = Offset(size.width * star.x, size.height * star.y)
                    )
                }
                // Galaxy orbs driven by orbAngle
                galaxyOrbs.forEach { orb ->
                    val angle = (orb.initialAngle + orbAngle * orb.speed) * Math.PI / 180.0
                    val ox = size.width / 2f + orb.radius * cos(angle).toFloat()
                    val oy = size.height / 2f + orb.radius * sin(angle).toFloat()
                    if (orb.size > 0f) {
                        val c = when ((galaxyOrbs.indexOf(orb)) % 3) {
                            0 -> listOf(Color(0.4f, 0.2f, 0.8f).copy(orb.opacity), Color.Transparent)
                            1 -> listOf(Color(0.6f, 0.1f, 0.9f).copy(orb.opacity), Color.Transparent)
                            else -> listOf(Color(0.1f, 0.4f, 1.0f).copy(orb.opacity), Color.Transparent)
                        }
                        drawCircle(
                            brush = Brush.radialGradient(c, Offset(ox, oy), orb.size),
                            radius = orb.size,
                            center = Offset(ox, oy)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AnimatedBackgroundPreview() { AnimatedBackgroundView(theme = "default") }

@Preview
@Composable
fun AnimatedBackgroundPreviewArtist() { AnimatedBackgroundView(theme = "artist") }
