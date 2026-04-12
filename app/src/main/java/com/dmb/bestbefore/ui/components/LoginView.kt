package com.dmb.bestbefore.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

enum class LoginMode {
    EVERYONE, ARTISTS
}

enum class Direction {
    LEFT, RIGHT
}

@Composable
fun LoginView(
    onLoginSuccess: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLoading by remember { mutableStateOf(false) } // Static mock state
    val errorMessage by remember { mutableStateOf<String?>(null) } // Static mock state
    
    var loginMode by remember { mutableStateOf(LoginMode.EVERYONE) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragOffset < -50) {
                            loginMode = LoginMode.ARTISTS
                        } else if (dragOffset > 50) {
                            loginMode = LoginMode.EVERYONE
                        }
                        dragOffset = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        dragOffset += dragAmount
                        change.consume()
                    }
                )
            }
    ) {
        // Re-using the background view we wrote previously
        // SwiftUI logic uses the "artist" keyword which defaults/maps. I map to Cyberpunk for similar effect.
        AnimatedBackgroundView(theme = if (loginMode == LoginMode.ARTISTS) "cyberpunk" else "default")

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Content Group (Title + Form)
            Column(
                verticalArrangement = Arrangement.spacedBy(60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Logo Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "BestBefore",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    AnimatedVisibility(
                        visible = loginMode == LoginMode.ARTISTS,
                        enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut()
                    ) {
                        Text(
                            text = "for Artists",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                // Form Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(horizontal = 40.dp)
                ) {
                    OutlinedInput(
                        placeholder = "email or nickname",
                        text = email,
                        onValueChange = { email = it }
                    )
                    OutlinedInput(
                        placeholder = "password",
                        text = password,
                        onValueChange = { password = it },
                        isSecure = true
                    )

                    Box(
                        modifier = Modifier
                            .padding(top = 20.dp)
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(Color.White, RoundedCornerShape(28.dp))
                            .clickable { /* performLogin mock */ onLoginSuccess() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Login",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Swipe Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 20.dp)
                    .alpha(0.6f)
            ) {
                SequentialArrows(direction = Direction.LEFT)
                Text(
                    text = if (loginMode == LoginMode.EVERYONE) "swipe for Artists" else "swipe for Everyone",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                SequentialArrows(direction = Direction.RIGHT)
            }

            // Bottom Links
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 60.dp)
            ) {
                Text(
                    text = "forgot my password",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable { /* Forgotten logic empty */ }
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { /* Show Signup Sheet empty */ }
                ) {
                    Text("create", color = Color.White, fontSize = 16.sp)
                    Text("an", color = Color.White, fontSize = 16.sp)
                    
                    AnimatedVisibility(
                        visible = loginMode == LoginMode.ARTISTS,
                        enter = scaleIn(initialScale = 0.5f) + fadeIn() + slideInVertically(initialOffsetY = { -10 }),
                        exit = scaleOut(targetScale = 0.5f) + fadeOut()
                    ) {
                        Text("Artists", color = Color.White, fontSize = 16.sp)
                    }
                    
                    Text("account", color = Color.White, fontSize = 16.sp)
                }
            }
        }

        // Overlay Spinner
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.scale(1.5f)
                )
            }
        }
    }
}

@Composable
fun OutlinedInput(
    placeholder: String,
    text: String,
    onValueChange: (String) -> Unit,
    isSecure: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(1.dp, Color.White, RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (text.isEmpty()) {
            Text(
                text = placeholder,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 16.sp
            )
        }

        BasicTextField(
            value = text,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            visualTransformation = if (isSecure) PasswordVisualTransformation() else VisualTransformation.None,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// MARK: - Animated Swipe Indicator
@Composable
fun SequentialArrows(direction: Direction) {
    var activeIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(400) // timer mapping "every: 0.4"
            activeIndex = (activeIndex + 1) % 3
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (direction == Direction.RIGHT) {
            ArrowText(0, activeIndex, direction)
            ArrowText(1, activeIndex, direction)
        } else {
            ArrowText(1, activeIndex, direction)
            ArrowText(0, activeIndex, direction)
        }
    }
}

@Composable
private fun ArrowText(index: Int, activeIndex: Int, direction: Direction) {
    val opacity by animateFloatAsState(
        targetValue = if (activeIndex == index) 1.0f else 0.3f,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing),
        label = "arrowAlpha"
    )

    Text(
        text = if (direction == Direction.LEFT) "<" else ">",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.alpha(opacity)
    )
}

@Preview(showBackground = true)
@Composable
fun LoginViewPreview() {
    LoginView()
}
