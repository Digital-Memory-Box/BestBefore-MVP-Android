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
    val userType by viewModel.userType.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var ageConfirmed by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Animated Background — switches with userType ─────────────
        AnimatedBackgroundView(theme = if (userType == "artist") "artist" else "default")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 16.dp),
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
                    text = androidx.compose.ui.res.stringResource(com.dmb.bestbefore.R.string.create_account_title),
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
                        placeholder = androidx.compose.ui.res.stringResource(com.dmb.bestbefore.R.string.name_placeholder),
                        text = name,
                        onValueChange = { viewModel.updateName(it) }
                    )
                    BBOutlinedInput(
                        placeholder = androidx.compose.ui.res.stringResource(com.dmb.bestbefore.R.string.email_placeholder),
                        text = email,
                        onValueChange = { viewModel.updateEmail(it) }
                    )
                    BBOutlinedInput(
                        placeholder = androidx.compose.ui.res.stringResource(com.dmb.bestbefore.R.string.password_placeholder),
                        text = password,
                        onValueChange = { viewModel.updatePassword(it) },
                        isSecure = true
                    )

                    // ── User Type Selector ──────────────────────────────
                    val normalLabel = androidx.compose.ui.res.stringResource(com.dmb.bestbefore.R.string.user_type_normal)
                    val artistLabel = androidx.compose.ui.res.stringResource(com.dmb.bestbefore.R.string.user_type_artist)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                            .padding(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            listOf("normal" to normalLabel, "artist" to artistLabel).forEach { (type, label) ->
                                val isSelected = userType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(
                                            if (isSelected) Color.White else Color.Transparent,
                                            RoundedCornerShape(20.dp)
                                        )
                                        .clickable { viewModel.updateUserType(type) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    // ── Terms & Age Checkboxes ──────────────────────────────
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { ageConfirmed = !ageConfirmed }
                    ) {
                        Checkbox(
                            checked = ageConfirmed,
                            onCheckedChange = { ageConfirmed = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color.White, checkmarkColor = Color.Black, uncheckedColor = Color.White)
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.dmb.bestbefore.R.string.age_confirmation),
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { termsAccepted = !termsAccepted }
                    ) {
                        Checkbox(
                            checked = termsAccepted,
                            onCheckedChange = { termsAccepted = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color.White, checkmarkColor = Color.Black, uncheckedColor = Color.White)
                        )
                        Column {
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.dmb.bestbefore.R.string.terms_agreement_prefix),
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Row {
                                Text(
                                    text = androidx.compose.ui.res.stringResource(com.dmb.bestbefore.R.string.terms_of_service),
                                    color = Color(0xFF00BFFF),
                                    fontSize = 14.sp,
                                    modifier = Modifier.clickable {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("${com.dmb.bestbefore.BuildConfig.API_BASE_URL}terms"))
                                        context.startActivity(intent)
                                    }
                                )
                                Text(
                                    text = androidx.compose.ui.res.stringResource(com.dmb.bestbefore.R.string.terms_and),
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = androidx.compose.ui.res.stringResource(com.dmb.bestbefore.R.string.privacy_policy),
                                    color = Color(0xFF00BFFF),
                                    fontSize = 14.sp,
                                    modifier = Modifier.clickable {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("${com.dmb.bestbefore.BuildConfig.API_BASE_URL}privacy"))
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        }
                    }

                    // Sign Up Button
                    val isFormValid = email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty() && ageConfirmed && termsAccepted && !isLoading
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(if (isFormValid) Color.White else Color.Gray, RoundedCornerShape(28.dp))
                            .clickable(enabled = isFormValid) {
                                viewModel.attemptSignup()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.dmb.bestbefore.R.string.sign_up_button),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                    if (!errorMessage.isNullOrEmpty()) {
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }


            Spacer(modifier = Modifier.weight(1f))

            // ── Bottom Link ─────────────────────────────────────────
            Text(
                text = androidx.compose.ui.res.stringResource(com.dmb.bestbefore.R.string.already_have_account),
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
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier.scale(1.5f)
                    )
                    Text(
                        text = "Creating account...",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    TextButton(onClick = { viewModel.cancelSignup() }) {
                        Text("Cancel", color = Color.LightGray, fontSize = 14.sp)
                    }
                }
            }
        }

        // ── Error Banner ─────────────────────────────────────────────
        val msg = errorMessage
        if (msg != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 100.dp)
                    .background(Color(0xFFCC0000), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = msg,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ── Email Verification Dialog ───────────────────────────────────
        val isVerificationSent by viewModel.isVerificationSent.collectAsState()
        if (isVerificationSent) {
            AlertDialog(
                onDismissRequest = { /* prevent dismiss by tapping outside */ },
                containerColor = Color(0xFF2C2C2C),
                title = {
                    Text(
                        text = "📧 Verify Your Email",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Text(
                        text = "A verification link has been sent to:\n$email\n\nPlease click the link in your inbox, then tap the button below.",
                        fontSize = 14.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.checkVerificationStatus() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Checking...", color = Color.Black)
                        } else {
                            Text("I Have Verified My Email ✓", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.attemptSignup() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Resend Email", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            )
        }
    }

    // ── Signup Success Flow ─────────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.signupSuccess.collect { email ->
            onSignupSuccess(email)
        }
    }
}


