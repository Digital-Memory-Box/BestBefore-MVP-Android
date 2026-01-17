package com.dmb.bestbefore.ui.screens.login

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

enum class LoginState {
    INITIAL, EMAIL_INPUT, PASSWORD_INPUT
}

@Composable
fun LoginScreen(
    onNavigateToSignup: () -> Unit,
    onNavigateToRoom: (String, String) -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val loginState by viewModel.loginState.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val focusManager = LocalFocusManager.current
    
    // Auto-animate entrance
    LaunchedEffect(Unit) {
        viewModel.transitionToEmailInput()
    }

    val transition = updateTransition(targetState = loginState, label = "login_transition")

    val sphereOffset by transition.animateDp(
        transitionSpec = { tween(durationMillis = 600, easing = FastOutSlowInEasing) },
        label = "sphere_offset"
    ) { state ->
        when (state) {
            LoginState.INITIAL -> 0.dp
            else -> 200.dp
        }
    }

    val titleOffsetY by transition.animateDp(
        transitionSpec = { tween(durationMillis = 600, easing = FastOutSlowInEasing) },
        label = "title_offset"
    ) { state ->
        when (state) {
            LoginState.INITIAL -> 0.dp
            else -> (-250).dp
        }
    }

    val emailAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 800) },
        label = "email_alpha"
    ) { state ->
        if (state != LoginState.INITIAL) 1f else 0f
    }

    val passwordAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 800) },
        label = "password_alpha"
    ) { state ->
        if (state == LoginState.PASSWORD_INPUT) 1f else 0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AnimatedBubbleBackground()



        Text(
            text = "BestBefore",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = titleOffsetY)
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = sphereOffset)
                .size(280.dp)
                .clip(
                    if (loginState == LoginState.INITIAL) CircleShape
                    else RoundedCornerShape(topStart = 140.dp, bottomStart = 140.dp)
                )
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF0080FF),
                            Color(0xFF00CC66)
                        )
                    )
                )
                .clickable {
                    when (loginState) {
                        LoginState.INITIAL -> viewModel.transitionToEmailInput()
                        LoginState.EMAIL_INPUT -> {
                            if (email.isNotEmpty()) {
                                viewModel.transitionToPasswordInput()
                            }
                        }
                        LoginState.PASSWORD_INPUT -> viewModel.attemptLogin()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (loginState != LoginState.INITIAL) {
                Text(
                    text = "Login",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.offset(x = (-60).dp)
                )
            }
        }

        if (loginState != LoginState.INITIAL) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .align(Alignment.Center)
                    .offset(y = (-90).dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.updateEmail(it) },
                    placeholder = { Text("email or nickname", color = Color.LightGray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            if (email.isNotEmpty()) {
                                viewModel.transitionToPasswordInput()
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White,
                        cursorColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = emailAlpha }
                )

                if (loginState == LoginState.PASSWORD_INPUT) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { viewModel.updatePassword(it) },
                        placeholder = { Text("password", color = Color.LightGray) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.attemptLogin()
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White,
                            cursorColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = passwordAlpha }
                    )

                    TextButton(
                        onClick = { /* Handle forgot password */ },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text("forgot my password", color = Color.White, fontSize = 14.sp)
                    }

                    TextButton(
                        onClick = onNavigateToSignup,
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text("create an account", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }

        if (errorMessage != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(errorMessage ?: "")
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loginSuccess.collect { (roomId, roomName) ->
            onNavigateToRoom(roomId, roomName)
        }
    }
}

@Composable
fun AnimatedBubbleBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bubble_animation")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

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

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        // Main center glow - blue to green gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF0D59F2).copy(alpha = 0.4f),
                    Color(0xFF00D972).copy(alpha = 0.2f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = size.minDimension * 0.5f * pulseScale
            ),
            radius = size.minDimension * 0.5f * pulseScale,
            center = Offset(centerX, centerY)
        )

        // Floating orb 1 - magenta/orange
        val orb1X = centerX + 180f * kotlin.math.cos(orb1Angle * kotlin.math.PI / 180).toFloat()
        val orb1Y = centerY + 180f * kotlin.math.sin(orb1Angle * kotlin.math.PI / 180).toFloat()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFF223E8).copy(alpha = 0.5f),
                    Color(0xFFFF9933).copy(alpha = 0.2f),
                    Color.Transparent
                )
            ),
            radius = 70f,
            center = Offset(orb1X, orb1Y)
        )

        // Floating orb 2 - cyan/purple
        val orb2X = centerX - 200f * kotlin.math.cos(orb2Angle * kotlin.math.PI / 180).toFloat()
        val orb2Y = centerY + 140f * kotlin.math.sin(orb2Angle * kotlin.math.PI / 180).toFloat()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF4DF2F2).copy(alpha = 0.4f),
                    Color(0xFFE64DF2).copy(alpha = 0.2f),
                    Color.Transparent
                )
            ),
            radius = 90f,
            center = Offset(orb2X, orb2Y)
        )

        // Subtle vignette overlay
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.2f),
                    Color.Black.copy(alpha = 0.4f)
                ),
                center = Offset(centerX, centerY),
                radius = size.maxDimension * 0.6f
            ),
            radius = size.maxDimension * 0.6f,
            center = Offset(centerX, centerY)
        )
    }
}