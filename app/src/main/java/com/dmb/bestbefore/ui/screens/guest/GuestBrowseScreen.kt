package com.dmb.bestbefore.ui.screens.guest

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dmb.bestbefore.data.local.SessionManager
import com.dmb.bestbefore.ui.components.TutorialGuideDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestBrowseScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToSignup: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }
    
    var showTutorial by remember { mutableStateOf(!sessionManager.hasSeenTutorial()) }
    var showJoinDialog by remember { mutableStateOf(false) }

    if (showTutorial) {
        TutorialGuideDialog(
            onDismiss = {
                sessionManager.setHasSeenTutorial(true)
                showTutorial = false
            }
        )
    }

    if (showJoinDialog) {
        Dialog(onDismissRequest = { showJoinDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Join BestBefore",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sign in to explore memories, create rooms, and connect with others.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onNavigateToLogin,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Log In")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onNavigateToSignup,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign Up")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showJoinDialog = false }
                    ) {
                        Text("Maybe Later")
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1E1E1E)) {
                NavigationBarItem(
                    selected = false,
                    onClick = { showJoinDialog = true },
                    icon = { Text("Rooming", fontSize = 12.sp, color = Color.White) }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { showJoinDialog = true },
                    icon = { Text("Hallway", fontSize = 12.sp, color = Color.White) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { showJoinDialog = true },
                    icon = { Text("Artists", fontSize = 12.sp, color = Color.White) }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hallway",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "See All",
                    color = Color.Gray,
                    modifier = Modifier.clickable { showJoinDialog = true }
                )
            }

            // Cards
            val cardData = listOf(
                "Childhood Memories" to listOf(Color(0xFFE91E63), Color(0xFF9C27B0)),
                "Summer 2024" to listOf(Color(0xFFFF9800), Color(0xFFFFEB3B)),
                "Travel Adventures" to listOf(Color(0xFF2196F3), Color(0xFF00BCD4)),
                "Music Collection" to listOf(Color(0xFF4CAF50), Color(0xFF8BC34A))
            )

            cardData.forEach { (title, colors) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { showJoinDialog = true },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(colors))
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
