package com.dmb.bestbefore.ui.screens.profile

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.dmb.bestbefore.data.models.TimeCapsuleRoom
import com.dmb.bestbefore.ui.components.OrbMenu
import com.dmb.bestbefore.ui.components.OrbMenuSimple
import com.dmb.bestbefore.data.models.HallwayCard
import com.dmb.bestbefore.ui.screens.hallway.BottomTab
import com.dmb.bestbefore.ui.screens.hallway.HallwayViewModel

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

    val profileImageUri by viewModel.profileImageUri.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val isAllMediaVisible by viewModel.isAllMediaVisible.collectAsState()

    // Permission launchers
    val mediaPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions result
        val prefs = context.getSharedPreferences("BestBeforePrefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("media_permissions_requested", true).apply()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Permission result handled, continue with room creation
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
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
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
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
            val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.READ_MEDIA_IMAGES
            } else {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }
            galleryPermissionLauncher.launch(permission)
        }
        
        viewModel.onRequestFilePermission = {
            val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.READ_MEDIA_IMAGES
            } else {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }
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
    var cameraImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
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

    // Unlock Dialog State
    val showUnlockDialog by viewModel.showUnlockDialog.collectAsState()
    val unlockDialogRoom by viewModel.unlockDialogRoom.collectAsState()

    if (showUnlockDialog && unlockDialogRoom != null) {
        UnlockRoomDialog(
            room = unlockDialogRoom!!,
            onDismiss = { viewModel.dismissUnlockDialog() },
            onKeep = { viewModel.keepRoom(context, unlockDialogRoom!!) },
            onDelete = { viewModel.deleteRoom(context, unlockDialogRoom!!) }
        )
    }

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
                        .padding(horizontal = 24.dp, vertical = 48.dp) // Adjusted padding
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
                    modifier = Modifier.align(Alignment.BottomCenter)
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

        // 2. ALL SECTON (Picture Gallery)
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
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
                    ProfileStep.PROFILE_MENU -> ProfileMenuScreen(viewModel, createdRooms, onNavigateBack, onLogout)
                    ProfileStep.ROOM_NAME -> RoomNameStep(viewModel)
                    ProfileStep.ROOM_TIME -> RoomTimeStep(viewModel)
                    ProfileStep.ROOM_MODE -> RoomModeStep(viewModel)
                    ProfileStep.FINALIZE_PUBLIC, ProfileStep.FINALIZE_PRIVATE -> FinalizeStep(viewModel)
                    ProfileStep.COLLABORATION -> CollaborationStep(viewModel)
                    ProfileStep.TIME_CAPSULE_LIST -> TimeCapsuleListScreen(viewModel, createdRooms)
                    ProfileStep.ROOM_DETAIL -> RoomDetailScreen(
                        viewModel = viewModel,
                        multiplePhotoPickerLauncher = multiplePhotoPickerLauncher,
                        takePictureLauncher = takePictureLauncher,
                        filePickerLauncher = filePickerLauncher,
                        onCreateCameraUri = { uri -> cameraImageUri = uri }
                    )
                    else -> {}
                }
            }
        }
    }
}

// Unlock Room Dialog
@Composable
fun UnlockRoomDialog(
    room: TimeCapsuleRoom,
    onDismiss: () -> Unit,
    onKeep: () -> Unit,
    onDelete: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Time Capsule Unlocked!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Room: ${room.roomName}",
                    fontSize = 18.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Your time capsule is ready. Do you want to keep this room in your profile or delete it?",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Delete", color = Color.White)
                    }
                    Button(
                        onClick = onKeep,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save", color = Color.White)
                    }
                }
            }
        }
    }
}




