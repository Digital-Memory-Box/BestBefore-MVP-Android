package com.dmb.bestbefore.ui.screens.profile

import androidx.compose.animation.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTapGestures
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
import com.dmb.bestbefore.ui.components.VideoPlayer
import com.dmb.bestbefore.ui.components.MusicPlayer


import androidx.compose.foundation.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Helper to prevent UI thread blocking while decoding large Base64 images from backend
@Composable
fun AsyncBase64Image(
    itemData: Any,
    contentScale: androidx.compose.ui.layout.ContentScale,
    modifier: Modifier = Modifier
) {
    val modelStr = itemData.toString()
    if (modelStr.startsWith("data:image")) {
        var bytes by remember { mutableStateOf<ByteArray?>(null) }
        LaunchedEffect(modelStr) {
            withContext(Dispatchers.IO) {
                try {
                    val cleanStr = modelStr.substringAfter("base64,")
                    bytes = android.util.Base64.decode(cleanStr, android.util.Base64.DEFAULT)
                } catch (e: Exception) {
                    bytes = ByteArray(0)
                }
            }
        }
        if (bytes != null && bytes!!.isNotEmpty()) {
            coil.compose.AsyncImage(
                model = java.nio.ByteBuffer.wrap(bytes!!),
                contentDescription = null,
                contentScale = contentScale,
                modifier = modifier
            )
        } else if (bytes == null) {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF007AFF), modifier = Modifier.size(24.dp))
            }
        }
    } else {
        coil.compose.AsyncImage(
            model = itemData,
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

private fun isAudioMemoryUri(uri: Uri): Boolean {
    val value = uri.toString().lowercase()
    return value.startsWith("data:audio") ||
        value.endsWith(".mp3") ||
        value.endsWith(".m4a") ||
        value.endsWith(".wav") ||
        value.endsWith(".aac") ||
        value.endsWith(".ogg")
}

private fun isNoteMemoryUri(uri: Uri): Boolean = uri.toString().startsWith("NOTE:")

private fun isVideoMemoryUri(uri: Uri): Boolean {
    val value = uri.toString().lowercase()
    return value.startsWith("data:video") ||
        value.endsWith(".mp4") ||
        value.endsWith(".mov") ||
        value.endsWith(".avi") ||
        value.endsWith(".mkv")
}

// --- ROOM DETAIL SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailScreen(
    viewModel: ProfileViewModel,
    multiplePhotoPickerLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    filePickerLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    onAddToRooming: (() -> Unit)? = null,
    onRemoveFromRooming: (() -> Unit)? = null,
    isRoomInRooming: Boolean = false,
    onIgnoreRoom: (() -> Unit)? = null
) {
    val room by viewModel.selectedRoom.collectAsState()
    val roomMedia by viewModel.roomMedia.collectAsState() // Persisted room media
    val roomEmotions by viewModel.roomEmotions.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val isRecording by viewModel.isRecordingAudio.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()

    val recordAudioPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startAudioRecording(context)
        } else {
            android.widget.Toast.makeText(context, "Microphone permission is required.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    // UI State
    var showQrCode by remember { mutableStateOf(false) }
    var show3DotMenu by remember { mutableStateOf(false) }
    var showRoomDetailsSheet by remember { mutableStateOf(false) }
    var showWriteNoteDialog by remember { mutableStateOf(false) }
    var showDeleteRoomConfirm by remember { mutableStateOf(false) }
    var noteContent by remember { mutableStateOf("") }
    var showAllMediaGrid by remember { mutableStateOf(false) }
    var isIgnored by remember { mutableStateOf(false) }
    
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
    
    // Combine room-specific media
    val currentRoomMedia = roomMedia[room?.id] ?: emptyList()
    
    Box(modifier = Modifier.fillMaxSize()) {
        com.dmb.bestbefore.ui.components.AnimatedBackgroundView(
            theme = room?.theme?.lowercase() ?: "default"
        )
        if (room == null) {
            Text("Room not found", color = Color.White, modifier = Modifier.align(Alignment.Center))
        } else {
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshRoomMemories() },
                modifier = Modifier.fillMaxSize()
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(bottom = 32.dp)
            ) {
                val canContribute = room!!.isOwnedByMe || room!!.isCollaborator
                val currentTime = System.currentTimeMillis()
                val isLocked = room!!.unlockTime > currentTime
                val isRoomClosed = room!!.scheduledClosureTime > 0L && currentTime >= room!!.scheduledClosureTime
                val isViewer = !canContribute
                val isArtistRoom = room!!.ownerUserType?.equals("artist", ignoreCase = true) == true
                val selectedEmotion = roomEmotions[room!!.id]

                if (canContribute) {
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
                            Icon(
                                Icons.Default.MusicNote,
                                "Music",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { showMusicSelector = true }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(
                                Icons.Default.QrCode2,
                                "QR Code",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { showQrCode = true }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
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
                                    if (room?.isOwnedByMe == true) {
                                        DropdownMenuItem(
                                            text = { Text("Edit Room", color = Color.White) },
                                            onClick = {
                                                show3DotMenu = false
                                                room?.let { viewModel.selectRoomForEditing(it) }
                                            }
                                        )
                                    }
                                    if (onAddToRooming != null) {
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        if (isRoomInRooming) Icons.Default.NotificationsOff else Icons.Default.Bookmark,
                                                        null,
                                                        tint = if (isRoomInRooming) Color(0xFFFF3B30) else Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        if (isRoomInRooming) "Remove from Rooming" else "Add to Rooming",
                                                        color = if (isRoomInRooming) Color(0xFFFF3B30) else Color.White
                                                    )
                                                }
                                            },
                                            onClick = {
                                                if (isRoomInRooming) onRemoveFromRooming?.invoke() else onAddToRooming()
                                                show3DotMenu = false
                                            }
                                        )
                                    }
                                    if (room?.isOwnedByMe == true) {
                                        DropdownMenuItem(
                                            text = { Text("Delete Room", color = Color.Red) },
                                            onClick = {
                                                show3DotMenu = false
                                                showDeleteRoomConfirm = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
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
                            Icon(
                                imageVector = Icons.Filled.Apps,
                                contentDescription = "View all memories",
                                tint = Color(0xFF007AFF),
                                modifier = Modifier
                                    .size(26.dp)
                                    .clickable {
                                        if (currentRoomMedia.isNotEmpty()) {
                                            showAllMediaGrid = true
                                        } else {
                                            android.widget.Toast.makeText(
                                                context,
                                                "No memories to show yet",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                            )
                            Spacer(modifier = Modifier.width(20.dp))
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2C2C2E))
                                        .clickable { show3DotMenu = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.MoreHoriz,
                                        "Menu",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
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
                                    if (onIgnoreRoom != null) {
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Block,
                                                        null,
                                                        tint = if (isIgnored) Color.Gray else Color(0xFFFF3B30),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        if (isIgnored) "Room Ignored ✓" else "Ignore This Room",
                                                        color = if (isIgnored) Color.Gray else Color(0xFFFF3B30),
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            },
                                            onClick = {
                                                if (!isIgnored) {
                                                    onIgnoreRoom()
                                                    isIgnored = true
                                                }
                                                show3DotMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = room!!.roomName,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 4.dp, bottom = if (canContribute) 8.dp else 20.dp)
                )

                if (canContribute) {
                    Text(
                        text = "Memories",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        userScrollEnabled = false,
                        modifier = Modifier.height(330.dp)
                    ) {
                        item { MemoryActionCard(Icons.Default.Image, "Add Photo", if (isRoomClosed) Color.Gray else Color(0xFF007AFF)) {
                            if (isRoomClosed) android.widget.Toast.makeText(context, "This room is archived.", android.widget.Toast.LENGTH_SHORT).show()
                            else multiplePhotoPickerLauncher.launch("image/*")
                        }}
                        item { MemoryActionCard(Icons.Default.Edit, "Write Note", if (isRoomClosed) Color.Gray else Color(0xFFAF52DE)) {
                            if (isRoomClosed) android.widget.Toast.makeText(context, "This room is archived.", android.widget.Toast.LENGTH_SHORT).show()
                            else showWriteNoteDialog = true
                        }}
                        item { MemoryActionCard(Icons.Default.Videocam, "Add Video", if (isRoomClosed) Color.Gray else Color(0xFFFF9500)) {
                            if (isRoomClosed) android.widget.Toast.makeText(context, "This room is archived.", android.widget.Toast.LENGTH_SHORT).show()
                            else multiplePhotoPickerLauncher.launch("video/*")
                        }}
                        item { MemoryActionCard(Icons.Default.MusicNote, "Add Music", if (isRoomClosed) Color.Gray else Color(0xFFFF3B30)) {
                            if (isRoomClosed) android.widget.Toast.makeText(context, "This room is archived.", android.widget.Toast.LENGTH_SHORT).show()
                            else filePickerLauncher.launch(arrayOf("audio/*"))
                        }}
                        item {
                            val label = if (isRecording) "Stop Recording" else "Record Audio"
                            val icon = if (isRecording) Icons.Default.Stop else Icons.Default.Mic
                            MemoryActionCard(icon, label, if (isRoomClosed) Color.Gray else Color(0xFFFF2D55)) {
                                if (isRoomClosed) {
                                    android.widget.Toast.makeText(context, "This room is archived.", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    if (isRecording) {
                                        viewModel.stopAudioRecordingAndUpload(context)
                                    } else {
                                        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.RECORD_AUDIO
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                        if (hasPermission) {
                                            viewModel.startAudioRecording(context)
                                        } else {
                                            recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            val isLockedLocal = System.currentTimeMillis() < room!!.unlockTime
                            if (!isLockedLocal) {
                                MemoryActionCard(Icons.Default.FolderOpen, "View All", Color(0xFF34C759)) {
                                    showAllMediaGrid = true
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxWidth()) {}
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                } else {
                    if (onAddToRooming != null) {
                        RoomingPrimaryCta(
                            inRooming = isRoomInRooming,
                            onAdd = {
                                onAddToRooming()
                                android.widget.Toast.makeText(context, "Added to Rooming", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onRemove = {
                                onRemoveFromRooming?.invoke()
                                android.widget.Toast.makeText(context, "Removed from Rooming", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    if (isArtistRoom) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(22.dp))
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProfileViewModel.ARTIST_ROOM_EMOTIONS.forEach { emotion ->
                                val isSelected = selectedEmotion == emotion
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isSelected) Color.White.copy(alpha = 0.16f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = if (isSelected) 1.dp else 0.dp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.35f) else Color.Transparent,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            viewModel.setArtistRoomEmotion(
                                                context = context,
                                                roomId = room!!.id,
                                                emotion = if (isSelected) null else emotion
                                            )
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emotion.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    ExplorerReadOnlyModeBanner()
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Recent Drops Section
                Text("Recent Drops", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))

                val isTimeCapsule = room!!.isCollaboration
                val isClosed = room!!.scheduledClosureTime > 0L &&
                              System.currentTimeMillis() >= room!!.scheduledClosureTime
                val showViewAll = !isTimeCapsule && !isClosed

                if (isLocked) {
                    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault())
                    val unlockDateString = sdf.format(java.util.Date(room!!.unlockTime))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1C1C1E).copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("This Time Capsule is locked", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(unlockDateString, color = Color.Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(24.dp))
                            CountdownTimer(targetTimeMillis = room!!.unlockTime)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                } else if (isClosed) {
                    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    Text(
                        "Archived — closed on ${sdf.format(java.util.Date(room!!.scheduledClosureTime))}",
                        color = Color(0xFF8E8E93), fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                } else if (room!!.scheduledClosureTime > 0L) {
                    val sdf = java.text.SimpleDateFormat("MMM dd 'at' h:mm a", java.util.Locale.getDefault())
                    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF1C1C1E).copy(alpha = 0.6f), RoundedCornerShape(12.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Room closes on ${sdf.format(java.util.Date(room!!.scheduledClosureTime))}", color = Color.Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            CountdownTimer(targetTimeMillis = room!!.scheduledClosureTime)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Display Media Strategy:
                // Viewers can only see max 5 drops if locked.
                // If it's closed/locked, default was previously takeLast(2) but now updated to 5.
                // If room is OPEN, viewers get see all. 
                val displayMedia = if (isLocked) {
                    currentRoomMedia.takeLast(5)
                } else if (isClosed) {
                    currentRoomMedia.takeLast(5)
                } else {
                    currentRoomMedia
                }
                
                if (displayMedia.isEmpty()) {
                    val emptyMessage = if (isViewer) {
                        "No memories in this room yet."
                    } else {
                        "No drops yet. Be the first to add a memory!"
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Color(0xFF1C1C1E).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Inbox, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(emptyMessage, color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                } else {
                    // Show photos in "Recent Drops" (Up to 2 per line)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        for (i in displayMedia.indices step 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val item1 = displayMedia[i]
                                val item2 = displayMedia.getOrNull(i + 1)
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.DarkGray)
                                        .clickable {
                                            viewModel.openGalleryViewer(displayMedia, i)
                                        }
                                ) {
                                    val itemStr = item1.toString()
                                    val isAudio1 = isAudioMemoryUri(item1)
                                    val isNote1 = isNoteMemoryUri(item1)
                                    val isVideo1 = isVideoMemoryUri(item1)
                                    if (isNote1) {
                                        val parts = itemStr.removePrefix("NOTE:").split(":", limit = 2)
                                        val noteTitle = parts.getOrElse(0) { "Note" }
                                        val noteBody = parts.getOrElse(1) { "" }
                                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2C2C2E)).padding(10.dp)) {
                                            Column {
                                                Icon(Icons.Default.StickyNote2, null, tint = Color(0xFFAF52DE), modifier = Modifier.size(24.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(noteTitle, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(noteBody, color = Color(0xFFAAAAAA), fontSize = 10.sp, maxLines = 4, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                            }
                                        }
                                    } else if (isAudio1) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFF2C2C2E)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(52.dp))
                                        }
                                    } else if (isVideo1) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFF2C2C2E)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Videocam, null, tint = Color.White, modifier = Modifier.size(52.dp))
                                        }
                                    } else {
                                        AsyncBase64Image(
                                            itemData = item1,
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                
                                if (item2 != null) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.DarkGray)
                                            .clickable {
                                                viewModel.openGalleryViewer(displayMedia, i + 1)
                                            }
                                    ) {
                                        val item2Str = item2.toString()
                                        val isAudio2 = isAudioMemoryUri(item2)
                                        val isNote2 = isNoteMemoryUri(item2)
                                        val isVideo2 = isVideoMemoryUri(item2)
                                        if (isNote2) {
                                            val parts = item2Str.removePrefix("NOTE:").split(":", limit = 2)
                                            val noteTitle2 = parts.getOrElse(0) { "Note" }
                                            val noteBody2 = parts.getOrElse(1) { "" }
                                            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2C2C2E)).padding(10.dp)) {
                                                Column {
                                                    Icon(Icons.Default.StickyNote2, null, tint = Color(0xFFAF52DE), modifier = Modifier.size(24.dp))
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(noteTitle2, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(noteBody2, color = Color(0xFFAAAAAA), fontSize = 10.sp, maxLines = 4, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                                }
                                            }
                                        } else if (isAudio2) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0xFF2C2C2E)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(52.dp))
                                            }
                                        } else if (isVideo2) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0xFF2C2C2E)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Videocam, null, tint = Color.White, modifier = Modifier.size(52.dp))
                                            }
                                        } else {
                                            AsyncBase64Image(
                                                itemData = item2,
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // ─── CONNECTED ROOMS SECTION ────────────────────────────────
                val connectedIds = room!!.connectedRooms
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    "Connected Rooms",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (connectedIds.isEmpty()) {
                    Text(
                        "No connections yet.",
                        color = Color(0xFF8E8E93),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        connectedIds.forEach { targetId ->
                            ConnectedRoomItem(
                                targetId = targetId,
                                viewModel = viewModel,
                                onClick = { room -> viewModel.selectRoom(room) }
                            )
                        }
                    }
                }

            } // End of Column (has room)
            } // End of PullToRefreshBox
        } // End of room == null else block

        com.dmb.bestbefore.ui.components.MusicPlayerBottomBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp)
        )
        
        // All Media Grid View Overlay
        AnimatedVisibility(
            visible = showAllMediaGrid,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            androidx.activity.compose.BackHandler(enabled = showAllMediaGrid) {
                showAllMediaGrid = false
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showAllMediaGrid = false }) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White)
                    }
                    Text("All Media", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.size(48.dp)) // balance layout
                }
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(currentRoomMedia.size) { index ->
                        val item = currentRoomMedia[index]
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .background(Color.DarkGray)
                                .clickable { viewModel.openGalleryViewer(currentRoomMedia, index) }
                        ) {
                            val itemStr = item.toString()
                            if (isNoteMemoryUri(item)) {
                                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2C2C2E)).padding(4.dp), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.StickyNote2, null, tint = Color(0xFFAF52DE), modifier = Modifier.size(32.dp))
                                }
                            } else if (isAudioMemoryUri(item)) {
                                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2C2C2E)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(34.dp))
                                }
                            } else if (isVideoMemoryUri(item)) {
                                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2C2C2E)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Videocam, null, tint = Color.White, modifier = Modifier.size(34.dp))
                                }
                            } else {
                                AsyncBase64Image(
                                    itemData = item,
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
        
        // QR Code Dialog with forwarding join link
        if (showQrCode) {
            val invLink = "https://bestbefore.up.railway.app/join/${room?.id}"

            AlertDialog(
                onDismissRequest = {
                    showQrCode = false
                },
                containerColor = Color(0xFF1C1C1E),
                title = { Text("Room Share Link", color = Color.White) },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val qrBitmap = remember(invLink) {
                            try {
                                val size = 512
                                val bitMatrix = com.google.zxing.qrcode.QRCodeWriter().encode(
                                    invLink,
                                    com.google.zxing.BarcodeFormat.QR_CODE,
                                    size,
                                    size
                                )
                                val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
                                for (x in 0 until size) {
                                    for (y in 0 until size) {
                                        bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                                    }
                                }
                                bitmap
                            } catch (e: Exception) {
                                null
                            }
                        }

                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier
                                    .size(200.dp)
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Scan to join room: ${room?.roomName}",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Share button
                        Button(
                            onClick = {
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, "Join my room \"${room?.roomName}\" on BestBefore!\n$invLink")
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Invite"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, "Share", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Link", color = Color.White)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showQrCode = false
                    }) {
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
                                viewModel.uploadNote(context, noteContent)
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

        if (showDeleteRoomConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteRoomConfirm = false },
                containerColor = Color(0xFF1C1C1E),
                title = {
                    Text(
                        text = "Delete Room?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Do you want to delete room ${room?.roomName ?: ""}?",
                        color = Color.LightGray
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteRoomConfirm = false
                            room?.let { viewModel.deleteRoom(context, it, fromInsideRoom = true) }
                        }
                    ) {
                        Text("Delete Room", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteRoomConfirm = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }

        if (isUploading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF007AFF), strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Uploading memory...", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RoomingPrimaryCta(
    inRooming: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    if (inRooming) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1A0A0A))
                .border(1.5.dp, Color(0xFFFF3B30), RoundedCornerShape(14.dp))
                .clickable { onRemove() }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    imageVector = Icons.Filled.NotificationsOff,
                    contentDescription = null,
                    tint = Color(0xFFFF3B30),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Remove from Rooming",
                    color = Color(0xFFFF3B30),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF2C2C2E))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                .clickable { onAdd() }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Add to Rooming",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ExplorerReadOnlyModeBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF0D2A4A).copy(alpha = 0.95f),
                        Color(0xFF0A1628).copy(alpha = 0.95f)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .border(1.dp, Color(0xFF1E88E5).copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF1E88E5), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Public,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = "EXPLORER MODE",
                    color = Color(0xFF64B5F6),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Viewing memories in this public space.",
                    color = Color(0xFFB0BEC5),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1565C0).copy(alpha = 0.45f))
                .border(1.dp, Color(0xFF42A5F5), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "READ ONLY",
                color = Color(0xFF90CAF9),
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
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
    
    var isPagerScrollEnabled by remember { mutableStateOf(true) }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState, 
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = isPagerScrollEnabled
        ) { page ->
             var scale by remember { mutableStateOf(1f) }
             var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
             
             LaunchedEffect(scale) {
                 isPagerScrollEnabled = scale == 1f
             }
             val state = rememberTransformableState { zoomChange, panChange, rotationChange ->
                 scale = (scale * zoomChange).coerceIn(1f, 5f)
                 
                 // Constrain offset when zooming out
                 if (scale == 1f) {
                     offset = androidx.compose.ui.geometry.Offset.Zero
                 } else {
                     offset += panChange
                 }
             }
             
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                 val isAudio = isAudioMemoryUri(media[page])
                 val isNote = isNoteMemoryUri(media[page])
                 val isVideo = isVideoMemoryUri(media[page])
                 if (isAudio) {
                      MusicPlayer(
                          audioUrl = media[page].toString(),
                          modifier = Modifier.align(Alignment.Center)
                      )
                 } else if (isNote) {
                     val parts = media[page].toString().removePrefix("NOTE:").split(":", limit = 2)
                     val noteTitle = parts.getOrElse(0) { "Note" }
                     val noteBody = parts.getOrElse(1) { "" }
                     Column(
                         modifier = Modifier
                             .fillMaxWidth()
                             .padding(24.dp)
                             .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
                             .padding(20.dp)
                     ) {
                         Text(noteTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                         Spacer(modifier = Modifier.height(12.dp))
                         Text(noteBody, color = Color(0xFFD0D0D0), fontSize = 16.sp, lineHeight = 22.sp)
                     }
                 } else if (isVideo) {
                      Box(
                          modifier = Modifier
                              .fillMaxWidth()
                              .height(400.dp)
                              .padding(16.dp),
                          contentAlignment = Alignment.Center
                      ) {
                          VideoPlayer(
                              videoUrl = media[page].toString(),
                              modifier = Modifier.fillMaxSize()
                          )
                      }
                 } else {
                     AsyncBase64Image(
                         itemData = media[page],
                         contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                         modifier = Modifier
                             .fillMaxSize()
                             .graphicsLayer(
                                 scaleX = scale,
                                 scaleY = scale,
                                 translationX = offset.x,
                                 translationY = offset.y
                             )
                             .then(
                                 if (scale > 1f) {
                                     Modifier.transformable(state)
                                 } else {
                                     Modifier
                                 }
                             )
                             .pointerInput(page) {
                                 detectTapGestures(onDoubleTap = {
                                     scale = if (scale > 1f) 1f else 2.5f
                                     offset = androidx.compose.ui.geometry.Offset.Zero
                                 })
                             }
                     )
                 }
             }
        }

        Text(
            text = "${pagerState.currentPage + 1} / ${media.size}",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 12.dp)
        )
        
        // Close Button
        IconButton(
            onClick = { viewModel.closeGalleryViewer() },
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp).windowInsetsPadding(WindowInsets.statusBars)
        ) {
             Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun CountdownTimer(targetTimeMillis: Long) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(targetTimeMillis) {
        while (currentTime < targetTimeMillis) {
            kotlinx.coroutines.delay(1000L)
            currentTime = System.currentTimeMillis()
        }
    }
    
    val diff = targetTimeMillis - currentTime
    if (diff <= 0) {
        Text("Unlocked!", color = Color(0xFF34C759), fontSize = 24.sp, fontWeight = FontWeight.Bold)
    } else {
        val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)
        val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(diff) % 24
        val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(diff) % 60
        val seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(diff) % 60
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (days > 0) {
                TimeUnitBox(value = days, unit = "DAYS")
                Text(":", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 14.dp))
            }
            TimeUnitBox(value = hours, unit = "HRS")
            Text(":", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 14.dp))
            TimeUnitBox(value = minutes, unit = "MINS")
            Text(":", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 14.dp))
            TimeUnitBox(value = seconds, unit = "SECS")
        }
    }
}

