package com.dmb.bestbefore.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CreateRoomFlowView(onDismiss: () -> Unit = {}) {
    var step by remember { mutableStateOf(1) }
    var roomName by remember { mutableStateOf("") }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Create Room", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp).clickable { onDismiss() })
            }
            
            // Progress Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..5) {
                    Box(modifier = Modifier.size(10.dp).background(if (step >= i) Color.Blue else Color.Gray.copy(0.3f), CircleShape))
                    if (i < 5) {
                        Box(modifier = Modifier.size(30.dp, 2.dp).background(if (step > i) Color.Blue else Color.Gray.copy(0.3f)))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(30.dp))
            
            // Flow Content
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(targetState = step, label = "StepTransition") { currentStep ->
                    when (currentStep) {
                        1 -> StepOneContent(roomName = roomName, onRoomNameChange = { roomName = it })
                        2 -> StepTwoContent()
                        3 -> StepThreeContent()
                        4 -> StepFourContent()
                        5 -> StepFiveContent()
                    }
                }
            }
            
            // Bottom Action Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (step > 1) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(0.1f), RoundedCornerShape(12.dp))
                            .clickable { step -= 1 }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Back", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (step == 1 && roomName.isEmpty()) Color.Gray else Color.Blue, RoundedCornerShape(12.dp))
                        .clickable(enabled = !(step == 1 && roomName.isEmpty())) {
                            if (step < 5) step += 1 else onDismiss()
                        }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (step < 5) "Next" else "Create Room", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// ----------------------------------------------------
// Step Contents (Visual Mocks matching Swift Structure)
// ----------------------------------------------------

@Composable
fun StepOneContent(roomName: String, onRoomNameChange: (String) -> Unit) {
    var isPrivate by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("What's the name?", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Give your room a unique title.", fontSize = 16.sp, color = Color.Gray)
        }
        
        TextField(
            value = roomName,
            onValueChange = onRoomNameChange,
            placeholder = { Text("Room Name", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(0.1f),
                unfocusedContainerColor = Color.White.copy(0.1f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tags (e.g. trip, music)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            TextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Add a tag...", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.White.copy(0.1f), unfocusedContainerColor = Color.White.copy(0.1f)),
                shape = RoundedCornerShape(12.dp)
            )
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Privacy Status", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrivacyOption("Public", "Anyone can see.", Icons.Default.List, !isPrivate, Color.Blue, { isPrivate = false }, modifier = Modifier.weight(1f))
                PrivacyOption("Private", "Invite only.", Icons.Default.Lock, isPrivate, Color.Blue, { isPrivate = true }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StepTwoContent() {
    var timeCapsuleEnabled by remember { mutableStateOf(true) }
    var durationDays by remember { mutableStateOf(21) }
    
    Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Time Capsule?", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Lock memories for a future date.", fontSize = 16.sp, color = Color.Gray)
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White.copy(0.1f), RoundedCornerShape(12.dp)).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Enable Time Capsule", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Content will be hidden until the timer ends.", fontSize = 12.sp, color = Color.Gray)
            }
            Switch(checked = timeCapsuleEnabled, onCheckedChange = { timeCapsuleEnabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.Blue))
        }
        
        if (timeCapsuleEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Unlock Method", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                // Duration Mock
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DurationButton("7 Days", 7, durationDays, Color.Blue, { durationDays = it }, modifier = Modifier.weight(1f))
                    DurationButton("21 Days", 21, durationDays, Color.Blue, { durationDays = it }, modifier = Modifier.weight(1f))
                    DurationButton("60 Days", 60, durationDays, Color.Blue, { durationDays = it }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun StepThreeContent() {
    var selectedTheme by remember { mutableStateOf("default") }
    
    Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Atmosphere", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Set the mood with background music.", fontSize = 16.sp, color = Color.Gray)
        }
        
        // Theme Selector
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Room Theme", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ThemeOption("Default", Color.Blue, selectedTheme == "default") { selectedTheme = "default" }
                ThemeOption("Cyberpunk", Color.Magenta, selectedTheme == "cyberpunk") { selectedTheme = "cyberpunk" }
                ThemeOption("Ocean", Color.Cyan, selectedTheme == "ocean") { selectedTheme = "ocean" }
            }
        }
        
        Divider(color = Color.White.copy(0.1f))
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Background Music", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            // Re-using common preset layout logic. (Assuming MusicPresetOption is globally aligned, here we just visually stack them)
            Column(modifier = Modifier.fillMaxWidth().background(Color.White.copy(0.05f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                Text("SoundCloud Widget enabled", color = Color.White)
            }
        }
    }
}

@Composable
fun StepFourContent() {
    Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Memory Dump Rules", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Configure auto-archival for drops.", fontSize = 16.sp, color = Color.Gray)
        }
        
        Column(modifier = Modifier.fillMaxWidth().background(Color.White.copy(0.05f), RoundedCornerShape(12.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Rolling Expiration (Snapchat Mode)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Automatically archive memories X days after they are posted.", fontSize = 14.sp, color = Color.Gray)
        }
        
        Column(modifier = Modifier.fillMaxWidth().background(Color.White.copy(0.05f), RoundedCornerShape(12.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Scheduled Room Closure", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Lock the entire room into a read-only archive state after a specific date.", fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@Composable
fun StepFiveContent() {
    Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Invite Friends", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Add people and assign them roles.", fontSize = 16.sp, color = Color.Gray)
        }
        
        // Mocking Collaborator inputs visually
        TextField(
            value = "",
            onValueChange = {},
            placeholder = { Text("Friend's Email Address", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.White.copy(0.1f), unfocusedContainerColor = Color.White.copy(0.1f)),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Preview
@Composable
fun CreateRoomFlowViewPreview() {
    CreateRoomFlowView()
}
