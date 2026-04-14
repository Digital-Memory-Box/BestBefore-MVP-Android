package com.dmb.bestbefore.ui.screens.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmb.bestbefore.ui.components.AnimatedBackgroundView
import com.dmb.bestbefore.ui.screens.login.BBOutlinedInput

// ═══════════════════════════════════════════════════════════════════════════
// BB-UI-03: Signup Screen — iOS-matching design
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun SignupScreen(
    onNavigateBack: (String?) -> Unit,
    onSignupSuccess: (String) -> Unit,
    viewModel: SignupViewModel = viewModel()
) {
    val name by viewModel.name.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Animated Background (default theme — simple orbs) ───────
        AnimatedBackgroundView()

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // ── Title + Form ────────────────────────────────────────
            Column(
                verticalArrangement = Arrangement.spacedBy(60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Title
                Text(
                    text = "Create Account",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                // Form Fields
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(horizontal = 40.dp)
                ) {
                    BBOutlinedInput(
                        placeholder = "name",
                        text = name,
                        onValueChange = { viewModel.updateName(it) }
                    )
                    BBOutlinedInput(
                        placeholder = "email",
                        text = email,
                        onValueChange = { viewModel.updateEmail(it) }
                    )
                    BBOutlinedInput(
                        placeholder = "password",
                        text = password,
                        onValueChange = { viewModel.updatePassword(it) },
                        isSecure = true
                    )

                    // Sign Up Button
                    Box(
                        modifier = Modifier
                            .padding(top = 20.dp)
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(Color.White, RoundedCornerShape(28.dp))
                            .clickable {
                                if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty()) {
                                    viewModel.attemptSignup()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sign Up",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }

            // Error message
            val msg = errorMessage
            if (msg != null) {
                Text(
                    text = msg,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Bottom Link ─────────────────────────────────────────
            Text(
                text = "already have an account? login",
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(bottom = 60.dp)
                    .clickable { onNavigateBack(null) }
            )
        }

        // ── Loading Overlay ─────────────────────────────────────────
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

    // ── Signup Success Flow ─────────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.signupSuccess.collect { email ->
            onSignupSuccess(email)
        }
    }

    // ── Email Verification Dialog ───────────────────────────────────
    val isVerificationSent by viewModel.isVerificationSent.collectAsState()
    if (isVerificationSent) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .background(Color(0xFF2C2C2C), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Verify Email",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "An email has been sent to $email.\nPlease verify your email to continue.",
                    fontSize = 16.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.checkVerificationStatus() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checking...", color = Color.Black)
                    } else {
                        Text(
                            "I have verified my email",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                TextButton(onClick = { /* Could add resend logic here */ }) {
                    Text("Resend Email", color = Color.Gray)
                }
            }
        }
    }
}