@Composable
fun TimeUnitBox(value: Long, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .background(Color(0xFF2C2C2E), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%02d", value),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(unit, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

// Room Details Bottom Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailsBottomSheet(
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
                Text(room.roomName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Status
                val isUnlocked = System.currentTimeMillis() >= room.unlockTime
                DetailRow("Status", if (isUnlocked) "Unlocked" else "Locked")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Unlock Date/Time
                val unlockDateText = if (room.unlockTime > 0) {
                    java.text.SimpleDateFormat("d MMM yyyy, HH:mm", java.util.Locale.US).format(
                        java.util.Date(room.unlockTime)
                    )
                } else {
                    "No unlock time set"
                }
                
                DetailRow("Unlock Time", unlockDateText)
                
                Spacer(modifier = Modifier.height(16.dp))
                DetailRow("Privacy", if (room.isPublic) "Public" else "Private")

                if (!room.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Description", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(room.description, color = Color.White, fontSize = 16.sp)
                }

                if (room.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Tags", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        room.tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(getRoomThemeColor(room.theme).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                    .border(1.dp, getRoomThemeColor(room.theme), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(text = "#$tag", color = getRoomThemeColor(room.theme), fontSize = 12.sp)
                            }
                        }
                    }
                }
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

@Composable
private fun ConnectedRoomItem(
    targetId: String,
    viewModel: ProfileViewModel,
    onClick: (com.dmb.bestbefore.data.models.TimeCapsuleRoom) -> Unit
) {
    val createdRooms by viewModel.createdRooms.collectAsState()
    var targetRoom by remember(targetId) { mutableStateOf(createdRooms.find { it.id == targetId }) }

    LaunchedEffect(targetId) {
        if (targetRoom == null) {
            targetRoom = viewModel.getRoomByIdFromRemote(targetId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .clickable {
                targetRoom?.let { onClick(it) }
            }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Link,
                "Connected",
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = targetRoom?.roomName ?: "Connected Room",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (targetRoom != null) "Tap to view room" else "Loading details...",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}
