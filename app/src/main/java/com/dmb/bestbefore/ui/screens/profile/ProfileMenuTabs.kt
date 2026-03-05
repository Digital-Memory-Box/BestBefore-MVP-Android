package com.dmb.bestbefore.ui.screens.profile

import androidx.compose.animation.*
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.verticalScroll
import com.dmb.bestbefore.data.models.TimeCapsuleRoom
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image


import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api

// --- REFACTORED PROFILE MENU (iOS Tab Style) ---

@Composable
fun ProfileMenuScreen(
    viewModel: ProfileViewModel,
    createdRooms: List<TimeCapsuleRoom>,
    onLogout: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Load theme preferences on first composition    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadThemePreferences(context)
    }
    
    var selectedTab by remember { mutableIntStateOf(0) } // Default to Dashboard (0)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Cancel",
                    color = Color.White,
                    fontSize = 17.sp,
                    modifier = Modifier.align(Alignment.CenterStart).clickable { viewModel.closeOverlay() }
                )
                Text(
                    text = "Edit Profile",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
                Text(
                    text = "Save",
                    color = Color(0xFF007AFF),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterEnd).clickable { viewModel.closeOverlay() } // Mock Save
                )
            }

            // Tabs Segment
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(32.dp)
                    .background(Color(0xFF1C1C1E), RoundedCornerShape(8.dp))
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf("Dashboard", "Customization", "Settings")
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (isSelected) Color(0xFF636366) else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { selectedTab = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> DashboardTab(viewModel, createdRooms)
                    1 -> CustomizationTab(viewModel)
                    2 -> SettingsTab(viewModel, onLogout)
                }
            }
        }
    }
}

