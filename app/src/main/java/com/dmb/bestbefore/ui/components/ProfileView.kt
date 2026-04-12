package com.dmb.bestbefore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileView(onDismiss: () -> Unit = {}) {
    var selectedTab by remember { mutableIntStateOf(1) } // Default Customization
    val selectedColor = Color.Blue // Mock mapped from UserDefaults equivalent
    val isLoading = false
    
    val myRooms = listOf(RoomObject(name = "Room A"), RoomObject(name = "Room B"))
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cancel", color = Color.White, modifier = Modifier.clickable { onDismiss() })
                Text("Edit Profile", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                Text("Save", fontWeight = FontWeight.Bold, color = selectedColor, modifier = Modifier.clickable { /* Save Mock */ })
            }
            
            // Tab Picker
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    TabButton("Dashboard", selectedTab == 0, selectedColor) { selectedTab = 0 }
                }
                Box(modifier = Modifier.weight(1f)) {
                    TabButton("Customization", selectedTab == 1, selectedColor) { selectedTab = 1 }
                }
                Box(modifier = Modifier.weight(1f)) {
                    TabButton("Settings", selectedTab == 2, selectedColor) { selectedTab = 2 }
                }
            }
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                when (selectedTab) {
                    0 -> DashboardSection(selectedColor, myRooms)
                    1 -> CustomizationSection(selectedColor)
                    2 -> SettingsSection(selectedColor)
                }
            }
        }
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.4f)), 
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Composable
private fun DashboardSection(selectedColor: Color, myRooms: List<RoomObject>) {
    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
        // Mapped UI component logic
        SharedUserCard(
            name = "Maya",
            biography = "Explorer of the digital frontier...",
            roomingCount = "42",
            roomersCount = "120",
            accentColor = selectedColor,
            privacyStatus = UserPrivacyStatus.PUBLIC_ACCOUNT
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) { UIStatsCard("My Rooms", "${myRooms.size}", selectedColor) }
            Box(modifier = Modifier.weight(1f)) { UIStatsCard("Memories", "256", Color.Magenta) }
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Your Rooms", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                myRooms.forEach { room ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(width = 120.dp, height = 80.dp)
                                .background(Brush.linearGradient(listOf(selectedColor.copy(0.3f), Color.Black)), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = selectedColor)
                        }
                        Text(room.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomizationSection(selectedColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
        // Theme Selection Mock Mapping
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Interface Theme", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("Default", "Glass", "Midnight").forEach { theme ->
                    Text(
                        text = theme,
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .background(if (theme == "Default") selectedColor else Color.White.copy(0.1f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable { /* Select Theme Logic */ }
                    )
                }
            }
        }
        
        // Profile Music Mapping
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Profile Music", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            MusicPresetOption("None", true, selectedColor) {}
            MusicPresetOption("Dreamy Synth", false, selectedColor) {}
            MusicPresetOption("Chill Cafe", false, selectedColor) {}
        }
    }
}

@Composable
private fun SettingsSection(selectedColor: Color) {
    var newEmail by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Account Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Update Email", fontSize = 14.sp, color = Color.Gray)
                TextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    placeholder = { Text("New Email", color = Color.Gray) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(0.05f),
                        unfocusedContainerColor = Color.White.copy(0.05f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Update Password", fontSize = 14.sp, color = Color.Gray)
                TextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    placeholder = { Text("New Password (min 6 chars)", color = Color.Gray) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(0.05f),
                        unfocusedContainerColor = Color.White.copy(0.05f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(selectedColor.copy(0.2f), RoundedCornerShape(12.dp))
                    .border(1.dp, selectedColor.copy(0.5f), RoundedCornerShape(12.dp))
                    .clickable { /* Save Logic */ }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Update Credentials", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        
        Box(
            modifier = Modifier
                .padding(top = 20.dp)
                .fillMaxWidth()
                .background(Color.Red.copy(0.1f), RoundedCornerShape(12.dp))
                .clickable { /* Logout Logic */ }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Log Out", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Red)
        }
    }
}

@Preview
@Composable
fun ProfileViewPreview() {
    ProfileView()
}
