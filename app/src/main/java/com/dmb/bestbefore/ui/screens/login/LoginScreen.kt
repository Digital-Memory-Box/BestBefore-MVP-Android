package com.dmb.bestbefore.ui.screens.login

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmb.bestbefore.ui.components.AnimatedBackgroundView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// ─── Login Mode ─────────────────────────────────────────────────────────────
// (Removed redundant LoginMode enum - defined in LoginViewModel.kt)

private enum class ArrowDirection {
    LEFT, RIGHT
}

// ═══════════════════════════════════════════════════════════════════════════
// BB-UI-01 & BB-UI-02: LoginScreen
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var loginMode by remember { mutableStateOf(LoginMode.EVERYONE) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var arrowPhase by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(400L)
            arrowPhase = (arrowPhase + 1) % 3
        }
    }

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
        // ── Background ──────────────────────────────────────────────────
        // BB-UI-01: Simple orbs for Everyone
        // BB-UI-02: Complex blue/pink orbs for Artists
        AnimatedBackgroundView(
            theme = if (loginMode == LoginMode.ARTISTS) "artist" else "default"
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // ── Title + Form ────────────────────────────────────────────
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
                    // BB-UI-01: "BestBefore" centered
                    Text(
                        text = "BestBefore",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    // BB-UI-02: "for Artists" slides in below BestBefore
                    AnimatedVisibility(
                        visible = loginMode == LoginMode.ARTISTS,
                        enter = slideInVertically(
                            initialOffsetY = { -it },
                            animationSpec = tween(400, easing = EaseOutCubic)
                        ) + fadeIn(animationSpec = tween(400)),
                        exit = slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = tween(300, easing = EaseInCubic)
                        ) + fadeOut(animationSpec = tween(300))
                    ) {
                        Text(
                            text = "for Artist",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Form Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(horizontal = 40.dp)
                ) {
                    BBOutlinedInput(
                        placeholder = "email or nickname",
                        text = email,
                        onValueChange = { email = it }
                    )
                    BBOutlinedInput(
                        placeholder = "password",
                        text = password,
                        onValueChange = { password = it },
                        isSecure = true
                    )

                    // Login Button
                    Box(
                        modifier = Modifier
                            .padding(top = 20.dp)
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(Color.White, RoundedCornerShape(28.dp))
                            .clickable {
                                if (email.isNotEmpty() && password.isNotEmpty()) {
                                    viewModel.login(
                                        email.trim(),
                                        password.trim(),
                                        loginMode,
                                        onLoginSuccess
                                    )
                                }
                            },
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

            // Error message
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Swipe Indicator with Turn-Signal Animation ──────────────
            // BB-UI-01: << swipe for Artists >> with sequential glow
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 20.dp)
                    .alpha(0.6f)
            ) {
                TurnSignalArrows(direction = ArrowDirection.LEFT, phase = arrowPhase)
                Text(
                    text = if (loginMode == LoginMode.EVERYONE)
                        "swipe for Artists" else "swipe for Everyone",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                TurnSignalArrows(direction = ArrowDirection.RIGHT, phase = arrowPhase)
            }

            // ── Bottom Links ────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 60.dp)
            ) {
                // BB-UI-01: "forgot my password" centered
                Text(
                    text = "forgot my password",
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { /* Forgotten logic empty for now */ }
                )

                // BB-UI-02: "create an [Artists] account" with animated word interpolation
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onNavigateToSignup() }
                ) {
                    Text("create", color = Color.White, fontSize = 16.sp)
                    Text("an", color = Color.White, fontSize = 16.sp)

                    // "Artists" word expands horizontally into the space
                    AnimatedVisibility(
                        visible = loginMode == LoginMode.ARTISTS,
                        enter = expandHorizontally(
                            expandFrom = Alignment.Start,
                            animationSpec = tween(350, easing = EaseOutCubic)
                        ) + fadeIn(animationSpec = tween(350)),
                        exit = shrinkHorizontally(
                            shrinkTowards = Alignment.Start,
                            animationSpec = tween(250, easing = EaseInCubic)
                        ) + fadeOut(animationSpec = tween(250))
                    ) {
                        Row {
                            Text(
                                "Artist",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            //Spacer(modifier = Modifier.width(1.dp))
                        }
                    }

                    Text("account", color = Color.White, fontSize = 16.sp)
                }
            }
        }

        // ── Loading Overlay ─────────────────────────────────────────────
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

// ═══════════════════════════════════════════════════════════════════════════
// Shared Outlined Input for Login & Signup
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun BBOutlinedInput(
    placeholder: String,
    text: String,
    onValueChange: (String) -> Unit,
    isSecure: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .background(Color.Transparent)
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
            visualTransformation = if (isSecure)
                PasswordVisualTransformation() else VisualTransformation.None,
            cursorBrush = SolidColor(Color.White),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// BB-UI-01: Turn-Signal Arrow Animation
// Simulates car turn-signal: arrows light up sequentially from outside→in
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun TurnSignalArrows(direction: ArrowDirection, phase: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        if (direction == ArrowDirection.LEFT) {
            SignalArrowChar(
                char = "<",
                isActive = phase == 0
            )
            SignalArrowChar(
                char = "<",
                isActive = phase == 1
            )
        } else {
            SignalArrowChar(
                char = ">",
                isActive = phase == 0
            )
            SignalArrowChar(
                char = ">",
                isActive = phase == 1
            )
        }
    }
}

@Composable
private fun SignalArrowChar(
    char: String,
    isActive: Boolean
) {
    val opacity by animateFloatAsState(
        targetValue = if (isActive) 1.0f else 0.2f,
        animationSpec = tween(
            durationMillis = 350,
            easing = if (isActive) EaseOutCubic else EaseInCubic
        ),
        label = "signalArrowAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1.0f,
        animationSpec = tween(
            durationMillis = 350,
            easing = if (isActive) EaseOutCubic else EaseInCubic
        ),
        label = "signalArrowScale"
    )

    Text(
        text = char,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier
            .alpha(opacity)
            .scale(scale)
    )
}