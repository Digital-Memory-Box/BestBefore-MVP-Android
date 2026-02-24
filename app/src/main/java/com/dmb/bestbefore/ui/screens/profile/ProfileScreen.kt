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
                    ProfileStep.ROOM_NAME -> RoomNameStep(viewModel)
                    ProfileStep.ROOM_TIME -> RoomTimeStep(viewModel)
                    ProfileStep.ROOM_MODE -> RoomModeStep(viewModel)

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

// Room Name Step
@Composable
private fun RoomNameStep(viewModel: ProfileViewModel) {
    val roomName by viewModel.roomName.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadUpcomingEvents(context)
    }

    OverlayStepContainer(
        title = "Create Room",
        subtitle = "Choose a Room name",
        onBack = { viewModel.closeOverlay() },
        onNext = {
            if (roomName.isNotEmpty()) {
                viewModel.goToStep(ProfileStep.ROOM_TIME)
            }
        }
    ) {
        BasicTextField(
            value = roomName,
            onValueChange = { viewModel.updateRoomName(it) },
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 18.sp
            ),
            cursorBrush = SolidColor(Color.White),
            singleLine = true, // Fix expansion on Enter
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White, RoundedCornerShape(12.dp))
                .padding(16.dp),
            decorationBox = { innerTextField ->
                if (roomName.isEmpty()) {
                    Text(
                        text = "Enter room name...",
                        color = Color.Gray,
                        fontSize = 18.sp
                    )
                }
                innerTextField()
            }
        )

        }
    }


// Room Time Step (Unified)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomTimeStep(viewModel: ProfileViewModel) {
    val targetTime by viewModel.targetTime.collectAsState()
    val targetHour by viewModel.targetHour.collectAsState()
    val targetMinute by viewModel.targetMinute.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = targetTime
    )
    
    val timeState = rememberTimePickerState(
        initialHour = targetHour,
        initialMinute = targetMinute
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { viewModel.updateTargetDate(it) }
                }) {
                    Text("OK")
                }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateTargetTime(timeState.hour, timeState.minute)
                }) {
                    Text("OK")
                }
            }
        ) {
            TimePicker(state = timeState)
        }
    }

    OverlayStepContainer(
        title = "Set Time",
        subtitle = "When should this capsule open?",
        onBack = { viewModel.goToStep(ProfileStep.ROOM_NAME) },
        onNext = { viewModel.goToStep(ProfileStep.ROOM_MODE) }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date Selection
             Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Select Date: ${java.text.SimpleDateFormat("MMM dd, yyyy").format(java.util.Date(targetTime))}")
            }
            // Time Selection
             Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Select Time: ${String.format("%02d:%02d", targetHour, targetMinute)}")
            }
        }
    }
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

// Room Mode Step
@Composable
private fun RoomModeStep(viewModel: ProfileViewModel) {
    val isPublic by viewModel.isPublic.collectAsState()

    OverlayStepContainer(
        title = "Room Privacy",
        subtitle = "Who can see this room?",
        onBack = { viewModel.goToStep(ProfileStep.ROOM_TIME) },
        onNext = { viewModel.finalizeRoom() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ModeOption(
                title = "Public",
                icon = Icons.Default.Public,
                selected = isPublic,
                onClick = { viewModel.updateRoomMode(true) }
            )
            ModeOption(
                title = "Private",
                icon = Icons.Default.Lock,
                selected = !isPublic,
                onClick = { viewModel.updateRoomMode(false) }
            )
        }
    }
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
