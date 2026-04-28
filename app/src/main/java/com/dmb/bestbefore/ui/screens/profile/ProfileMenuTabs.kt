package com.dmb.bestbefore.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import com.dmb.bestbefore.ui.theme.LocalBestBeforeColors
import coil.compose.AsyncImage
import com.dmb.bestbefore.ui.components.ProfileAvatar

// --- REFACTORED PROFILE MENU (iOS Tab Style) ---

@Composable
fun ProfileMenuScreen(
    viewModel: ProfileViewModel,
    musicViewModel: com.dmb.bestbefore.ui.components.MusicViewModel,
    createdRooms: List<TimeCapsuleRoom>,
    onLogout: () -> Unit,
    hallwayViewModel: com.dmb.bestbefore.ui.screens.hallway.HallwayViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Load theme preferences on first composition    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadThemePreferences(context)
    }
    
    var selectedTab by remember { mutableIntStateOf(0) } // Default to Dashboard (0)

    val colors = LocalBestBeforeColors.current
    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
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
                    color = colors.primary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterEnd).clickable { 
                        viewModel.saveCustomization(context)
                        viewModel.closeOverlay() 
                    }
                )
            }

            // Tabs Segment
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(32.dp)
                    .background(colors.surface, RoundedCornerShape(8.dp))
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
                    1 -> CustomizationTab(viewModel, musicViewModel)
                    2 -> SettingsTab(viewModel, hallwayViewModel, onLogout)
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
    val isRefreshing by viewModel.isLoading.collectAsState(initial = false)
    
    val userName by viewModel.userName.collectAsState(initial = "")
    val bio by viewModel.bio.collectAsState(initial = "")
    val accentColor by viewModel.accentColor.collectAsState(initial = Color(0xFF007AFF))
    val totalRooms by viewModel.totalRooms.collectAsState(initial = 0)
    val totalMemories by viewModel.totalMemories.collectAsState(initial = 0)
    val roomingCount by viewModel.roomingCount.collectAsState(initial = 0)
    val roomersCount by viewModel.roomersCount.collectAsState(initial = 0)
    val profileImageUri by viewModel.profileImageUri.collectAsState(initial = null as Uri?)
    val preferredTags by viewModel.preferredTags.collectAsState(initial = emptyList<String>())

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
            com.dmb.bestbefore.ui.components.SharedUserCard(
                name = if (userName.startsWith("@")) userName else "@$userName",
                biography = bio,
                roomingCount = roomingCount.toString(),
                roomersCount = roomersCount.toString(),
                accentColor = accentColor,
                privacyStatus = com.dmb.bestbefore.ui.components.UserPrivacyStatus.NONE,
                profileImageUri = profileImageUri,
                tags = preferredTags
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.dmb.bestbefore.ui.components.UIStatsCard(
                    title = "My Rooms",
                    value = totalRooms.toString(),
                    color = Color(0xFF00CFE8),
                    icon = Icons.Default.Home,
                    modifier = Modifier.weight(1f)
                )
                com.dmb.bestbefore.ui.components.UIStatsCard(
                    title = "Memories",
                    value = totalMemories.toString(),
                    color = Color(0xFFDB5BFF),
                    icon = Icons.Default.Collections,
                    modifier = Modifier.weight(1f)
                )
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
                     items(createdRooms, key = { it.id }) { room ->
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
                                          AsyncImage(
                                              model = room.photos.first().url,
                                              contentDescription = null,
                                              modifier = Modifier.fillMaxSize(),
                                              contentScale = ContentScale.Crop,
                                              alpha = 0.5f
                                          )
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
            val activities by viewModel.recentActivities.collectAsState(initial = emptyList())
            
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
fun CustomizationTab(
    viewModel: ProfileViewModel,
    musicViewModel: com.dmb.bestbefore.ui.components.MusicViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val selectedTheme by viewModel.selectedTheme.collectAsState(initial = com.dmb.bestbefore.ui.theme.AppThemes.Default)
    val accentColor by viewModel.accentColor.collectAsState(initial = Color(0xFF007AFF))
    val applyAccentToAll by viewModel.applyAccentToAll.collectAsState(initial = false)
    val syncAccent by viewModel.syncAccentWithRoom.collectAsState(initial = false)
    val profileImageUri by viewModel.profileImageUri.collectAsState(initial = null as Uri?)
    
    val userName by viewModel.userName.collectAsState(initial = "")
    val bioText by viewModel.bio.collectAsState(initial = "")
    val preferredTags by viewModel.preferredTags.collectAsState(initial = emptyList<String>())
    val accentColorTags by viewModel.accentColor.collectAsState(initial = Color(0xFF007AFF))

    val colors = LocalBestBeforeColors.current

    val updatePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.updateProfileImage(it, context) }
    }

    // Load tracks when tab is active
    var authToken by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        authToken = viewModel.getAuthToken(context)
    }
    
    LaunchedEffect(authToken) {
        authToken?.let { musicViewModel.loadPlaylist(it) }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        // BB-UI-14: Public Name
        Text(text = "Public Name", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        BasicTextField(
            value = userName,
            onValueChange = { viewModel.updateUserName(it) },
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(12.dp))
                .padding(16.dp),
            decorationBox = { innerTextField ->
                if (userName.isEmpty()) Text("Enter name", color = Color.Gray)
                innerTextField()
            }
        )
        Spacer(modifier = Modifier.height(20.dp))

        // BB-UI-14: Biography
        Text(text = "Biography", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        BasicTextField(
            value = bioText,
            onValueChange = { viewModel.updateBio(it) },
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(12.dp))
                .padding(16.dp)
                .heightIn(min = 90.dp),
            decorationBox = { innerTextField ->
                if (bioText.isEmpty()) Text("Enter biography", color = Color.Gray)
                innerTextField()
            }
        )
        Spacer(modifier = Modifier.height(20.dp))

        // ── Profile Tags ──────────────────────────────────────────────
        var tagInput by remember { mutableStateOf("") }

        Text(
            text = "Profile Tags",
            color = Color.Gray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Tag chips display (scrollable)
        if (preferredTags.isNotEmpty()) {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                items(preferredTags) { tag ->
                    Row(
                        modifier = Modifier
                            .background(
                                accentColorTags.copy(alpha = 0.15f),
                                RoundedCornerShape(50.dp)
                            )
                            .border(1.dp, accentColorTags.copy(alpha = 0.4f), RoundedCornerShape(50.dp))
                            .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Tag,
                            contentDescription = null,
                            tint = accentColorTags,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = tag,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColorTags
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .clickable { viewModel.removeProfileTag(tag, context) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "×",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                lineHeight = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Tag input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(colors.surface, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Tag,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = tagInput,
                    onValueChange = { if (it.length <= 20) tagInput = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (tagInput.isEmpty()) Text("Add a tag (e.g. artist, music)", color = Color.Gray, fontSize = 14.sp)
                        inner()
                    }
                )
            }
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .background(accentColorTags, RoundedCornerShape(12.dp))
                    .clickable {
                        if (tagInput.isNotBlank()) {
                            viewModel.addProfileTag(tagInput, context)
                            tagInput = ""
                        }
                    }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Add", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Tags appear on your profile card and help others discover you.",
            color = Color.Gray,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(20.dp))

        // BB-UI-14: Profile Photo
        Text(text = "Profile Photo", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        ProfileAvatar(
            imageUri = profileImageUri,
            size = 84.dp,
            accentColor = accentColor,
            onClick = { updatePhotoLauncher.launch("image/*") }
        )
        Spacer(modifier = Modifier.height(24.dp))

        // BB-UI-15: Interface Theme
        Text(text = "Interface Theme", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            com.dmb.bestbefore.ui.theme.AppThemes.getAllThemes().forEach { theme ->
               val isSelected = theme.name == selectedTheme.name
               val forceWhiteSelectedText = applyAccentToAll && (theme.name == "Glass" || theme.name == "Midnight")
               Box(
                   modifier = Modifier
                       .weight(1f)
                       .height(38.dp)
                       .background(
                           if (isSelected) accentColor else colors.surface,
                           RoundedCornerShape(18.dp)
                       )
                       .border(
                           1.dp,
                           if (isSelected) accentColor else Color.White.copy(alpha = 0.08f),
                           RoundedCornerShape(18.dp)
                       )
                       .clickable { viewModel.selectTheme(context, theme) },
                   contentAlignment = Alignment.Center
               ) {
                   Text(
                       text = theme.name,
                       color = if (isSelected && forceWhiteSelectedText) Color.White else if (isSelected) Color.Black else Color.White,
                       fontSize = 12.sp,
                       fontWeight = FontWeight.Bold
                   )
               }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // BB-UI-15: Accent Color
        Text(text = "Accent Color", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val colorsList = listOf(Color(0xFF007AFF), Color(0xFFAF52DE), Color(0xFFFF3B30), Color(0xFFFF9500), Color(0xFF34C759), Color(0xFFFF2D55))
            colorsList.forEach { color ->
                val swatchColor = if (syncAccent) color.copy(alpha = 0.35f) else color
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(swatchColor, CircleShape)
                        .clickable(enabled = !syncAccent) { viewModel.selectAccentColor(context, color) }
                        .then(
                            if (accentColor == color && !syncAccent) {
                                Modifier.border(3.dp, Color.White, CircleShape)
                            } else Modifier
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Advanced Customization
        Text(text = "Advanced Customization", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // Apply Accent Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(12.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Apply Accent to all UI elements", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Converts all icons and text to the accent color, except for white exceptions in the Default theme.", color = Color.Gray, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Switch(
                checked = applyAccentToAll,
                onCheckedChange = { viewModel.toggleApplyAccent(context, it) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sync Accent Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(12.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Sync accent with room themes", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Automatically updates the global accent color based on the current room's theme color while swiping.", color = Color.Gray, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Switch(
                checked = syncAccent,
                onCheckedChange = { viewModel.toggleSyncAccent(context, it) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // BB-UI-15: Profile Music
        Text(text = "Profile Music", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        val selectedMusic by viewModel.profileMusic.collectAsState(initial = "None")
        val musicTracks by musicViewModel.tracks.collectAsState(initial = emptyList())

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(12.dp))
        ) {
             val isNone = selectedMusic == "None" || selectedMusic == null
             Row(
                 modifier = Modifier
                     .fillMaxWidth()
                     .border(if (isNone) 2.dp else 0.dp, if (isNone) accentColor else Color.Transparent, RoundedCornerShape(12.dp))
                     .clickable { viewModel.saveProfileMusic(context, "None") }
                     .padding(16.dp),
                 verticalAlignment = Alignment.CenterVertically
             ) {
                 Icon(Icons.AutoMirrored.Filled.VolumeOff, null, tint = if(isNone) Color.White else Color.Gray)
                 Spacer(modifier = Modifier.width(16.dp))
                 Text("None", color = Color.White, fontWeight = FontWeight.Bold)
                 Spacer(modifier = Modifier.weight(1f))
                 if(isNone) Icon(Icons.Default.CheckCircle, null, tint = accentColor)
             }

             musicTracks.forEach { track ->
                 Row(
                     modifier = Modifier
                         .fillMaxWidth()
                         .clickable { viewModel.saveProfileMusic(context, track.title) }
                         .padding(16.dp),
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     Icon(Icons.Default.Done, null, tint = Color.Gray)
                     Spacer(modifier = Modifier.width(16.dp))
                     Text(track.title, color = Color.Gray, fontWeight = FontWeight.Bold)
                 }
             }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // BB-UI-15: Memory Suggestions (Placeholder)
        Text(text = "Memory Suggestions", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(colors.surface, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Suggestions feature coming soon", color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// --- TAB 3: SETTINGS ---
// BB-UI-16
@Composable
fun SettingsTab(
    viewModel: ProfileViewModel,
    hallwayViewModel: com.dmb.bestbefore.ui.screens.hallway.HallwayViewModel,
    onLogout: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = LocalBestBeforeColors.current
    val accentColor = colors.primary

    var newEmail by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var showIgnoredRooms by remember { mutableStateOf(false) }
    val ignoredRoomCards by hallwayViewModel.ignoredRoomCards.collectAsState(initial = emptyList())

    // ── Ignored Rooms Full-Screen Overlay ─────────────────────────────────────
    if (showIgnoredRooms) {
        androidx.activity.compose.BackHandler { showIgnoredRooms = false }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                            .clickable { showIgnoredRooms = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Ignored Rooms",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "${ignoredRoomCards.size} room${if (ignoredRoomCards.size != 1) "s" else ""} hidden",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                if (ignoredRoomCards.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Block,
                                null,
                                tint = Color.Gray.copy(alpha = 0.4f),
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                "No ignored rooms",
                                color = Color.Gray,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Rooms you ignore from Hallway appear here.",
                                color = Color.Gray.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(ignoredRoomCards) { card ->
                            val themeColor = try {
                                card.themeColorHex?.let {
                                    android.graphics.Color.parseColor(if (it.startsWith("#")) it else "#$it")
                                        .let { raw -> Color(raw) }
                                } ?: Color(0xFF1C1C1E)
                            } catch (e: Exception) { Color(0xFF1C1C1E) }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.DarkGray)
                            ) {
                                // Gradient bg
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    themeColor.copy(alpha = 0.25f),
                                                    Color.Black.copy(alpha = 0.92f)
                                                )
                                            )
                                        )
                                )

                                // Ignored badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(14.dp)
                                        .background(Color(0xFFFF3B30).copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Block, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                        Text("Ignored", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                // Room info + Unignore button
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        card.title,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (card.description.isNotEmpty()) {
                                        Text(
                                            card.description,
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.6f),
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                    // Unignore button
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                Color.White.copy(alpha = 0.13f),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                            .clickable { hallwayViewModel.unignoreRoom(card.id) }
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            "Remove from Ignored",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(32.dp)) }
                    }
                }
            }
        }
        return
    }

    // ── Normal Settings Content ───────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        Text("Account Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        // Ignored Rooms entry row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(12.dp))
                .clickable { showIgnoredRooms = true }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFF3B30).copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Block, null, tint = Color(0xFFFF3B30), modifier = Modifier.size(18.dp))
                }
                Column {
                    Text("Ignored Rooms", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        if (ignoredRoomCards.isEmpty()) "No rooms ignored"
                        else "${ignoredRoomCards.size} room${if (ignoredRoomCards.size != 1) "s" else ""} hidden",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (ignoredRoomCards.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFF3B30).copy(alpha = 0.2f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            ignoredRoomCards.size.toString(),
                            color = Color(0xFFFF3B30),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Icon(Icons.Default.ChevronRight, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Update Email
        Text("Update Email", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        BasicTextField(
            value = newEmail,
            onValueChange = { newEmail = it },
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(12.dp))
                .padding(16.dp),
            decorationBox = { innerTextField ->
                if (newEmail.isEmpty()) Text("name@example.com", color = Color.Gray)
                innerTextField()
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Update Password
        Text("Update Password", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        BasicTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(12.dp))
                .padding(16.dp),
            decorationBox = { innerTextField ->
                if (newPassword.isEmpty()) Text("New Password (min 6 chars)", color = Color.Gray)
                innerTextField()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Update Credentials Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .clickable { }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Update Credentials", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Log Out Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF3B0D0D), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFFF3B30).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .clickable { onLogout() }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Log Out", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}
