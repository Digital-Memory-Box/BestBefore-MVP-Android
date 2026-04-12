package com.dmb.bestbefore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SignupView(onDismiss: () -> Unit = {}) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLoading = false
    val errorMessage: String? = null

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackgroundView()
        
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Create Account", 
                    fontSize = 36.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = Color.White
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(horizontal = 40.dp)
                ) {
                    OutlinedInput(placeholder = "name", text = name, onValueChange = { name = it })
                    OutlinedInput(placeholder = "email", text = email, onValueChange = { email = it })
                    OutlinedInput(placeholder = "password", text = password, onValueChange = { password = it }, isSecure = true)

                    Box(
                        modifier = Modifier
                            .padding(top = 20.dp)
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(Color.White, RoundedCornerShape(28.dp))
                            .clickable { /* performSignup placeholder */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sign Up", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }

            if (errorMessage != null) {
                Text(errorMessage, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "already have an account? login",
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier
                    .padding(bottom = 60.dp)
                    .clickable { onDismiss() }
            )
        }
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.scale(1.5f))
            }
        }
    }
}

@Preview
@Composable
fun SignupViewPreview() {
    SignupView()
}
