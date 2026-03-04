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
        com.dmb.bestbefore.ui.components.AnimatedBackgroundView()

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