// --- TAB 1: DASHBOARD ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTab(
    viewModel: ProfileViewModel,
    createdRooms: List<TimeCapsuleRoom>
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isRefreshing by viewModel.isLoading.collectAsState()

    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.initDatabase(context) },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
        // Stats Cards
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // My Rooms Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp)
                        .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                         Icon(Icons.Default.Home, null, tint = Color(0xFF007AFF), modifier = Modifier.size(32.dp))
                         Column {
                             val totalRooms by viewModel.totalRooms.collectAsState()
                             Text(totalRooms.toString(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                             Text("My Rooms", color = Color.Gray, fontSize = 14.sp)
                         }
                    }
                }
                 // Memories Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp)
                        .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                         Icon(Icons.Default.Image, null, tint = Color(0xFFAF52DE), modifier = Modifier.size(32.dp))
                         Column {
                             val totalMemories by viewModel.totalMemories.collectAsState()
                             Text(totalMemories.toString(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                             Text("Memories", color = Color.Gray, fontSize = 14.sp)
                         }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Your Rooms
        item {
            Text("Your Rooms", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                 if (createdRooms.isEmpty()) {
                     item {
                         Text("No rooms yet", color = Color.Gray, fontSize = 14.sp)
                     }
                 } else {
                     items(createdRooms) { room ->
                         val context = androidx.compose.ui.platform.LocalContext.current
                         var showMenu by remember { mutableStateOf(false) }
                         var showRoomDetails by remember { mutableStateOf(false) }
                         
                         Box(modifier = Modifier.width(120.dp)) {
                             Column(
                                 modifier = Modifier
                                     .width(120.dp)
                                     .clickable { 
                                         // Click on card to enter room
                                         viewModel.selectRoom(room)
                                     },
                                 horizontalAlignment = Alignment.CenterHorizontally
                             ) {
                                  Box(
                                      modifier = Modifier
                                          .fillMaxWidth()
                                          .aspectRatio(1.3f)
                                          .background(Brush.linearGradient(listOf(Color(0xFF0038A8), Color(0xFF001F5C))), RoundedCornerShape(12.dp)),
                                      contentAlignment = Alignment.Center
                                  ) {
                                      if (room.photos.isNotEmpty()) {
                                          // TODO: Display actual room photo
                                          Icon(Icons.Default.Image, null, tint = Color(0xFF007AFF), modifier = Modifier.size(32.dp))
                                      } else {
                                          Icon(Icons.Default.Folder, null, tint = Color(0xFF007AFF), modifier = Modifier.size(32.dp))
                                      }
                                      
                                      // "..." menu button in top-right corner
                                      Box(
                                          modifier = Modifier
                                              .align(Alignment.TopEnd)
                                              .padding(8.dp)
                                              .size(24.dp)
                                              .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                              .clickable { showMenu = !showMenu },
                                          contentAlignment = Alignment.Center
                                      ) {
                                          Icon(Icons.Default.MoreVert, "Menu", tint = Color.White, modifier = Modifier.size(16.dp))
                                      }
                                  }
                                  Spacer(modifier = Modifier.height(8.dp))
                                  Text(room.roomName, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                             }
                             
                             // Dropdown Menu
                             DropdownMenu(
                                 expanded = showMenu,
                                 onDismissRequest = { showMenu = false },
                                 modifier = Modifier.background(Color(0xFF2C2C2E))
                             ) {
                                 // Room Details
                                 DropdownMenuItem(
                                     text = {
                                         Row(verticalAlignment = Alignment.CenterVertically) {
                                             Icon(Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                             Spacer(modifier = Modifier.width(12.dp))
                                             Text("Room Details", color = Color.White)
                                         }
                                     },
                                     onClick = {
                                         showMenu = false
                                         showRoomDetails = true
                                     }
                                 )
                                 // Edit Room
                                 DropdownMenuItem(
                                     text = {
                                         Row(verticalAlignment = Alignment.CenterVertically) {
                                             Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                             Spacer(modifier = Modifier.width(12.dp))
                                             Text("Edit Room", color = Color.White)
                                         }
                                     },
                                     onClick = {
                                         showMenu = false
                                         viewModel.selectRoomForEditing(room)
                                     }
                                 )
                                 // Delete Room
                                 DropdownMenuItem(
                                     text = {
                                         Row(verticalAlignment = Alignment.CenterVertically) {
                                             Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                                             Spacer(modifier = Modifier.width(12.dp))
                                             Text("Delete Room", color = Color.Red)
                                         }
                                     },
                                     onClick = {
                                         showMenu = false
                                         viewModel.deleteRoom(context, room, fromInsideRoom = false)
                                     }
                                 )
                             }
                             
                             // Room Details Bottom Sheet
                             if (showRoomDetails) {
                                 RoomDetailsBottomSheet(
                                     room = room,
                                     onDismiss = { showRoomDetails = false }
                                 )
                             }
                         }
                     }
                 }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Recent Activity
        item {
            Text("Recent Activity", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            val activities by viewModel.recentActivities.collectAsState()
            
            if (activities.isEmpty()) {
                Text("No recent activity", color = Color.Gray, fontSize = 14.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    activities.forEach { activity ->
                        ActivityItem(
                            icon = when (activity.type) {
                                ProfileViewModel.ActivityType.CREATED_ROOM -> Icons.Default.AddCircleOutline
                                ProfileViewModel.ActivityType.ADDED_PHOTOS -> Icons.Default.Collections
                                else -> Icons.Default.Bolt
                            },
                            title = activity.title,
                            date = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(activity.date))
                        )
                    }
                }
            }
             Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ActivityItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
         Box(
             modifier = Modifier.size(40.dp).background(Color(0xFF2C2C2E), CircleShape),
             contentAlignment = Alignment.Center
         ) {
             Icon(icon, null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
         }
         Spacer(modifier = Modifier.width(16.dp))
         Column {
             Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
             Text(date, color = Color.Gray, fontSize = 12.sp)
         }
    }
}

// --- TAB 2: CUSTOMIZATION ---
@Composable
fun CustomizationTab(viewModel: ProfileViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        // Public Name
        Text(text = "Public Name", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        val userName by viewModel.userName.collectAsState()
        BasicTextField(
            value = userName,
            onValueChange = { /* Update Name - Add to VM if needed */ },
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            modifier = Modifier
                .fillMaxWidth() // Fill width
                .background(Color(0xFF1C1C1E), RoundedCornerShape(10.dp))
                .padding(16.dp),
            decorationBox = { innerTextField ->
                if (userName.isEmpty()) Text("Enter name", color = Color.Gray)
                innerTextField()
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Interface Theme
        Text(text = "Interface Theme", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            com.dmb.bestbefore.ui.theme.AppThemes.getAllThemes().forEach { theme ->
               val isSelected = theme.name == selectedTheme.name
               Box(
                   modifier = Modifier
                       .weight(1f)
                       .height(36.dp)
                       .background(
                           if (isSelected) Color(0xFF007AFF) else Color(0xFF1C1C1E),
                           RoundedCornerShape(18.dp)
                       )
                       .clickable { viewModel.selectTheme(context, theme) },
                   contentAlignment = Alignment.Center
               ) {
                   Text(text = theme.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
               }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Accent Color
        Text(text = "Accent Color", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val colors = listOf(Color(0xFF007AFF), Color(0xFFAF52DE), Color(0xFFFF2D55), Color(0xFFFF9500), Color(0xFF34C759), Color(0xFFFF3B30))
            colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color, CircleShape)
                        .clickable { viewModel.selectAccentColor(context, color) }
                        .then(
                            if (accentColor == color) {
                                Modifier.border(3.dp, Color.White, CircleShape)
                            } else Modifier
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Profile Music
        Text(text = "Profile Music", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp))
                .padding(vertical = 8.dp) // Inner padding
        ) {
             // None
             Row(
                 modifier = Modifier.fillMaxWidth().clickable {}.border(1.dp, Color(0xFF007AFF), RoundedCornerShape(12.dp)).padding(16.dp),
                 verticalAlignment = Alignment.CenterVertically
             ) {
                 Icon(Icons.AutoMirrored.Filled.VolumeOff, null, tint = Color.White)
                 Spacer(modifier = Modifier.width(16.dp))
                 Text("None", color = Color.White, fontWeight = FontWeight.Bold)
                 Spacer(modifier = Modifier.weight(1f))
                 Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF007AFF)) 
             }
             // Options
             listOf("Dreamy Synth", "Chill Cafe").forEach { music ->
                 Row(
                     modifier = Modifier.fillMaxWidth().clickable {}.padding(16.dp),
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     Icon(if(music=="Dreamy Synth") Icons.Default.AutoAwesome else Icons.Default.Coffee, null, tint = Color.Gray)
                     Spacer(modifier = Modifier.width(16.dp))
                     Text(music, color = Color.Gray, fontWeight = FontWeight.Bold)
                 }
             }
        }
    }
}

// --- TAB 3: SETTINGS ---
@Composable
fun SettingsTab(viewModel: ProfileViewModel, onLogout: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // State for credential updates
    val updateEmailError by viewModel.credentialUpdateError.collectAsState()
    val updateEmailSuccess by viewModel.credentialUpdateSuccess.collectAsState()
    val isUpdating by viewModel.isUpdatingCredential.collectAsState()
    
    var newEmail by remember { mutableStateOf("") }
    var emailCurrentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var passwordCurrentPassword by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        Text("Account Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Update Email Section
        Text("Update Email", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        BasicTextField(
            value = newEmail,
            onValueChange = { newEmail = it },
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C1C1E), RoundedCornerShape(10.dp))
                .padding(16.dp),
            decorationBox = { innerTextField ->
                if (newEmail.isEmpty()) Text("New Email", color = Color.Gray)
                innerTextField()
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        BasicTextField(
            value = emailCurrentPassword,
            onValueChange = { emailCurrentPassword = it },
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C1C1E), RoundedCornerShape(10.dp))
                .padding(16.dp),
            decorationBox = { innerTextField ->
                if (emailCurrentPassword.isEmpty()) Text("Current Password", color = Color.Gray)
                innerTextField()
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = {
                if (newEmail.isNotBlank() && emailCurrentPassword.isNotBlank()) {
                    viewModel.updateEmail(context, newEmail, emailCurrentPassword)
                }
            },
            enabled = !isUpdating && newEmail.isNotBlank() && emailCurrentPassword.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
        ) {
            Text(if (isUpdating) "Updating..." else "Update Email")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Update Password Section
        Text("Update Password", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        BasicTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C1C1E), RoundedCornerShape(10.dp))
                .padding(16.dp),
            decorationBox = { innerTextField ->
                if (newPassword.isEmpty()) Text("New Password (min 6 chars)", color = Color.Gray)
                innerTextField()
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        BasicTextField(
            value = passwordCurrentPassword,
            onValueChange = { passwordCurrentPassword = it },
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C1C1E), RoundedCornerShape(10.dp))
                .padding(16.dp),
            decorationBox = { innerTextField ->
                if (passwordCurrentPassword.isEmpty()) Text("Current Password", color = Color.Gray)
                innerTextField()
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = {
                if (newPassword.isNotBlank() && passwordCurrentPassword.isNotBlank()) {
                    viewModel.updatePassword(context, newPassword, passwordCurrentPassword)
                }
            },
            enabled = !isUpdating && newPassword.isNotBlank() && passwordCurrentPassword.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
        ) {
            Text(if (isUpdating) "Updating..." else "Update Password")
        }
        
        // Error/Success Messages
        Spacer(modifier = Modifier.height(16.dp))
        
        updateEmailError?.let { error ->
            Text(text = error, color = Color.Red, fontSize = 14.sp)
            androidx.compose.runtime.LaunchedEffect(error) {
                kotlinx.coroutines.delay(5000)
                viewModel.clearCredentialMessages()
            }
        }
        
        updateEmailSuccess?.let { success ->
            Text(text = success, color = Color.Green, fontSize = 14.sp)
            androidx.compose.runtime.LaunchedEffect(success) {
                kotlinx.coroutines.delay(5000)
                viewModel.clearCredentialMessages()
                // Clear input fields
                newEmail = ""
                emailCurrentPassword = ""
                newPassword = ""
                passwordCurrentPassword = ""
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("App Preferences", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("High Fidelity Mode", color = Color.White, fontSize = 16.sp)
            Switch(checked = true, onCheckedChange = {})
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Spatial Audio", color = Color.White, fontSize = 16.sp)
            Switch(checked = true, onCheckedChange = {})
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E0B0B)), // Dark Red/Brown
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Log Out", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
        }
         Spacer(modifier = Modifier.height(48.dp))
    }
}
