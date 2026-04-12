package com.dmb.bestbefore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

@Composable
fun EditRoomView(room: RoomObject? = null, onDismiss: () -> Unit = {}) {
    var roomName by remember { mutableStateOf(room?.name ?: "") }
    var isPrivate by remember { mutableStateOf(room?.isPrivate ?: false) }
    var timeCapsuleEnabled by remember { mutableStateOf(room?.isTimeCapsule ?: false) }
    var durationDays by remember { mutableStateOf(room?.capsuleDurationDays ?: 21) }
    var selectedTheme by remember { mutableStateOf(room?.theme ?: "default") }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit Room", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp).clickable { onDismiss() })
            }
            
            // Scrollable Settings
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(30.dp)
            ) {
                
                // Name
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Room Name", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    TextField(
                        value = roomName,
                        onValueChange = { roomName = it },
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
                }
                
                // Privacy
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Privacy Status", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrivacyOption("Public", "Anyone can see.", Icons.Default.List, !isPrivate, Color.Blue, { isPrivate = false }, modifier = Modifier.weight(1f))
                        PrivacyOption("Private", "Only invited.", Icons.Default.Lock, isPrivate, Color.Blue, { isPrivate = true }, modifier = Modifier.weight(1f))
                    }
                }
                
                // Time Capsule
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color.White.copy(0.1f), RoundedCornerShape(12.dp)).padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Enable Time Capsule", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Content hidden until timer ends.", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(checked = timeCapsuleEnabled, onCheckedChange = { timeCapsuleEnabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.Blue))
                    }
                    
                    if (timeCapsuleEnabled) {
                        // Presets visually
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DurationButton("1 Week", 7, durationDays, Color.Blue, { durationDays = it }, modifier = Modifier.weight(1f))
                            DurationButton("21 Days", 21, durationDays, Color.Blue, { durationDays = it }, modifier = Modifier.weight(1f))
                            DurationButton("1 Month", 30, durationDays, Color.Blue, { durationDays = it }, modifier = Modifier.weight(1f))
                        }
                    }
                }
                
                Divider(color = Color.White.copy(0.1f))
                
                // Dump Rules
                Column(
                    modifier = Modifier.fillMaxWidth().background(Color.White.copy(0.05f), RoundedCornerShape(12.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Memory Dump Rules", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Rolling Expiry Configuration", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                
                // Theme
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Room Theme", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ThemeOption("Default", Color.Blue, selectedTheme == "default") { selectedTheme = "default" }
                        ThemeOption("Ocean", Color.Cyan, selectedTheme == "ocean") { selectedTheme = "ocean" }
                        ThemeOption("Sunset", Color.Yellow, selectedTheme == "sunset") { selectedTheme = "sunset" }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Save Action
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 30.dp)
                    .background(if (roomName.isEmpty()) Color.Gray else Color.Blue, RoundedCornerShape(12.dp))
                    .clickable(enabled = roomName.isNotEmpty(), onClick = onDismiss)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Preview
@Composable
fun EditRoomViewPreview() {
    EditRoomView()
}
