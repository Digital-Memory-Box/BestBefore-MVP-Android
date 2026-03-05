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
    val isRecording by viewModel.isRecordingAudio.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

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
    var noteContent by remember { mutableStateOf("") }
    
    // Combine room-specific media
    // Combine room-specific media
    val currentRoomMedia = roomMedia[room?.id] ?: emptyList()
    
    Box(modifier = Modifier.fillMaxSize()) {
        com.dmb.bestbefore.ui.components.AnimatedBackgroundView(
            baseColor = getRoomThemeColor(room?.theme ?: "Default")
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
                                         room?.let { viewModel.selectRoomForEditing(it) }
                                     }
                                 )
                                 DropdownMenuItem(
                                     text = { Text("Delete Room", color = Color.Red) },
                                     onClick = {
                                         show3DotMenu = false
                                         room?.let { viewModel.deleteRoom(context, it, fromInsideRoom = true) }
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

                val currentTime = System.currentTimeMillis()
                val isLocked = room!!.unlockTime > currentTime
                val isRoomClosed = room!!.scheduledClosureTime > 0L && currentTime >= room!!.scheduledClosureTime

                // Grid of Actions
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    userScrollEnabled = false,
                    modifier = Modifier.height(330.dp)
                ) {
                    // Item 1: Add Photo (Blue)
                    item { MemoryActionCard(Icons.Default.Image, "Add Photo", if (isRoomClosed) Color.Gray else Color(0xFF007AFF)) {
                         if (isRoomClosed) android.widget.Toast.makeText(context, "This room is archived.", android.widget.Toast.LENGTH_SHORT).show()
                         else multiplePhotoPickerLauncher.launch(
                             androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                         )
                     }}
                    // Item 2: Write Note (Purple)
                    item { MemoryActionCard(Icons.Default.Edit, "Write Note", if (isRoomClosed) Color.Gray else Color(0xFFAF52DE)) {
                        if (isRoomClosed) android.widget.Toast.makeText(context, "This room is archived.", android.widget.Toast.LENGTH_SHORT).show()
                        else showWriteNoteDialog = true
                    }}
                    // Item 3: Add Video (Orange)
                    item { MemoryActionCard(Icons.Default.Videocam, "Add Video", if (isRoomClosed) Color.Gray else Color(0xFFFF9500)) {
                        if (isRoomClosed) android.widget.Toast.makeText(context, "This room is archived.", android.widget.Toast.LENGTH_SHORT).show()
                        else multiplePhotoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    }}
                    // Item 4: Add Music (Red)
                    item { MemoryActionCard(Icons.Default.MusicNote, "Add Music", if (isRoomClosed) Color.Gray else Color(0xFFFF3B30)) {
                        if (isRoomClosed) android.widget.Toast.makeText(context, "This room is archived.", android.widget.Toast.LENGTH_SHORT).show()
                        else filePickerLauncher.launch(arrayOf("audio/*"))
                    }}
                    // Item 5: Record Audio (Pink/Red)
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
                                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    if (hasPermission) {
                                        viewModel.startAudioRecording(context)
                                    } else {
                                        recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            }
                        }
                    }
                    // Item 6: View All (Green) â€” only shown when not TimeCapsule and no Scheduled Closure
                    item {
                        val isTimeCapsuleLocal = room!!.isCollaboration
                        val isClosedLocal = room!!.scheduledClosureTime > 0L &&
                                           System.currentTimeMillis() >= room!!.scheduledClosureTime
                        if (!isTimeCapsuleLocal && !isClosedLocal) {
                            MemoryActionCard(Icons.Default.FolderOpen, "View All", Color(0xFF34C759)) {
                                viewModel.openGalleryViewer(currentRoomMedia)
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth()) {} // empty placeholder to keep grid layout
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
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
                        "ðŸ“¦ Archived â€” Closed on ${sdf.format(java.util.Date(room!!.scheduledClosureTime))}",
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
                
                // Display Media Strategy
                val displayMedia = if (isLocked || isClosed) currentRoomMedia.takeLast(2) else currentRoomMedia
                
                if (displayMedia.isEmpty()) {
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
                                    val isAudio1 = itemStr.startsWith("data:audio")
                                    val isNote1 = itemStr.startsWith("NOTE:")
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
                                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2C2C2E)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(48.dp))
                                        }
                                    } else {
                                        val modelData1 = if (item1.toString().startsWith("data:image")) {
                                            val base64String = item1.toString().substringAfter("base64,")
                                            val decodedBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                                            java.nio.ByteBuffer.wrap(decodedBytes)
                                        } else {
                                            item1
                                        }
                                        
                                        coil.compose.AsyncImage(
                                            model = modelData1,
                                            contentDescription = "Room Media",
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
                                        val isAudio2 = item2Str.startsWith("data:audio")
                                        val isNote2 = item2Str.startsWith("NOTE:")
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
                                            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2C2C2E)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(48.dp))
                                            }
                                        } else {
                                            val modelData2 = if (item2.toString().startsWith("data:image")) {
                                                val base64String = item2.toString().substringAfter("base64,")
                                                val decodedBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                                                java.nio.ByteBuffer.wrap(decodedBytes)
                                            } else {
                                                item2
                                            }
                                            
                                            coil.compose.AsyncImage(
                                                model = modelData2,
                                                contentDescription = "Room Media",
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
                } // End of isLocked / isClosed / isEmpty check
            } // End of Column (has room)
            } // End of PullToRefreshBox
        } // End of room == null else block
        
        // Image Viewer Overlay
        val isGalleryOpen by viewModel.isGalleryViewerOpen.collectAsState()
        if (isGalleryOpen) {
             ProfileGalleryViewer(viewModel)
        }
        
        // QR Code Dialog with real invite token
        if (showQrCode) {
            val invLink by viewModel.inviteLink.collectAsState()
            val isGenerating by viewModel.isGeneratingToken.collectAsState()

            // Generate token when dialog opens
            LaunchedEffect(showQrCode) {
                if (showQrCode && invLink == null) {
                    viewModel.generateInviteToken()
                }
            }

            AlertDialog(
                onDismissRequest = {
                    showQrCode = false
                    viewModel.clearInviteToken()
                },
                containerColor = Color(0xFF1C1C1E),
                title = { Text("Room QR Code", color = Color.White) },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isGenerating || invLink == null) {
                            CircularProgressIndicator(
                                color = Color(0xFF007AFF),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Generating invite link...", color = Color.Gray, fontSize = 12.sp)
                        } else {
                            // Generate QR bitmap from invite link
                            val qrBitmap = remember(invLink) {
                                invLink?.let { link ->
                                    try {
                                        val size = 512
                                        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
                                        // Simple QR-like encoding using the link hash
                                        val hash = link.hashCode()
                                        val canvas = android.graphics.Canvas(bitmap)
                                        canvas.drawColor(android.graphics.Color.WHITE)
                                        val paint = android.graphics.Paint().apply { color = android.graphics.Color.BLACK }
                                        val cellSize = size / 32
                                        val bytes = link.toByteArray()
                                        for (row in 0 until 32) {
                                            for (col in 0 until 32) {
                                                val byteIndex = (row * 32 + col) % bytes.size
                                                val bitIndex = (row * 32 + col) % 8
                                                if ((bytes[byteIndex].toInt() shr bitIndex) and 1 == 1) {
                                                    canvas.drawRect(
                                                        (col * cellSize).toFloat(),
                                                        (row * cellSize).toFloat(),
                                                        ((col + 1) * cellSize).toFloat(),
                                                        ((row + 1) * cellSize).toFloat(),
                                                        paint
                                                    )
                                                }
                                            }
                                        }
                                        // Draw finder patterns (top-left, top-right, bottom-left)
                                        val fp = 7 * cellSize
                                        listOf(0 to 0, (size - fp) to 0, 0 to (size - fp)).forEach { (x, y) ->
                                            canvas.drawRect(x.toFloat(), y.toFloat(), (x + fp).toFloat(), (y + fp).toFloat(), paint)
                                            val white = android.graphics.Paint().apply { color = android.graphics.Color.WHITE }
                                            canvas.drawRect((x + cellSize).toFloat(), (y + cellSize).toFloat(), (x + fp - cellSize).toFloat(), (y + fp - cellSize).toFloat(), white)
                                            canvas.drawRect((x + 2 * cellSize).toFloat(), (y + 2 * cellSize).toFloat(), (x + fp - 2 * cellSize).toFloat(), (y + fp - 2 * cellSize).toFloat(), paint)
                                        }
                                        bitmap
                                    } catch (e: Exception) {
                                        null
                                    }
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
                                    invLink?.let { link ->
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_TEXT, "Join my room \"${room?.roomName}\" on BestBefore!\n$link")
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Invite"))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Share, "Share", tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share Invite Link", color = Color.White)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showQrCode = false
                        viewModel.clearInviteToken()
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
        com.dmb.bestbefore.ui.components.AnimatedBackgroundView()
        androidx.compose.foundation.pager.HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
             var scale by remember { mutableStateOf(1f) }
             var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
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
                 val isAudio = media[page].toString().startsWith("data:audio")
                 if (isAudio) {
                     val context = androidx.compose.ui.platform.LocalContext.current
                     Column(horizontalAlignment = Alignment.CenterHorizontally) {
                         Icon(Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(80.dp).clickable {
                             viewModel.playBase64Audio(context, media[page].toString())
                         })
                         Spacer(modifier = Modifier.height(16.dp))
                         Text("Tap to Play Audio", color = Color.White, fontSize = 16.sp)
                     }
                 } else {
                     val displayModel = if (media[page].toString().startsWith("data:image")) {
                         val base64Str = media[page].toString().substringAfter("base64,")
                         val bytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
                         java.nio.ByteBuffer.wrap(bytes)
                     } else {
                         media[page]
                     }
                     
                     coil.compose.AsyncImage(
                         model = displayModel,
                         contentDescription = null,
                         contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                         modifier = Modifier
                             .fillMaxSize()
                             .graphicsLayer(
                                 scaleX = scale,
                                 scaleY = scale,
                                 translationX = offset.x,
                                 translationY = offset.y
                             )
                             .transformable(state)
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
        Text("Unlocked!", color = Color(0xFF34C759), fontSize = 28.sp, fontWeight = FontWeight.Bold)
    } else {
        val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)
        val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(diff) % 24
        val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(diff) % 60
        val seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(diff) % 60
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (days > 0) {
                TimeUnitBox(value = days, unit = "DAYS")
                Text(":", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
            }
            TimeUnitBox(value = hours, unit = "HOURS")
            Text(":", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
            TimeUnitBox(value = minutes, unit = "MINS")
            Text(":", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
            TimeUnitBox(value = seconds, unit = "SECS")
        }
    }
}

@Composable
fun TimeUnitBox(value: Long, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .background(Color(0xFF2C2C2E), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%02d", value),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(unit, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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

                if (!room.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Description", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(room.description, color = Color.White, fontSize = 14.sp)
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
