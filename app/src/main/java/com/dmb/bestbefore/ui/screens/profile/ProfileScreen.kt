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
import com.dmb.bestbefore.ui.screens.hallway.HallwayScreen
import com.dmb.bestbefore.ui.theme.LocalBestBeforeColors

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

    var showMusicSelector by remember { mutableStateOf(false) }
    var authToken by remember { mutableStateOf<String?>(null) }
    val musicViewModel: com.dmb.bestbefore.ui.components.MusicViewModel = viewModel()

    LaunchedEffect(Unit) {
        authToken = viewModel.getAuthToken(context)
    }

    if (showMusicSelector && authToken != null) {
        com.dmb.bestbefore.ui.components.MusicSelectorBottomSheet(
            viewModel = musicViewModel,
            token = authToken!!,
            onDismissRequest = { showMusicSelector = false }
        )
    }

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
    
    // QR code scanner launcher for joining rooms
    val qrScanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scannedValue = result.data?.getStringExtra("SCAN_RESULT") ?: ""
            // Handle both HTTPS format and legacy custom-scheme format
            val roomId = when {
                // https://bestbefore.up.railway.app/join/{roomId}
                scannedValue.startsWith("https://bestbefore.up.railway.app/join/") ->
                    scannedValue.removePrefix("https://bestbefore.up.railway.app/join/").trim('/')
                // legacy: bestbefore:room:{roomId}
                scannedValue.startsWith("bestbefore:room:") ->
                    scannedValue.removePrefix("bestbefore:room:")
                // bestbefore://room/{roomId}
                scannedValue.startsWith("bestbefore://room/") ->
                    scannedValue.removePrefix("bestbefore://room/")
                else -> null
            }
            if (roomId != null) {
                viewModel.joinRoomViaQR(context, roomId)
            } else if (scannedValue.startsWith("https://bestbefore.up.railway.app/invite-join/")) {
                val token = scannedValue.removePrefix("https://bestbefore.up.railway.app/invite-join/").trim('/')
                viewModel.joinRoomViaToken(context, token)
            } else if (scannedValue.startsWith("bestbefore://invite/")) {
                val token = scannedValue.removePrefix("bestbefore://invite/")
                viewModel.joinRoomViaToken(context, token)
            } else {
                android.widget.Toast.makeText(context, "Invalid QR code", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val qrCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = android.content.Intent(context, com.dmb.bestbefore.ui.screens.profile.QrScannerActivity::class.java)
            qrScanLauncher.launch(intent)
        } else {
            android.widget.Toast.makeText(context, "Camera permission required to scan QR codes", android.widget.Toast.LENGTH_SHORT).show()
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

        // Handle invite token deep link (bestbefore://invite/{token} or /invite-join/{token} HTTPS)
        val pendingToken = com.dmb.bestbefore.MainActivity.pendingInviteToken
        if (pendingToken != null) {
            viewModel.joinRoomViaToken(context, pendingToken)
            com.dmb.bestbefore.MainActivity.clearPendingInviteToken()
        }

        // Handle QR room join — /join/{roomId} HTTPS App Link or bestbefore://room/{roomId}
        val pendingQRRoom = com.dmb.bestbefore.MainActivity.pendingQRRoomId
        if (pendingQRRoom != null) {
            viewModel.joinRoomViaQR(context, pendingQRRoom)
            com.dmb.bestbefore.MainActivity.clearPendingQRRoomId()
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
            com.dmb.bestbefore.ui.screens.hallway.HallwayScreen(
                onNavigateToProfile = { viewModel.openProfileMenu() },
                onNavigateToNotifications = onNavigateToNotifications,
                viewModel = hallwayViewModel
            )
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
                    ProfileStep.PROFILE_MENU -> ProfileMenuScreen(viewModel, musicViewModel, createdRooms, onLogout)
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