// Separate Profile Menu Screen (Overlay)
@Composable
private fun ProfileMenuScreen(
    viewModel: ProfileViewModel,
    createdRooms: List<TimeCapsuleRoom>,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val profileImageUri by viewModel.profileImageUri.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.updateProfileImage(uri, context)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentPadding = PaddingValues(bottom = 100.dp) // Space for bottom nav
        ) {
            // Header: BestBefore + Wrench (Settings)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BestBefore",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.Build, // Wrench/Settings
                        contentDescription = "Settings",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Profile Card (Custom styled)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left Column: Text Info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Personal Profile > Edit Profile",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = userName,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Biography > See All Biography",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor...",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                maxLines = 3
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            // Action Icons Row
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Icon(Icons.Default.CameraAlt, "Camera", tint = Color.Gray)
                                Icon(Icons.Default.Close, "Close", tint = Color.Gray)
                                Icon(Icons.Default.PlayArrow, "Play", tint = Color.White)
                            }
                        }
                        
                        // Right Column: Image and Stats
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(start = 16.dp)
                        ) {
                            // Profile Image
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Gray)
                                    .clickable { 
                                        imagePickerLauncher.launch(
                                            androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                            ) {
                                if (profileImageUri != null) {
                                    coil.compose.AsyncImage(
                                        model = profileImageUri,
                                        contentDescription = "Profile Image",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Rooming: 108",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Roomers: 19",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Action Buttons List
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileMenuButton(Icons.Default.AccessTime, "Time Capsule") { viewModel.showTimeCapsuleList() }
                    ProfileMenuButton(Icons.Default.Share, "Collaborations") { /* TODO */ }
                    ProfileMenuButton(Icons.Default.Add, "Create a new room", isGreenPlus = true) { viewModel.startCreateRoom(RoomCreationSource.PROFILE_MENU) }
                    ProfileMenuButton(Icons.Default.Save, "Saved rooms") { /* TODO */ }
                    ProfileMenuButton(Icons.Default.Upgrade, "Share your profile") { /* TODO */ } // Use Upgrade/Upload icon
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Recent Rooms Section
            item {
                Text(
                    text = "${createdRooms.size} Rooms > See All Your Rooms",
                    fontSize = 18.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                if (createdRooms.isEmpty()) {
                    Text("No rooms created yet.", color = Color.Gray, fontSize = 14.sp)
                } else {
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    ) {
                        items(createdRooms) { room ->
                            Box(
                                modifier = Modifier
                                    .width(200.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.DarkGray)
                                    .clickable { viewModel.selectRoom(room) }
                            ) {
                                // Background Image Placeholder
                                coil.compose.AsyncImage(
                                    model = "https://picsum.photos/seed/${room.id}/400/200",
                                    contentDescription = "Room Cover",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                
                                // Overlay gradient for text readability
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                            )
                                        )
                                )

                                // Text Overlay
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Text(
                                        text = room.roomName,
                                        color = Color.White, // Adjusted for readability over image
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }



            // Logout Button
            item {
                Button(
                    onClick = { 
                        viewModel.logout(context)
                        onLogout() 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Log Out", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // Bottom Navigation (Mock for Profile Screen)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black) // Opaque background
                .windowInsetsPadding(WindowInsets.navigationBars) // Add padding for nav bar
                .padding(bottom = 24.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
             Text("Rooming", color = Color.Gray, fontSize = 16.sp)
             Text(
                 "Hallway", 
                 color = Color.Gray, 
                 fontSize = 16.sp, 
                 modifier = Modifier.clickable { viewModel.closeOverlay() } // Go back to Hallway
             )
             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                 Text("▽", color = Color.White, fontSize = 10.sp)
                 Text("Profile", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
             }
        }

        // Orb Menu Overlap
        OrbMenu(
            modifier = Modifier.align(Alignment.CenterEnd),
            onAddClick = { viewModel.startCreateRoom() },
            onProfileClick = { /* Already on Profile */ },
            onSearchClick = { /* Search */ },
            onChatClick = { /* Chat */ }
        )
    }
}

@Composable
private fun ProfileMenuButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isGreenPlus: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isGreenPlus) Color.Green else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            fontSize = 18.sp,
            color = Color.White
        )
    }
}

// Room Name Step
@Composable
private fun RoomNameStep(viewModel: ProfileViewModel) {
    val roomName by viewModel.roomName.collectAsState()
    val calendarEvents by viewModel.calendarEvents.collectAsState()
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

        if (calendarEvents.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Suggestions from Calendar",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 250.dp)
            ) {
                items(calendarEvents) { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A1A1A))
                            .clickable {
                                viewModel.applyCalendarEvent(event)
                                viewModel.goToStep(ProfileStep.ROOM_TIME)
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.title,
                                color = Color.White,
                                fontSize = 16.sp,
                                maxLines = 1
                            )
                            Text(
                                text = java.text.SimpleDateFormat("EEE, MMM dd HH:mm", java.util.Locale.getDefault()).format(event.startTime),
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// Room Time Step (Unified)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomTimeStep(viewModel: ProfileViewModel) {
    val targetTime by viewModel.targetTime.collectAsState()
    val targetHour by viewModel.targetHour.collectAsState()
    val targetMinute by viewModel.targetMinute.collectAsState()
    val addToCalendar by viewModel.addToCalendar.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = targetTime
    )
    val timeState = rememberTimePickerState(
        initialHour = targetHour,
        initialMinute = targetMinute,
        is24Hour = true
    )

    OverlayStepContainer(
        title = "Create Room",
        subtitle = "When will it open?",
        onBack = { viewModel.goToStep(ProfileStep.ROOM_NAME) },
        onNext = { viewModel.goToStep(ProfileStep.ROOM_MODE) }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date Selection
            Text(
                text = "Open Date",
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            val calendar = remember { java.util.Calendar.getInstance() }
            calendar.timeInMillis = targetTime
            val dateFormat = java.text.SimpleDateFormat("EEE, MMM dd, yyyy", java.util.Locale.getDefault())
            
            Button(
                onClick = { showDatePicker = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A1A1A),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = dateFormat.format(calendar.time),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time Selection
            Text(
                text = "Open Time",
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Button(
                onClick = { showTimePicker = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A1A1A),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = String.format("%02d:%02d", targetHour, targetMinute),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add to Calendar checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (addToCalendar) Color.White else Color(0xFF1A1A1A))
                    .clickable { viewModel.updateAddToCalendar(!addToCalendar) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (addToCalendar) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Add to Calendar",
                    tint = if (addToCalendar) Color.Black else Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Add to Calendar",
                    color = if (addToCalendar) Color.Black else Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let {
                        viewModel.updateTargetDate(it)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateTargetTime(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = timeState)
            }
        )
    }
}

// Room Mode Step
@Composable
private fun RoomModeStep(viewModel: ProfileViewModel) {
    val isPublic by viewModel.isPublic.collectAsState()

    OverlayStepContainer(
        title = "Create Room",
        subtitle = "Choose Room Mode",
        backText = "< Edit Notification",
        onBack = { viewModel.goToStep(ProfileStep.ROOM_TIME) },
        onNext = {
            viewModel.goToStep(
                if (isPublic) ProfileStep.FINALIZE_PUBLIC else ProfileStep.FINALIZE_PRIVATE
            )
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModeButton(
                text = "Public",
                isSelected = isPublic,
                onClick = { viewModel.updateRoomMode(true) },
                modifier = Modifier.weight(1f)
            )
            ModeButton(
                text = "Private",
                isSelected = !isPublic,
                onClick = { viewModel.updateRoomMode(false) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Finalize Step
@Composable
private fun FinalizeStep(viewModel: ProfileViewModel) {
    val roomName by viewModel.roomName.collectAsState()
    val isCollaboration by viewModel.isCollaboration.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    OverlayStepContainer(
        title = "Create Room",
        subtitle = "\"$roomName\"\nwill be created.",
        backText = "< Edit Room Mode",
        nextButtonText = "Create",
        onBack = { viewModel.goToStep(ProfileStep.ROOM_MODE) },
        onBackToProfile = { viewModel.closeOverlay() },
        onNext = {
            if (isCollaboration) {
                viewModel.goToStep(ProfileStep.COLLABORATION)
            } else {
                viewModel.finalizeRoom(context)
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isCollaboration) Color.White else Color(0xFF1A1A1A))
                .clickable { viewModel.updateCollaboration(!isCollaboration) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = "Collaboration",
                tint = if (isCollaboration) Color.Black else Color.White
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Collaboration",
                color = if (isCollaboration) Color.Black else Color.White,
                fontSize = 16.sp
            )
        }
    }
}

// Collaboration Step
@Composable
private fun CollaborationStep(viewModel: ProfileViewModel) {
    val roomName by viewModel.roomName.collectAsState()
    var shareLink by remember { mutableStateOf(false) }
    var inviteRoomer by remember { mutableStateOf(false) }
    // Removed showGeneratedContent state as we want to show content immediately upon selection
    val context = androidx.compose.ui.platform.LocalContext.current

    OverlayStepContainer(
        title = "Collaboration",
        subtitle = "\"$roomName\"",
        backText = "< Back",
        nextButtonText = "Create",
        onBack = { viewModel.goToStep(ProfileStep.FINALIZE_PUBLIC) },
        onBackToProfile = { viewModel.closeOverlay() },
        onNext = { 
            viewModel.finalizeRoom(context)
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Share Link Button
            ModeButton(
                text = "Share Link",
                isSelected = shareLink,
                onClick = { 
                    shareLink = !shareLink
                    inviteRoomer = false
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Link Content (Visible only if shareLink is true)
            AnimatedVisibility(visible = shareLink) {
                 Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Dummy Link
                    Text(
                        text = "https://bestbefore.app/r/dummy-link",
                        color = Color(0xFF4DA6FF),
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A1A1A))
                            .padding(12.dp)
                    )
                }
            }

            // Invite Roomer Button
            ModeButton(
                text = "Invite Roomer",
                isSelected = inviteRoomer,
                onClick = { 
                    inviteRoomer = !inviteRoomer
                    shareLink = false
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            // QR Code Content (Visible only if inviteRoomer is true)
            AnimatedVisibility(visible = inviteRoomer) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Dummy QR Code
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(16.dp)
                    ) {
                         androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                             val canvasWidth = size.width
                             val canvasHeight = size.height
                             val cellSize = canvasWidth / 10
                             
                             // Draw patterns
                             drawRect(
                                 color = Color.Black,
                                 topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                                 size = androidx.compose.ui.geometry.Size(cellSize * 3, cellSize * 3)
                             )
                             drawRect(
                                 color = Color.Black,
                                 topLeft = androidx.compose.ui.geometry.Offset(canvasWidth - cellSize * 3, 0f),
                                 size = androidx.compose.ui.geometry.Size(cellSize * 3, cellSize * 3)
                             )
                             drawRect(
                                 color = Color.Black,
                                 topLeft = androidx.compose.ui.geometry.Offset(0f, canvasHeight - cellSize * 3),
                                 size = androidx.compose.ui.geometry.Size(cellSize * 3, cellSize * 3)
                             )
                             
                             // Random dots
                             for(i in 3..7) {
                                 for(j in 3..7) {
                                     if((i+j) % 2 == 0) {
                                         drawRect(
                                             color = Color.Black,
                                             topLeft = androidx.compose.ui.geometry.Offset(i * cellSize, j * cellSize),
                                             size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                                         )
                                     }
                                 }
                             }
                         }
                    }
                }
            }
        }
    }
}

// Time Capsule List Screen
@Composable
private fun TimeCapsuleListScreen(
    viewModel: ProfileViewModel,
    rooms: List<TimeCapsuleRoom>
) {
    val roomMediaMap by viewModel.roomMedia.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Time Capsule",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sort ↓",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.clickable { viewModel.sortRoomsByDateCreated() }
                )
                IconButton(onClick = { viewModel.goToStep(ProfileStep.PROFILE_MENU) }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (rooms.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No time capsules yet.\nCreate one to get started!",
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rooms) { room ->
                    TimeCapsuleCard(
                        room = room,
                        roomMediaMap = roomMediaMap,
                        onClick = { viewModel.selectRoom(room) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeCapsuleCard(
    room: TimeCapsuleRoom,
    roomMediaMap: Map<String, List<android.net.Uri>>,
    onClick: () -> Unit
) {
    // Get a random media item for this room as thumbnail
    val randomMedia = remember(room, roomMediaMap) {
        roomMediaMap[room.id]?.takeIf { it.isNotEmpty() }?.let { list ->
            list.randomOrNull()
        }
    }
    
    // Format creation date/time
    val createdDate = remember(room.dateCreated) {
        val date = java.util.Date(room.dateCreated)
        val dateFormat = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault())
        dateFormat.format(date)
    }
    
    val createdTime = remember(room.dateCreated) {
        val date = java.util.Date(room.dateCreated)
        val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        timeFormat.format(date)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Thumbnail image on the left
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray)
        ) {
            if (randomMedia != null) {
                coil.compose.AsyncImage(
                    model = randomMedia,
                    contentDescription = "Room Thumbnail",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Placeholder if no media
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "No photo",
                        tint = Color.Gray,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
        
        // Date/Time/Caption info on the right
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Created: $createdTime",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = createdDate,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = "Edit Caption",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Gray
            )
        }
    }
}

// Room Detail Screen
@Composable
private fun RoomDetailScreen(
    viewModel: ProfileViewModel,
    multiplePhotoPickerLauncher: androidx.activity.result.ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest>? = null,
    takePictureLauncher: androidx.activity.result.ActivityResultLauncher<android.net.Uri>? = null,
    filePickerLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>? = null,
    onCreateCameraUri: (android.net.Uri) -> Unit = { _ -> }
) {
    val room by viewModel.selectedRoom.collectAsState()
    val selectedMediaUris by viewModel.selectedMediaUris.collectAsState()
    val roomMediaMap by viewModel.roomMedia.collectAsState()
    val capturedImageUri by viewModel.capturedImageUri.collectAsState()
    val isGalleryViewerOpen by viewModel.isGalleryViewerOpen.collectAsState()
    val galleryViewerMedia by viewModel.galleryViewerMedia.collectAsState()
    val galleryViewerIndex by viewModel.galleryViewerIndex.collectAsState()

    room?.let { selectedRoom ->
        val savedMedia = roomMediaMap[selectedRoom.id] ?: emptyList()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedRoom.roomName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = { viewModel.goBack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Media upload options
            val context = androidx.compose.ui.platform.LocalContext.current
            
            // Helper function to check if permission is granted
            fun hasPermission(permission: String): Boolean {
                return androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            
            // Helper function to create image URI for camera
            fun createImageUri(): android.net.Uri {
                val imageFile = java.io.File(
                    context.cacheDir,
                    "camera_${System.currentTimeMillis()}.jpg"
                )
                return androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
            }
            
            val handleGalleryClick: () -> Unit = {
                val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    android.Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                }
                
                if (hasPermission(permission)) {
                    multiplePhotoPickerLauncher?.launch(
                        androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                } else {
                    viewModel.requestGalleryPermission()
                }
            }
            
            val handleFileClick: () -> Unit = {
                val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    android.Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                }
                
                if (hasPermission(permission)) {
                    filePickerLauncher?.launch(arrayOf("image/*", "video/*"))
                } else {
                    viewModel.requestFilePermission()
                }
            }
            
            val handleCameraClick: () -> Unit = {
                if (hasPermission(android.Manifest.permission.CAMERA)) {
                    val uri = createImageUri()
                    onCreateCameraUri(uri)
                    takePictureLauncher?.launch(uri)
                } else {
                    viewModel.requestCameraPermission()
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MediaButton(icon = Icons.Default.Photo, label = "Photo archive", onClick = handleGalleryClick)
                MediaButton(icon = Icons.Default.AttachFile, label = "Choose file", onClick = handleFileClick)
                MediaButton(icon = Icons.Default.CameraAlt, label = "Use camera", onClick = handleCameraClick)
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // Camera Preview Section (if image was captured)
            if (capturedImageUri != null) {
                Text(
                    text = "Camera Preview",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Preview Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.DarkGray)
                ) {
                    coil.compose.AsyncImage(
                        model = capturedImageUri,
                        contentDescription = "Captured Photo",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Accept / Retake buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { viewModel.clearCapturedImage() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f).height(50.dp).padding(end = 8.dp)
                    ) {
                        Text("Retake", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = { viewModel.acceptCapturedImage() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D972)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f).height(50.dp).padding(start = 8.dp)
                    ) {
                        Text("Accept", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Selected Memories",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedMediaUris.isNotEmpty()) {
                // Preview Row for Selected Media
                Text(
                    text = "Selected to Upload",
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                ) {
                    items(selectedMediaUris) { uri ->
                        coil.compose.AsyncImage(
                            model = uri,
                            contentDescription = "Selected Memory",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .width(120.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Upload Button
                Button(
                    onClick = { viewModel.uploadMedia(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "Upload ${selectedMediaUris.size} Memories",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Saved Memories Section
            if (savedMedia.isNotEmpty()) {
                Text(
                    text = "Saved Memories",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Take remaining space
                ) {
                    items(
                        count = savedMedia.size
                    ) { index ->
                        val uri = savedMedia[index]
                        coil.compose.AsyncImage(
                            model = uri,
                            contentDescription = "Saved Memory",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.DarkGray)
                                .clickable {
                                    viewModel.openGalleryViewer(savedMedia, index)
                                }
                        )
                    }
                }
            } else if (selectedMediaUris.isEmpty()) {
                // Show Empty State only if both are empty
                 Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, Color.DarkGray, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("lll")
                }
            } else {
                 Spacer(modifier = Modifier.weight(1f))
            }
        }
        
        // Gallery viewer overlay
        val isGalleryOpen by viewModel.isGalleryViewerOpen.collectAsState()
        val galleryMedia by viewModel.galleryViewerMedia.collectAsState()
        val galleryIndex by viewModel.galleryViewerIndex.collectAsState()
        
        if (isGalleryOpen) {
            GalleryViewerOverlay(
                media = galleryMedia,
                initialPage = galleryIndex,
                onClose = { viewModel.closeGalleryViewer() },
                onPageChanged = { viewModel.updateGalleryIndex(it) }
            )
        }
        
        // Room unlock dialog
        val showUnlockDialog by viewModel.showUnlockDialog.collectAsState()
        val unlockDialogRoom by viewModel.unlockDialogRoom.collectAsState()
        
        if (showUnlockDialog && unlockDialogRoom != null) {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            AlertDialog(
                onDismissRequest = { viewModel.dismissUnlockDialog() },
                title = {
                    Text(
                        text = "Time Capsule Unlocked",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text("\"${unlockDialogRoom?.roomName}\" is now available to see. Would you like to keep it or delete it?")
                },
                confirmButton = {
                    TextButton(onClick = { unlockDialogRoom?.let { viewModel.keepRoom(ctx, it) } }) {
                        Text("Keep")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { unlockDialogRoom?.let { viewModel.deleteRoom(ctx, it) } }) {
                        Text("Delete", color = Color.Red)
                    }
                },
                containerColor = Color(0xFF1A1A1A),
                titleContentColor = Color.White,
                textContentColor = Color.White
            )
        }
    }
}

@Composable
private fun MediaButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

// Shared Components
@Composable
private fun OverlayStepContainer(
    title: String,
    subtitle: String,
    backText: String = "", // Deprecated
    nextButtonText: String = "Next",
    onBack: () -> Unit,
    onBackToProfile: (() -> Unit)? = null,
    onNext: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = false) {} // Prevent clicking through to underlying content
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Top Bar with Back Arrow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp), // System bar spacing
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title & Subtitle
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = subtitle,
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            content()

            Spacer(modifier = Modifier.weight(1f))
            
            // Bottom area (removed text back button)
        }

        // Next button (orb style with text)
        OrbMenuSimple(
            modifier = Modifier.align(Alignment.CenterEnd),
            label = nextButtonText,
            onClick = onNext
        )
    }
}

@Composable
private fun NumberPickerColumn(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (value > range.first) onValueChange(value - 1) }) {
                Icon(Icons.Default.Remove, "Decrease", tint = Color.White)
            }
            Text(
                text = value.toString().padStart(2, '0'),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.width(60.dp),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { if (value < range.last) onValueChange(value + 1) }) {
                Icon(Icons.Default.Add, "Increase", tint = Color.White)
            }
        }
    }
}

@Composable
private fun ModeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color.White else Color(0xFF1A1A1A))
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// --- Ported Hallway Components ---

@Composable
private fun CardStack(
    cards: List<HallwayCard>,
    selectedIndex: Int,
    onCardSelected: (Int) -> Unit
) {
    if (cards.isEmpty()) return

    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = selectedIndex,
        initialPageOffsetFraction = 0f
    ) { cards.size }

    // Sync Pager -> ViewModel
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != selectedIndex) {
            onCardSelected(pagerState.currentPage)
        }
    }

    // Sync ViewModel -> Pager (if changed externally)
    LaunchedEffect(selectedIndex) {
        if (pagerState.currentPage != selectedIndex) {
            pagerState.animateScrollToPage(selectedIndex)
        }
    }

    androidx.compose.foundation.pager.VerticalPager(
        state = pagerState,
        modifier = Modifier
            .width(260.dp) // Adjusted width for visual balance
            .fillMaxHeight(),
        contentPadding = PaddingValues(vertical = 32.dp),
        pageSpacing = (-16).dp // Slight negative spacing by default
    ) { page ->
        val card = cards[page]

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp) // Fixed height for calculation
                .graphicsLayer {
                    val pageOffset = (
                        (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    ) 
                    // Note: In Compose Pager, (page - currentPage) is positive for NEXT pages.
                    // User Code: position > 0 is Next.
                    // Compose: page > currentPage => page - currentPage > 0.
                    // So `position` = `page - currentPage - currentPageOffsetFraction` ??
                    
                    // Let's stick to standard Compose Pager offset calculation:
                    // position = (pageIndex - currentPage) - offsetFraction
                    // But we can simplify: just take the difference.
                    
                    // Re-calculate strictly to match user snippet:
                    // position = (page - (currentPage + currentPageOffsetFraction))
                    val position = page - (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                    
                    val height = size.height
                    val absPos = kotlin.math.abs(position)
                    
                    // Pivot X = 0 (Left), Pivot Y = Center (0.5f)
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)

                    if (position < 0) {
                        // Previous (Top)
                        val scale = 0.85f + (1 - absPos.coerceAtMost(1f)) * 0.15f
                        this.scaleX = scale
                        this.scaleY = scale
                        this.translationY = -size.height * position * 0.4f
                        this.alpha = 1f - (absPos * 0.3f)
                    } else if (position > 0) {
                        // Next (Bottom)
                        val scale = 0.85f + (1 - absPos.coerceAtMost(1f)) * 0.15f
                        this.scaleX = scale
                        this.scaleY = scale
                        this.translationY = -size.height * position * 0.85f
                        this.alpha = 1f - (absPos * 0.3f)
                    } else {
                        // Current
                        this.scaleX = 1f
                        this.scaleY = 1f
                        this.translationY = 0f
                        this.alpha = 1f
                    }
                }
        ) {
            StackCardItem(
                card = card,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun StackCardItem(
    card: HallwayCard,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(200.dp)
            .height(280.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E)
                    )
                )
            ),
        contentAlignment = Alignment.BottomStart
    ) {
        // Card Image
        if (card.imageUrl != null) {
            coil.compose.AsyncImage(
                model = card.imageUrl,
                contentDescription = card.title,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Card gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF16213E).copy(alpha = 0.8f) // Fade to dark blue at bottom
                        )
                    )
                )
        )

        
        Text(
            text = card.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun ExploreItem(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A1A))
            .padding(10.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun BottomNavigation(
    currentTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(74.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            text = "Rooming",
            isSelected = currentTab == BottomTab.ROOMING,
            onClick = { onTabSelected(BottomTab.ROOMING) }
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onTabSelected(BottomTab.EVERYONE) }
        ) {
            if (currentTab == BottomTab.EVERYONE) {
                Text(
                    text = "▽",
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
            Text(
                text = "Everyone",
                fontSize = if (currentTab == BottomTab.EVERYONE) 20.sp else 18.sp,
                fontWeight = if (currentTab == BottomTab.EVERYONE) FontWeight.Bold else FontWeight.Normal,
                color = if (currentTab == BottomTab.EVERYONE) Color.White else Color.White.copy(alpha = 0.4f)
            )
        }
        
        BottomNavItem(
            text = "Artists",
            isSelected = currentTab == BottomTab.ARTISTS,
            onClick = { onTabSelected(BottomTab.ARTISTS) }
        )
    }
}

@Composable
private fun BottomNavItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        fontSize = 18.sp,
        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.clickable { onClick() }
    )
}

// Gallery Viewer Overlay - Fullscreen swipeable media viewer
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryViewerOverlay(
    media: List<android.net.Uri>,
    initialPage: Int = 0,
    onClose: () -> Unit,
    onPageChanged: (Int) -> Unit
) {
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = initialPage,
        pageCount = { media.size }
    )
    
    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Swipeable Image Pager
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                coil.compose.AsyncImage(
                    model = media[page],
                    contentDescription = "Memory ${page + 1}",
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // Close Button - Top Right
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Page Indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${pagerState.currentPage + 1} / ${media.size}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
