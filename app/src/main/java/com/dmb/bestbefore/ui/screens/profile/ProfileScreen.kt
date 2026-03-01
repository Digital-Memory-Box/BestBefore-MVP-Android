package com.dmb.bestbefore.ui.screens.profile

import androidx.compose.animation.*
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.zIndex
import androidx.compose.foundation.verticalScroll

import com.dmb.bestbefore.data.models.TimeCapsuleRoom
import com.dmb.bestbefore.ui.components.OrbMenu

import com.dmb.bestbefore.data.models.HallwayCard
import com.dmb.bestbefore.ui.screens.hallway.BottomTab
import com.dmb.bestbefore.ui.screens.hallway.HallwayViewModel
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars


@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
    hallwayViewModel: HallwayViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentStep by viewModel.currentStep.collectAsState()
    val createdRooms by viewModel.createdRooms.collectAsState()

    val isAllMediaVisible by viewModel.isAllMediaVisible.collectAsState()


    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled, continue with room creation
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Calendar permission result
    }

    val readCalendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.loadUpcomingEvents(context)
        }
    }
    
    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            android.widget.Toast.makeText(context, "Camera permission required to take photos", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    // Gallery permission launcher
    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            android.widget.Toast.makeText(context, "Storage permission required to access photos", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    // File permission launcher (same as gallery for now)
    val filePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            android.widget.Toast.makeText(context, "Storage permission required to access files", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Set permission callbacks and init database
    LaunchedEffect(Unit) {
        viewModel.initDatabase(context)
        
        viewModel.onRequestNotificationPermission = {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.onRequestCalendarPermission = {
            calendarPermissionLauncher.launch(android.Manifest.permission.WRITE_CALENDAR)
        }
        viewModel.onRequestReadCalendarPermission = {
            readCalendarPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR)
        }
        
        viewModel.onRequestCameraPermission = {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
        
        viewModel.onRequestGalleryPermission = {
            val permission =
                android.Manifest.permission.READ_MEDIA_IMAGES
            galleryPermissionLauncher.launch(permission)
        }
        
        viewModel.onRequestFilePermission = {
            val permission =
                android.Manifest.permission.READ_MEDIA_IMAGES
            filePermissionLauncher.launch(permission)
        }
    }

    // Media Picker Launchers
    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.updateSelectedMedia(uris)
        }
    }
    
    // Camera capture launcher
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            viewModel.setCapturedImage(cameraImageUri!!)
        }
    }
    
    // File picker launcher - multiple files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.updateSelectedMedia(uris)
        }
    }

    // Back Handler Logic
    androidx.activity.compose.BackHandler(enabled = true) {
        if (isAllMediaVisible) {
            viewModel.toggleAllMedia(false)
        } else {
            // goBack returns true if it handled the back action internally (e.g. popped history)
            // If false, it means we are at the root (Profile/Hallway) and should exit
            if (!viewModel.goBack()) {
                onNavigateBack()
            }
        }
    }



    // Check for Deep Link from Notification
    LaunchedEffect(Unit) {
        val pendingId = com.dmb.bestbefore.MainActivity.pendingRoomId
        if (pendingId != null) {
            viewModel.handleDeepLink(pendingId)
            com.dmb.bestbefore.MainActivity.clearPending()
        }
    }




    // Moved Overlay Logic to inside the Box for correct Z-ordering


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. HALLWAY (Main Page)
        // Show only if no overlay step AND "ALL" is not visible
        AnimatedVisibility(
            visible = currentStep == ProfileStep.NONE && !isAllMediaVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val cards by hallwayViewModel.cards.collectAsState()
            val selectedIndex by hallwayViewModel.selectedCardIndex.collectAsState()
            val currentTab by hallwayViewModel.currentTab.collectAsState()

            Box(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars) // Respect status bar
                        .padding(horizontal = 24.dp, vertical = 24.dp) // Adjusted padding
                        .align(Alignment.TopCenter), // Align top
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hallway",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "All",
                        fontSize = 20.sp, // Made smaller and clickable
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.clickable { viewModel.toggleAllMedia(true) }
                    )
                }

                // Main content row (Card Stack + Info)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 100.dp, bottom = 80.dp) // Leave space for header/footer
                ) {
                     // Card stack (left side ~60% of screen)
                    Box(
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight()
                            .padding(start = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CardStack(
                            cards = cards,
                            selectedIndex = selectedIndex,
                            onCardSelected = { hallwayViewModel.selectCard(it) }
                        )
                    }

                    // Side info panel (right side ~40% of screen)
                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight()
                            .padding(horizontal = 12.dp)
                    ) {
                        // Selected card info
                        if (cards.isNotEmpty() && selectedIndex in cards.indices) {
                            val selectedCard = cards[selectedIndex]
                            
                            Spacer(modifier = Modifier.height(60.dp))
                            
                            Text(
                                text = selectedCard.title,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Time Capsule: ${selectedCard.timeCapsuleDays} Days",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Description",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = selectedCard.description, // Ensure HallwayCard has description or use placeholder
                                fontSize = 11.sp,
                                color = Color(0xFFAAAAAA),
                                lineHeight = 14.sp,
                                maxLines = 4
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "> Full Description",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.clickable { /* Handle full description */ }
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Explore More section
                        Text(
                            text = "Explore More",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ExploreItem(text = "> New York City")
                        Spacer(modifier = Modifier.height(8.dp))
                        ExploreItem(text = "> Daily Trip")
                        
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Bottom navigation
                BottomNavigation(
                    currentTab = currentTab,
                    onTabSelected = { hallwayViewModel.selectTab(it) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars) // Added safe area padding
                        .padding(bottom = 16.dp) // Extra spacing to be "a little bit upper"
                )

                // Orb menu (Side bar) - New orbital layout
                OrbMenu(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onAddClick = { viewModel.startCreateRoom(RoomCreationSource.HALLWAY) },
                    onProfileClick = { viewModel.openProfileMenu() },
                    onSearchClick = { /* Search is empty for now */ },
                    onChatClick = { /* Chat icon only */ }
                )
            }
        }

        // 2. ALL SECTION (Picture Gallery)
        AnimatedVisibility(
            visible = isAllMediaVisible && currentStep == ProfileStep.NONE,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp, top = 32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.toggleAllMedia(false) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = "All Memories",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(30) { index ->
                        coil.compose.AsyncImage(
                            model = "https://picsum.photos/seed/${index}/200/300",
                            contentDescription = "Memory $index",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(0.7f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.DarkGray)
                        )
                    }
                }
            }
        }

        // 3. OVERLAYS (Room Creation, Profile, etc.)
        AnimatedVisibility(
            visible = currentStep != ProfileStep.NONE,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
            ) {
                when (currentStep) {
                    ProfileStep.PROFILE_MENU -> ProfileMenuScreen(viewModel, createdRooms, onLogout)
                    ProfileStep.ROOM_NAME         -> CreateRoomStep1(viewModel)
                    ProfileStep.ROOM_TIME_CAPSULE -> CreateRoomStep2(viewModel)
                    ProfileStep.ROOM_ATMOSPHERE   -> CreateRoomStep3Atmosphere(viewModel)
                    ProfileStep.ROOM_MEMORY_RULES -> CreateRoomStep4(viewModel)
                    ProfileStep.ROOM_INVITE       -> CreateRoomStep5(viewModel)

                    ProfileStep.TIME_CAPSULE_LIST -> TimeCapsuleListScreen(viewModel, createdRooms)
                    ProfileStep.ROOM_DETAIL -> RoomDetailScreen(
                        viewModel = viewModel,
                        multiplePhotoPickerLauncher = multiplePhotoPickerLauncher,
                        filePickerLauncher = filePickerLauncher
                    )
                    ProfileStep.EDIT_ROOM -> EditRoomScreen(viewModel)
                    else -> {}
                }
            }
        }
    }
}


