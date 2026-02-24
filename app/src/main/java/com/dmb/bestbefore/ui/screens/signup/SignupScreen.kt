package com.dmb.bestbefore.ui.screens.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel

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
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Create Account",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Name Field
            IOSStyleTextFieldS(
                value = name,
                onValueChange = { viewModel.updateName(it) },
                placeholder = "Name (optional)",
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )

            // Email Field
            IOSStyleTextFieldS(
                value = email,
                onValueChange = { viewModel.updateEmail(it) },
                placeholder = "Email",
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )

            // Password Field
            IOSStyleTextFieldS(
                value = password,
                onValueChange = { viewModel.updatePassword(it) },
                placeholder = "Password (min 6 chars)",
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                isPassword = true,
                onDone = {
                    focusManager.clearFocus()
                    viewModel.attemptSignup()
                }
            )

            Button(
                onClick = { viewModel.attemptSignup() },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007AFF) // iOS systemBlue
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        "Create Account",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            TextButton(
                onClick = { onNavigateBack(null) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    "Already have an account? Log in",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            }
        }

        if (errorMessage != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
             containerColor = Color(0xFF333333), // Dark gray
             contentColor = Color.White
            ) {
                Text(errorMessage ?: "")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.signupSuccess.collect { email ->
            onSignupSuccess(email)
        }
    }
    
    val isVerificationSent by viewModel.isVerificationSent.collectAsState()
    if (isVerificationSent) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable(enabled = false) {}, // Block interaction
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checking...")
                    } else {
                         Text("I have verified my email")
                    }
                }
                
                TextButton(onClick = { /* Could add resend logic here */ }) {
                    Text("Resend Email", color = Color.Gray)
                }
            }
            }
    }
}

// Duplicated from LoginScreen.kt to avoid package issues without refactoring
@Composable
fun IOSStyleTextFieldS(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    isPassword: Boolean = false,
    onDone: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onDone = { onDone?.invoke() }
        ),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        singleLine = true,
        cursorBrush = SolidColor(Color.White),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(width = 1.dp, color = Color.White, shape = RoundedCornerShape(10.dp))
                    .background(Color.Black, shape = RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = Color.LightGray,
                        fontSize = 16.sp
                    )
                }
                innerTextField()
            }
        }
    )
}
