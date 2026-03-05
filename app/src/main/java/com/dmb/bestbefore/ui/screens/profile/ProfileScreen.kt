package com.dmb.bestbefore.ui.screens.profile

import androidx.compose.animation.*
import com.dmb.bestbefore.data.models.HallwayCard
import com.dmb.bestbefore.ui.screens.hallway.BottomTab
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

import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer

import com.dmb.bestbefore.data.models.TimeCapsuleRoom
import com.dmb.bestbefore.ui.components.OrbMenu

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
    onNavigateToNotifications: () -> Unit,
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
            viewModel.loadCalendarEvents(context)
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
            viewModel.uploadMedia(context)
        }
    }
    
    // Camera capture launcher
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            viewModel.setCapturedImage(cameraImageUri!!)
            viewModel.acceptCapturedImage()
            viewModel.uploadMedia(context)
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
        
        val pendingInviteId = com.dmb.bestbefore.MainActivity.pendingInviteRoomId
        val pendingInviteName = com.dmb.bestbefore.MainActivity.pendingInviteRoomName
        if (pendingInviteId != null && pendingInviteName != null) {
            viewModel.showInviteDialog(pendingInviteId, pendingInviteName)
            com.dmb.bestbefore.MainActivity.clearPendingInvite()
        }

        // Handle QR code deep link invite token
        val pendingToken = com.dmb.bestbefore.MainActivity.pendingInviteToken
        if (pendingToken != null) {
            viewModel.joinRoomViaToken(context, pendingToken)
            com.dmb.bestbefore.MainActivity.clearPendingInviteToken()
        }
    }




    // Moved Overlay Logic to inside the Box for correct Z-ordering
    
    val pendingInviteRoomId by viewModel.pendingInviteRoomId.collectAsState()
    val pendingInviteRoomName by viewModel.pendingInviteRoomName.collectAsState()

    if (pendingInviteRoomId != null && pendingInviteRoomName != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideInviteDialog() },
            containerColor = Color(0xFF1C1C1E),
            title = { Text("Room Invitation", color = Color.White) },
            text = { Text("You have been invited to collaborate in \"$pendingInviteRoomName\". Do you accept?", color = Color.LightGray) },
            confirmButton = {
                TextButton(onClick = { viewModel.handleAcceptInvite(context, pendingInviteRoomId!!) }) {
                    Text("Accept", color = Color(0xFF34C759), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.handleDeclineInvite(context, pendingInviteRoomId!!) }) {
                    Text("Decline", color = Color.Red)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.navigationBars)
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
            val notifRepo = remember { com.dmb.bestbefore.data.repository.NotificationRepository(context) }
            val unreadCount by notifRepo.unreadCount.collectAsState()
            val hallwayRefreshing by hallwayViewModel.isRefreshing.collectAsState()

            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = hallwayRefreshing,
                onRefresh = { hallwayViewModel.refreshRooms() },
                modifier = Modifier.fillMaxSize()
            ) {
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
                    val headerText = when (currentTab) {
                        BottomTab.ROOMING -> "Rooming"
                        BottomTab.EVERYONE -> "Hallway"
                        BottomTab.ARTISTS -> "Artists"
                    }
                    androidx.compose.animation.AnimatedContent(
                        targetState = headerText,
                        transitionSpec = {
                            (fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) +
                                slideInVertically(animationSpec = androidx.compose.animation.core.tween(300)) { -it / 2 })
                                .togetherWith(fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) +
                                    slideOutVertically(animationSpec = androidx.compose.animation.core.tween(200)) { it / 2 })
                        },
                        label = "headerTextAnimation"
                    ) { text ->
                        Text(
                            text = text,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Box(
                        modifier = Modifier.clickable {
                            notifRepo.markAllRead()
                            onNavigateToNotifications()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-4).dp)
                                    .size(16.dp)
                                    .background(Color.Red, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Main content
                if (currentTab == BottomTab.ROOMING) {
                    // ROOMING TAB: 1 room per row with info
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 100.dp, bottom = 100.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(cards.size) { index ->
                            val card = cards[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF1A1A2E), Color(0xFF16213E))
                                        )
                                    )
                                    .clickable {
                                        viewModel.selectRoomFromHallway(
                                            card.id,
                                            card.title,
                                            card.timeCapsuleDays
                                        )
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Room thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0A0A1A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (card.imageUrl != null) {
                                        coil.compose.AsyncImage(
                                            model = card.imageUrl,
                                            contentDescription = card.title,
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFF007AFF),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                // Room info
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = card.title,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = card.description,
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Time Capsule: ${card.timeCapsuleDays} days",
                                        color = Color(0xFF007AFF),
                                        fontSize = 11.sp
                                    )
                                }
                                // Arrow
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    // EVERYONE TAB: Card Stack + Info Panel
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 100.dp, bottom = 80.dp)
                    ) {
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
                                onCardSelected = { hallwayViewModel.selectCard(it) },
                                onCardTapped = { card -> viewModel.selectRoomFromHallway(card.id, card.title, card.timeCapsuleDays) }
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(0.4f)
                                .fillMaxHeight()
                                .padding(horizontal = 12.dp)
                        ) {
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
                                    text = selectedCard.description,
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
                }

                // Bottom navigation
                BottomNavigation(
                    currentTab = currentTab,
                    onTabSelected = { 
                        if (it == BottomTab.EVERYONE) {
                            viewModel.closeOverlay()
                        }
                        hallwayViewModel.selectTab(it)
                    },
                    onProfileClick = { viewModel.openProfileMenu() },
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
            } // end PullToRefreshBox
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