// --- REFACTORED PROFILE MENU (iOS Tab Style) ---

@Composable
private fun ProfileMenuScreen(
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
@Composable
private fun DashboardTab(
    viewModel: ProfileViewModel,
    createdRooms: List<TimeCapsuleRoom>
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
                        .height(100.dp)
                        .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                         Icon(Icons.Default.Home, null, tint = Color(0xFF007AFF), modifier = Modifier.size(24.dp))
                         Spacer(modifier = Modifier.height(8.dp))
                         Text("3", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                         Text("My Rooms", color = Color.Gray, fontSize = 12.sp)
                    }
                }
                 // Memories Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                        .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                         Icon(Icons.Default.Image, null, tint = Color(0xFFAF52DE), modifier = Modifier.size(24.dp))
                         Spacer(modifier = Modifier.height(8.dp))
                         Text("6", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                         Text("Memories", color = Color.Gray, fontSize = 12.sp)
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
                                         viewModel.deleteRoom(context, room)
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
private fun ActivityItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, date: String) {
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
private fun CustomizationTab(viewModel: ProfileViewModel) {
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
private fun SettingsTab(viewModel: ProfileViewModel, onLogout: () -> Unit) {
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


// --- EDIT ROOM SCREEN ---
@Composable
private fun EditRoomScreen(viewModel: ProfileViewModel) {
    // Reference: "Edit Room" Screenshot
    val roomName by viewModel.roomName.collectAsState()
    val isPublic by viewModel.isPublic.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit Room", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Box(
                    modifier = Modifier.size(32.dp).background(Color(0xFF2C2C2E), CircleShape).clickable { viewModel.goBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
            
            // Room Name
            Text("Room Name", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            BasicTextField(
                value = roomName,
                onValueChange = { viewModel.updateRoomName(it) },
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1C1C1E), RoundedCornerShape(10.dp))
                    .padding(16.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Privacy Status
            Text("Privacy Status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Public
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                        .border(if(isPublic) 2.dp else 0.dp, if(isPublic) Color(0xFF007AFF) else Color.Transparent, RoundedCornerShape(12.dp))
                        .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp))
                        .clickable { viewModel.updateRoomMode(true) }
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.Public, null, tint = Color.White)
                        Column {
                            Text("Public", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Anyone can see.", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }
                
                // Private
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                        .border(if(!isPublic) 2.dp else 0.dp, if(!isPublic) Color(0xFF007AFF) else Color.Transparent, RoundedCornerShape(12.dp))
                        .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp))
                        .clickable { viewModel.updateRoomMode(false) }
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.Lock, null, tint = Color.Gray)
                        Column {
                            Text("Private", color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("Only invited.", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Enable Time Capsule switch
            Row(
                 modifier = Modifier.fillMaxWidth().background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp)).padding(16.dp),
                 horizontalArrangement = Arrangement.SpaceBetween,
                 verticalAlignment = Alignment.CenterVertically
            ) {
                 Column {
                     Text("Enable Time Capsule", color = Color.White, fontWeight = FontWeight.Bold)
                     Text("Content hidden until timer ends.", color = Color.Gray, fontSize = 12.sp)
                 }
                 Switch(checked = false, onCheckedChange = {})
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Atmosphere
            Text("Atmosphere", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
             Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp))
                    .padding(vertical = 8.dp) 
            ) {
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
                 Row(modifier = Modifier.padding(16.dp)) {
                      Icon(Icons.Default.MusicNote, null, tint = Color.Gray)
                      Spacer(modifier = Modifier.width(16.dp))
                      Text("Loft Beats", color = Color.Gray, fontWeight = FontWeight.Bold)
                 }
                 Row(modifier = Modifier.padding(16.dp)) {
                      Icon(Icons.Default.Eco, null, tint = Color.Gray)
                      Spacer(modifier = Modifier.width(16.dp))
                      Text("Nature Ambience", color = Color.Gray, fontWeight = FontWeight.Bold)
                 }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Save Button
            Button(
                onClick = { viewModel.goBack() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp).padding(bottom=24.dp)
            ) {
                Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// --- EXISTING HELPERS ---

@Composable
fun CardStack(
    cards: List<HallwayCard>,
    selectedIndex: Int,
    onCardSelected: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (cards.isEmpty()) {
            Text("No cards available", color = Color.White, modifier = Modifier.align(Alignment.Center))
        } else {
            cards.forEachIndexed { index, card ->

                
                // Simplified Stack Logic for MVP visual
                if (index == selectedIndex) {
                    // Active card
                    Box(
                        modifier = Modifier
                            .offset(y = 0.dp)
                            .scale(1f)
                            .zIndex(100f)
                            .fillMaxWidth(0.8f)
                            .aspectRatio(0.7f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.DarkGray)
                            .clickable { onCardSelected(index) }
                    ) {
                        coil.compose.AsyncImage(
                            model = card.imageUrl,
                            contentDescription = card.title,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } 
            }
        }
    }
}

@Composable
fun BottomNavigation(
    currentTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
     Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
         // Consistent with HallwayScreen navigation structure
         Icon(Icons.Default.Circle, null, tint = if(currentTab == BottomTab.ROOMING) Color.White else Color.Gray, modifier = Modifier.clickable { onTabSelected(BottomTab.ROOMING) })
         Icon(Icons.Default.Menu, null, tint = if(currentTab == BottomTab.EVERYONE) Color.White else Color.Gray, modifier = Modifier.clickable { onTabSelected(BottomTab.EVERYONE) })
         Icon(Icons.Default.Person, null, tint = if(currentTab == BottomTab.ARTISTS) Color.White else Color.Gray, modifier = Modifier.clickable { onTabSelected(BottomTab.ARTISTS) })
    }
}

@Composable
fun ExploreItem(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        color = Color.Gray,
        modifier = Modifier.clickable { }
    )
}

// ─────────────────────────────────────────────────────────────
//  SHARED CHROME: title bar + 5-dot progress + bottom buttons
// ─────────────────────────────────────────────────────────────

private val CardDarkBg = Color(0xFF1C1C1E)
private val AccentBlue  = Color(0xFF1A7AF8)

@Composable
private fun CreateRoomChrome(
    step: Int,           // 1-based, 1..4
    onDismiss: () -> Unit,
    onBack: (() -> Unit)?,  // null = hide Back button
    onNext: () -> Unit,
    nextLabel: String = "Next",
    nextEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Header row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Create Room",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF2C2C2E), androidx.compose.foundation.shape.CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── 5-dot step indicator ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..5) {
                    val filled = i <= step
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (filled) AccentBlue else Color(0xFF3A3A3C),
                                androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    if (i < 5) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(
                                    if (i < step) AccentBlue else Color(0xFF3A3A3C)
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Body content ──
            Column(modifier = Modifier.weight(1f)) {
                content()
            }

            // ── Bottom buttons ──
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = if (onBack != null) Arrangement.spacedBy(12.dp)
                                        else Arrangement.Center
            ) {
                if (onBack != null) {
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E))
                    ) {
                        Text("Back", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
                Button(
                    onClick = onNext,
                    enabled = nextEnabled,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (onBack != null) AccentBlue else Color(0xFF3A3A3C),
                        disabledContainerColor = Color(0xFF3A3A3C)
                    )
                ) {
                    Text(nextLabel, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  STEP 1 — What's the name? + Public/Private
// ─────────────────────────────────────────────────────────────
@Composable
private fun CreateRoomStep1(viewModel: ProfileViewModel) {
    val roomName by viewModel.roomName.collectAsState()
    val isPublic by viewModel.isPublic.collectAsState()

    CreateRoomChrome(
        step = 1,
        onDismiss = { viewModel.closeOverlay() },
        onBack = null,
        onNext = {
            if (roomName.isNotBlank()) viewModel.goToStep(ProfileStep.ROOM_TIME_CAPSULE)
        },
        nextEnabled = roomName.isNotBlank()
    ) {
        Text(
            text = "What's the name?",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Give your room a unique title.",
            color = Color(0xFF8E8E93),
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Room Name field
        BasicTextField(
            value = roomName,
            onValueChange = { viewModel.updateRoomName(it) },
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            cursorBrush = SolidColor(Color.White),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .background(CardDarkBg, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 18.dp),
            decorationBox = { inner ->
                if (roomName.isEmpty()) Text("Room Name", color = Color(0xFF636366), fontSize = 16.sp)
                inner()
            }
        )

        Spacer(modifier = Modifier.height(28.dp))
        Text("Privacy Status", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        // Public / Private cards
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            PrivacyCard(
                title = "Public",
                subtitle = "Anyone can see and join.",
                iconVector = Icons.Default.Language,
                selected = isPublic,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.updateRoomMode(true) }
            )
            PrivacyCard(
                title = "Private",
                subtitle = "Only visible to invited.",
                iconVector = Icons.Default.Lock,
                selected = !isPublic,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.updateRoomMode(false) }
            )
        }
    }
}

@Composable
private fun PrivacyCard(
    title: String,
    subtitle: String,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(100.dp)
            .background(CardDarkBg, RoundedCornerShape(14.dp))
            .then(
                if (selected) Modifier.border(2.dp, AccentBlue, RoundedCornerShape(14.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            Icon(iconVector, null, tint = if (selected) Color.White else Color(0xFF8E8E93), modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = if (selected) Color.White else Color(0xFF8E8E93), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, color = Color(0xFF8E8E93), fontSize = 11.sp, lineHeight = 14.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  STEP 2 — Time Capsule? (Duration + Specific Date tabs)
// ─────────────────────────────────────────────────────────────
@Composable
private fun CreateRoomStep2(viewModel: ProfileViewModel) {
    val enabled    by viewModel.isTimeCapsuleEnabled.collectAsState()
    val method     by viewModel.unlockMethod.collectAsState()
    val days       by viewModel.capsuleDays.collectAsState()
    val hours      by viewModel.capsuleHours.collectAsState()
    val mins       by viewModel.capsuleMins.collectAsState()
    val preset     by viewModel.selectedPreset.collectAsState()
    val targetTime by viewModel.targetTime.collectAsState()
    val targetHour by viewModel.targetHour.collectAsState()
    val targetMin  by viewModel.targetMinute.collectAsState()

    CreateRoomChrome(
        step = 2,
        onDismiss = { viewModel.closeOverlay() },
        onBack = { viewModel.goToStep(ProfileStep.ROOM_NAME) },
        onNext = { viewModel.goToStep(ProfileStep.ROOM_ATMOSPHERE) }
    ) {
        androidx.compose.foundation.rememberScrollState().let { scroll ->
            Column(modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)) {

                Text("Time Capsule?", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Lock memories for a future date.", color = Color(0xFF8E8E93), fontSize = 14.sp)
                Spacer(modifier= Modifier.height(16.dp))

                // Enable toggle row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardDarkBg, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Time Capsule", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Content will be hidden until the timer ends.", color = Color(0xFF8E8E93), fontSize = 12.sp)
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { viewModel.updateTimeCapsuleEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentBlue
                        )
                    )
                }

                if (enabled) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Unlock Method", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Duration / Specific Date segmented control
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1C1C1E), RoundedCornerShape(10.dp))
                            .padding(4.dp)
                    ) {
                        listOf(UnlockMethod.DURATION to "Duration", UnlockMethod.SPECIFIC_DATE to "Specific Date").forEach { (m, label) ->
                            val sel = method == m
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (sel) Color(0xFF3A3A3C) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.updateUnlockMethod(m) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = Color.White, fontSize = 14.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (method == UnlockMethod.DURATION) {
                        // ── Duration picker ──
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardDarkBg, RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            DurationStepper("Days",  days,  { viewModel.updateCapsuleDays(days - 1)  }, { viewModel.updateCapsuleDays(days + 1)  })
                            DurationStepper("Hours", hours, { viewModel.updateCapsuleHours(hours - 1) }, { viewModel.updateCapsuleHours(hours + 1) })
                            DurationStepper("Mins",  mins,  { viewModel.updateCapsuleMins(mins - 1)  }, { viewModel.updateCapsuleMins(mins + 1)  })
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "${days}d ${hours}h ${mins}m",
                            color = AccentBlue,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Preset chips
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("1 Week", "21 Days", "1 Month").forEach { p ->
                                val sel = preset == p
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (sel) AccentBlue else Color(0xFF2C2C2E),
                                            RoundedCornerShape(20.dp)
                                        )
                                        .clickable { viewModel.selectPreset(p) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(p, color = Color.White, fontSize = 14.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    } else {
                        // ── Specific Date inline calendar ──
                        InlineCalendar(
                            selectedMillis = targetTime,
                            hour = targetHour,
                            minute = targetMin,
                            onDateSelected = { viewModel.updateTargetDate(it) },
                            onTimeChanged = { h, m -> viewModel.updateTargetTime(h, m) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DurationStepper(
    label: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Color(0xFF8E8E93), fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Color(0xFF3A3A3C), androidx.compose.foundation.shape.CircleShape)
                    .clickable { onDecrement() },
                contentAlignment = Alignment.Center
            ) { Text("−", color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center) }
            Text(value.toString(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Color(0xFF3A3A3C), androidx.compose.foundation.shape.CircleShape)
                    .clickable { onIncrement() },
                contentAlignment = Alignment.Center
            ) { Text("+", color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center) }
        }
    }
}

@Composable
private fun InlineCalendar(
    selectedMillis: Long,
    hour: Int,
    minute: Int,
    onDateSelected: (Long) -> Unit,
    onTimeChanged: (Int, Int) -> Unit
) {
    val todayMillis = remember {
        val c = java.util.Calendar.getInstance()
        c.set(java.util.Calendar.HOUR_OF_DAY, 0)
        c.set(java.util.Calendar.MINUTE, 0)
        c.set(java.util.Calendar.SECOND, 0)
        c.set(java.util.Calendar.MILLISECOND, 0)
        c.timeInMillis
    }

    var displayMonthMillis by remember { mutableStateOf(
        run {
            val c = java.util.Calendar.getInstance()
            c.set(java.util.Calendar.DAY_OF_MONTH, 1)
            c.set(java.util.Calendar.HOUR_OF_DAY, 0); c.set(java.util.Calendar.MINUTE, 0)
            c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0)
            c.timeInMillis
        }
    ) }

    val monthCal = remember(displayMonthMillis) {
        java.util.Calendar.getInstance().apply { timeInMillis = displayMonthMillis }
    }

    val monthName = remember(displayMonthMillis) {
        java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(displayMonthMillis))
    }

    val selectedDayMillis = remember(selectedMillis) {
        val c = java.util.Calendar.getInstance(); c.timeInMillis = selectedMillis
        c.set(java.util.Calendar.HOUR_OF_DAY, 0); c.set(java.util.Calendar.MINUTE, 0)
        c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0)
        c.timeInMillis
    }

    // Build day grid
    val daysInMonth = monthCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = monthCal.get(java.util.Calendar.DAY_OF_WEEK) // 1=Sun..7=Sat
    // Convert to Mon-based offset: Mon=0..Sun=6
    val startOffset = (firstDayOfWeek - 2 + 7) % 7

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardDarkBg, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            // Month navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(monthName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ChevronRight, null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                }
                Row {
                    IconButton(onClick = {
                        val c = java.util.Calendar.getInstance(); c.timeInMillis = displayMonthMillis
                        c.add(java.util.Calendar.MONTH, -1); displayMonthMillis = c.timeInMillis
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ChevronLeft, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = {
                        val c = java.util.Calendar.getInstance(); c.timeInMillis = displayMonthMillis
                        c.add(java.util.Calendar.MONTH, 1); displayMonthMillis = c.timeInMillis
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ChevronRight, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Day-of-week headers (Mon..Sun)
            val dayHeaders = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
            Row(modifier = Modifier.fillMaxWidth()) {
                dayHeaders.forEach { header ->
                    Text(header, color = Color(0xFF8E8E93), fontSize = 11.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Day cells
            val totalCells = startOffset + daysInMonth
            val rows = (totalCells + 6) / 7
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNum = cellIndex - startOffset + 1
                        val valid = dayNum in 1..daysInMonth
                        if (valid) {
                            val dayCal = java.util.Calendar.getInstance().apply {
                                timeInMillis = displayMonthMillis
                                set(java.util.Calendar.DAY_OF_MONTH, dayNum)
                            }
                            val dayMillis = run {
                                val c2 = dayCal.clone() as java.util.Calendar
                                c2.set(java.util.Calendar.HOUR_OF_DAY, 0); c2.set(java.util.Calendar.MINUTE, 0)
                                c2.set(java.util.Calendar.SECOND, 0); c2.set(java.util.Calendar.MILLISECOND, 0)
                                c2.timeInMillis
                            }
                            val isSelected = dayMillis == selectedDayMillis
                            val isToday = dayMillis == todayMillis
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .then(
                                        if (isSelected) Modifier.background(AccentBlue, androidx.compose.foundation.shape.CircleShape)
                                        else Modifier
                                    )
                                    .clickable { onDateSelected(dayCal.timeInMillis) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    color = when {
                                        isSelected -> Color.White
                                        isToday -> AccentBlue
                                        else -> Color.White
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Time picker row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Time", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // — Hour spinner —
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF3A3A3C), androidx.compose.foundation.shape.CircleShape)
                                .clickable { onTimeChanged((hour + 1) % 24, minute) },
                            contentAlignment = Alignment.Center
                        ) { Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(String.format("%02d", hour), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF3A3A3C), androidx.compose.foundation.shape.CircleShape)
                                .clickable { onTimeChanged((hour - 1 + 24) % 24, minute) },
                            contentAlignment = Alignment.Center
                        ) { Text("-", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                    }

                    Text(":", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)

                    // — Minute spinner —
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF3A3A3C), androidx.compose.foundation.shape.CircleShape)
                                .clickable { onTimeChanged(hour, (minute + 5) % 60) },
                            contentAlignment = Alignment.Center
                        ) { Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(String.format("%02d", minute), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF3A3A3C), androidx.compose.foundation.shape.CircleShape)
                                .clickable { onTimeChanged(hour, (minute - 5 + 60) % 60) },
                            contentAlignment = Alignment.Center
                        ) { Text("-", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  STEP 3 — Atmosphere (Room Theme + Background Music)
// ─────────────────────────────────────────────────────────────
@Composable
private fun CreateRoomStep3Atmosphere(viewModel: ProfileViewModel) {
    val selectedTheme by viewModel.roomAtmosphereTheme.collectAsState()
    val selectedMusic by viewModel.selectedMusic.collectAsState()

    // Theme data: name → background color
    val themes = listOf(
        "Default"   to Color(0xFF1A7AF8),
        "Ocean"     to Color(0xFF00C6A2),
        "Sunset"    to Color(0xFFE8820C),
        "Forest"    to Color(0xFF22A84A),
        "Cyberpunk" to Color(0xFFAA3FD6)
    )

    // Music options: icon emoji + label
    data class MusicOption(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String)
    val musicOptions = listOf(
        MusicOption(Icons.Default.VolumeOff,   "None"),
        MusicOption(Icons.Default.MusicNote,   "Lofi Beats"),
        MusicOption(Icons.Default.Star,        "Nature Ambience"),
        MusicOption(Icons.Default.Favorite,    "Minimal Piano")
    )

    CreateRoomChrome(
        step = 3,
        onDismiss = { viewModel.closeOverlay() },
        onBack = { viewModel.goToStep(ProfileStep.ROOM_TIME_CAPSULE) },
        onNext = { viewModel.goToStep(ProfileStep.ROOM_MEMORY_RULES) }
    ) {
        Text("Atmosphere", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Set the mood with background music.", color = Color(0xFF8E8E93), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))

        // ── Room Theme ──
        Text("Room Theme", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            themes.forEach { (name, color) ->
                val isSelected = selectedTheme == name
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.updateSelectedTheme(name) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .then(
                                if (isSelected)
                                    Modifier.border(2.5.dp, Color.White, androidx.compose.foundation.shape.CircleShape)
                                else Modifier
                            )
                            .padding(if (isSelected) 3.dp else 0.dp)
                            .background(color, androidx.compose.foundation.shape.CircleShape)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = name,
                        color = if (isSelected) Color.White else Color(0xFF8E8E93),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Background Music ──
        Text("Background Music", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            musicOptions.forEach { option ->
                val isSelected = selectedMusic == option.label
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) Color(0xFF0D2B5E) else CardDarkBg,
                            RoundedCornerShape(14.dp)
                        )
                        .then(
                            if (isSelected)
                                Modifier.border(1.5.dp, AccentBlue, RoundedCornerShape(14.dp))
                            else Modifier
                        )
                        .clickable { viewModel.updateSelectedMusic(option.label) }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = option.label,
                            tint = if (isSelected) AccentBlue else Color(0xFF8E8E93),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = option.label,
                            color = if (isSelected) Color.White else Color(0xFF8E8E93),
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  STEP 4 — Memory Dump Rules
// ─────────────────────────────────────────────────────────────
@Composable
private fun CreateRoomStep4(viewModel: ProfileViewModel) {
    val rolling  by viewModel.rollingExpiration.collectAsState()
    val closure  by viewModel.scheduledClosureEnabled.collectAsState()

    CreateRoomChrome(
        step = 4,
        onDismiss = { viewModel.closeOverlay() },
        onBack = { viewModel.goToStep(ProfileStep.ROOM_ATMOSPHERE) },
        onNext = { viewModel.goToStep(ProfileStep.ROOM_INVITE) }
    ) {
        Text("Memory Dump Rules", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Configure auto-archival for drops.", color = Color(0xFF8E8E93), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(20.dp))

        // Rolling Expiration card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardDarkBg, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Text("Rolling Expiration (Snapchat Mode)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Automatically archive memories X days after they are posted.",
                color = Color(0xFF8E8E93), fontSize = 12.sp, lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2C2C2E), RoundedCornerShape(22.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Never", "1 Day (24...)", "7 Days", "30 Days").forEach { option ->
                    val sel = rolling == option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (sel) Color.White else Color.Transparent,
                                RoundedCornerShape(18.dp)
                            )
                            .clickable { viewModel.updateRollingExpiration(option) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            option,
                            color = if (sel) Color.Black else Color(0xFF8E8E93),
                            fontSize = 12.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scheduled Room Closure card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardDarkBg, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Scheduled Room Closure", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    "Lock the entire room into a read-only archive state after a specific date.",
                    color = Color(0xFF8E8E93), fontSize = 12.sp, lineHeight = 16.sp
                )
            }
            Switch(
                checked = closure,
                onCheckedChange = { viewModel.updateScheduledClosure(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AccentBlue
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  STEP 5 — Invite Friends
// ─────────────────────────────────────────────────────────────
@Composable
private fun CreateRoomStep5(viewModel: ProfileViewModel) {
    val emails by viewModel.inviteEmails.collectAsState()
    var emailInput by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    CreateRoomChrome(
        step = 5,
        onDismiss = { viewModel.closeOverlay() },
        onBack = { viewModel.goToStep(ProfileStep.ROOM_MEMORY_RULES) },
        onNext = { viewModel.finalizeRoom(context) },
        nextLabel = "Create Room"
    ) {
        Text("Invite Friends", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Add people and assign them roles (Viewer or Contributor).", color = Color(0xFF8E8E93), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(20.dp))

        // Email input + add button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BasicTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                cursorBrush = SolidColor(Color.White),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .background(CardDarkBg, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                decorationBox = { inner ->
                    if (emailInput.isEmpty()) Text("Friend's Email Address", color = Color(0xFF636366), fontSize = 15.sp)
                    inner()
                }
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF3A3A3C), androidx.compose.foundation.shape.CircleShape)
                    .clickable {
                        if (emailInput.isNotBlank()) {
                            viewModel.addInviteEmail(emailInput.trim())
                            emailInput = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        // Added emails list
        if (emails.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                emails.forEach { email ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardDarkBg, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, null, tint = Color(0xFF8E8E93), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(email, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Default.Close, null, tint = Color(0xFF8E8E93), modifier = Modifier
                                .size(18.dp)
                                .clickable { viewModel.removeInviteEmail(email) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  LEGACY HELPERS (kept so old OverlayStepContainer callers compile)
// ─────────────────────────────────────────────────────────────
@Composable
private fun RoomNameStep(viewModel: ProfileViewModel) {
    CreateRoomStep1(viewModel)
}
@Composable
private fun RoomTimeStep(viewModel: ProfileViewModel) {
    CreateRoomStep2(viewModel)
}
@Composable
private fun RoomModeStep(viewModel: ProfileViewModel) {
    CreateRoomStep1(viewModel)
}
// Kept as alias for legacy callers
@Composable
private fun CreateRoomStep3(viewModel: ProfileViewModel) {
    CreateRoomStep4(viewModel)
}
@Composable
private fun CreateRoomStep4Alias(viewModel: ProfileViewModel) {
    CreateRoomStep5(viewModel)
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        text = content
    )
}


@Composable
private fun TimeCapsuleListScreen(
    viewModel: ProfileViewModel,
    createdRooms: List<TimeCapsuleRoom>
) {
    val showOnlySaved by viewModel.showOnlySaved.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
         Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
             Row(verticalAlignment = Alignment.CenterVertically) {
                 IconButton(onClick = { viewModel.closeOverlay() }) {
                     Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                 }
                 Text(
                     if(showOnlySaved) "Saved Rooms" else "My Time Capsules", 
                     color = Color.White, 
                     fontSize = 20.sp, 
                     fontWeight = FontWeight.Bold
                 )
             }
             
             LazyColumn(
                 contentPadding = PaddingValues(vertical = 16.dp),
                 verticalArrangement = Arrangement.spacedBy(16.dp)
             ) {
                 items(createdRooms) { room ->
                     Card(
                        colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
                        modifier = Modifier.fillMaxWidth().height(80.dp).clickable { viewModel.selectRoom(room) }
                     ) {
                         Row(
                             modifier = Modifier.fillMaxSize().padding(16.dp),
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             Column {
                                 Text(room.roomName, color = Color.White, fontWeight = FontWeight.Bold)
                                 Text("Unlocks: ${java.text.SimpleDateFormat("MMM dd HH:mm").format(java.util.Date(room.unlockTime))}", color = Color.Gray, fontSize = 12.sp)
                             }
                         }
                     }
                 }
             }
         }
    }
}



@Composable
private fun ModeOption(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) Color.White else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (selected) Color.White else Color.Gray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            color = if (selected) Color.White else Color.Gray,
            fontSize = 16.sp
        )
    }
}


@Composable
fun OverlayStepContainer(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onNext: () -> Unit,
    nextLabel: String = "Next",
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            TextButton(onClick = onNext) {
                Text(nextLabel, color = Color.White, fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = title,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = subtitle,
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        content()
    }
}

// --- ROOM DETAIL SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailScreen(
    viewModel: ProfileViewModel,
    multiplePhotoPickerLauncher: androidx.activity.result.ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest>,
    filePickerLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
) {
    val room by viewModel.selectedRoom.collectAsState()
    val roomMedia by viewModel.roomMedia.collectAsState() // Persisted room media
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // UI State
    var showQrCode by remember { mutableStateOf(false) }
    var show3DotMenu by remember { mutableStateOf(false) }
    var showRoomDetailsSheet by remember { mutableStateOf(false) }
    var showWriteNoteDialog by remember { mutableStateOf(false) }
    var noteContent by remember { mutableStateOf("") }
    
    // Combine room-specific media
    // Combine room-specific media
    val currentRoomMedia = roomMedia[room?.id] ?: emptyList()
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (room == null) {
            Text("Room not found", color = Color.White, modifier = Modifier.align(Alignment.Center))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 24.dp)
            ) {
                // Header: Back, Title, Grid/Menu Icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { viewModel.goBack() }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        Text("Back", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         // Grid Icon Removed
                         
                         // QR Code Button
                         Icon(
                             Icons.Default.QrCode2,
                             "QR Code",
                             tint = Color.White,
                             modifier = Modifier
                                 .size(24.dp)
                                 .clickable { showQrCode = true }
                         )
                         Spacer(modifier = Modifier.width(16.dp))
                         
                         // 3-Dot Menu
                         Box {
                             Icon(
                                 Icons.Default.MoreHoriz,
                                 "Menu",
                                 tint = Color.White,
                                 modifier = Modifier
                                     .size(24.dp)
                                     .clickable { show3DotMenu = true }
                             )
                             
                             DropdownMenu(
                                 expanded = show3DotMenu,
                                 onDismissRequest = { show3DotMenu = false },
                                 modifier = Modifier.background(Color(0xFF2C2C2E))
                             ) {
                                 DropdownMenuItem(
                                     text = { Text("Room Details", color = Color.White) },
                                     onClick = {
                                         show3DotMenu = false
                                         showRoomDetailsSheet = true
                                     }
                                 )
                                 DropdownMenuItem(
                                     text = { Text("Edit Room", color = Color.White) },
                                     onClick = {
                                         show3DotMenu = false
                                         viewModel.goToStep(ProfileStep.EDIT_ROOM)
                                     }
                                 )
                                 DropdownMenuItem(
                                     text = { Text("Delete Room", color = Color.Red) },
                                     onClick = {
                                         show3DotMenu = false
                                         room?.let { viewModel.deleteRoom(context, it) }
                                     }
                                 )
                             }
                         }
                     }
                }
                
                // Room Title
                Text(
                    text = room!!.roomName,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 32.dp)
                )
                
                Text(
                    text = "Memories",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Grid of Actions
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    userScrollEnabled = false,
                    modifier = Modifier.height(330.dp) // Increased height for 3 rows of buttons
                ) {
                    // Item 1: Add Photo (Blue)
                    item { MemoryActionCard(Icons.Default.Image, "Add Photo", Color(0xFF007AFF)) {
                         multiplePhotoPickerLauncher.launch(
                             androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                         )
                    }}
                    // Item 2: Write Note (Purple)
                    item { MemoryActionCard(Icons.Default.Edit, "Write Note", Color(0xFFAF52DE)) { 
                        showWriteNoteDialog = true
                    }}
                    // Item 3: Add Video (Orange)
                    item { MemoryActionCard(Icons.Default.Videocam, "Add Video", Color(0xFFFF9500)) { 
                        multiplePhotoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    }}
                    // Item 4: Add Music (Red)
                    item { MemoryActionCard(Icons.Default.MusicNote, "Add Music", Color(0xFFFF3B30)) { 
                        filePickerLauncher.launch(arrayOf("audio/*"))
                    }}
                    // Item 5: Record Audio (Pink/Red)
                    item { MemoryActionCard(Icons.Default.Mic, "Record Audio", Color(0xFFFF2D55)) { 
                        android.widget.Toast.makeText(context, "Audio recording feature coming soon!", android.widget.Toast.LENGTH_SHORT).show()
                    }}
                    // Item 6: View All (Green)
                    item { MemoryActionCard(Icons.Default.FolderOpen, "View All", Color(0xFF34C759)) { 
                        viewModel.openGalleryViewer(currentRoomMedia)
                    }}
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Recent Drops Section
                Text("Recent Drops", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (currentRoomMedia.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Inbox, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No drops yet. Be the first to add a memory!", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(currentRoomMedia) { uri ->
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.DarkGray)
                                    .clickable { 
                                        viewModel.openGalleryViewer(currentRoomMedia, currentRoomMedia.indexOf(uri))
                                    }
                            ) {
                                coil.compose.AsyncImage(
                                    model = uri,
                                    contentDescription = "Room Media",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Image Viewer Overlay
        val isGalleryOpen by viewModel.isGalleryViewerOpen.collectAsState()
        if (isGalleryOpen) {
             ProfileGalleryViewer(viewModel)
        }
        
        // QR Code Dialog
        if (showQrCode) {
            AlertDialog(
                onDismissRequest = { showQrCode = false },
                containerColor = Color(0xFF1C1C1E),
                title = { Text("Room QR Code", color = Color.White) },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Dummy QR Code visual representation
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .border(2.dp, Color(0xFF007AFF), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "QR ${room?.id?.take(8) ?: "CODE"}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Scan to join room: ${room?.roomName}",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showQrCode = false }) {
                        Text("Close", color = Color(0xFF007AFF))
                    }
                }
            )
        }
        
        // Write Note Dialog
        if (showWriteNoteDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showWriteNoteDialog = false
                    noteContent = ""
                },
                containerColor = Color(0xFF1C1C1E),
                title = { Text("Write a Note", color = Color.White) },
                text = {
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        placeholder = { Text("Write your memory here...", color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF007AFF),
                            unfocusedBorderColor = Color.Gray
                        ),
                        maxLines = 8
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (noteContent.isNotBlank()) {
                                // TODO: Save note to room via ViewModel
                                android.widget.Toast.makeText(context, "Note saved!", android.widget.Toast.LENGTH_SHORT).show()
                                showWriteNoteDialog = false
                                noteContent = ""
                            }
                        }
                    ) {
                        Text("Save", color = Color(0xFF007AFF))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showWriteNoteDialog = false
                        noteContent = ""
                    }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
        
        // Room Details Bottom Sheet
        if (showRoomDetailsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showRoomDetailsSheet = false },
                containerColor = Color(0xFF1C1C1E)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        "Room Details",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Room Name
                    DetailRow("Name", room?.roomName ?: "")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Room ID
                    DetailRow("Room ID", room?.id ?: "")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Unlock Time
                    DetailRow("Unlock Time", "${room?.capsuleMinutes ?: 0} minutes")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Privacy
                    DetailRow("Privacy", if (room?.isPublic == true) "Public" else "Private")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Created Date
                    val dateCreated = room?.dateCreated ?: 0L
                    val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    DetailRow("Created", if (dateCreated > 0) dateFormat.format(java.util.Date(dateCreated)) else "Unknown")
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}



@Composable
fun MemoryActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp) // Square-ish
            .background(
                Brush.radialGradient(
                     colors = listOf(color.copy(alpha = 0.2f), Color(0xFF1C1C1E)),
                     center = androidx.compose.ui.geometry.Offset.Unspecified,
                     radius = 200f
                ),
                RoundedCornerShape(16.dp)
            )
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(40.dp).background(color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, color = Color.White, fontSize = 14.sp)
        }
    }
}

// Simple Gallery Viewer Overlay
@Composable
fun ProfileGalleryViewer(viewModel: ProfileViewModel) {
    val media by viewModel.galleryViewerMedia.collectAsState()
    val startIndex by viewModel.galleryViewerIndex.collectAsState()
    
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = startIndex) { media.size }
    
    // Sync pager changes back to VM if needed, or just let it slide
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        androidx.compose.foundation.pager.HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
             coil.compose.AsyncImage(
                 model = media[page],
                 contentDescription = null,
                 contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                 modifier = Modifier.fillMaxSize()
             )
        }
        
        // Close Button
        IconButton(
            onClick = { viewModel.closeGalleryViewer() },
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        ) {
             Icon(Icons.Default.Close, null, tint = Color.White)
        }
    }
}

// Room Details Bottom Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomDetailsBottomSheet(
    room: TimeCapsuleRoom,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Room Details", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Details Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2C2C2E), RoundedCornerShape(12.dp))
                    .padding(20.dp)
            ) {
                // Room name
                Text(room.roomName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Unlock Date/Time
                val unlockDateText = if (room.unlockTime > 0) {
                    java.text.SimpleDateFormat("d MMM yyyy 'at' HH:mm", java.util.Locale.US).format(
                        java.util.Date(room.unlockTime)
                    )
                } else {
                    "No unlock time set"
                }
                
                Text(unlockDateText, color = Color.Gray, fontSize = 14.sp)
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// Helper component for Room Details bottom sheet
@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 16.sp)
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
