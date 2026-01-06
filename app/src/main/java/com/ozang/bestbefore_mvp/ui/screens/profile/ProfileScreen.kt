package com.ozang.bestbefore_mvp.ui.screens.profile

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.ozang.bestbefore_mvp.data.models.TimeCapsuleRoom
import com.ozang.bestbefore_mvp.ui.components.OrbMenu
import com.ozang.bestbefore_mvp.ui.components.OrbMenuSimple

@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val createdRooms by viewModel.createdRooms.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Main profile content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Profile header
            Text(
                text = "Profile",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Menu items
            ProfileMenuItem(
                icon = Icons.Default.Add,
                text = "Create Room",
                onClick = { viewModel.startCreateRoom() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileMenuItem(
                icon = Icons.Default.Timer,
                text = "Time Capsule",
                onClick = { viewModel.showTimeCapsuleList() }
            )
        }

        // Bottom navigation
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Profile",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.clickable { onNavigateBack() }
            )
        }

        // Orb menu
        OrbMenu(
            modifier = Modifier.align(Alignment.CenterEnd),
            onAddClick = { viewModel.startCreateRoom() },
            onProfileClick = { /* Already on profile */ }
        )

        // Overlay container
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
                    ProfileStep.ROOM_NAME -> RoomNameStep(viewModel)
                    ProfileStep.TIME_CAPSULE -> TimeCapsuleStep(viewModel)
                    ProfileStep.NOTIFICATION -> NotificationStep(viewModel)
                    ProfileStep.ROOM_MODE -> RoomModeStep(viewModel)
                    ProfileStep.FINALIZE_PUBLIC, ProfileStep.FINALIZE_PRIVATE -> FinalizeStep(viewModel)
                    ProfileStep.COLLABORATION -> CollaborationStep(viewModel)
                    ProfileStep.TIME_CAPSULE_LIST -> TimeCapsuleListScreen(viewModel, createdRooms)
                    ProfileStep.ROOM_DETAIL -> RoomDetailScreen(viewModel)
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
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

    OverlayStepContainer(
        title = "Create Room",
        subtitle = "Choose a Room name",
        onBack = { viewModel.closeOverlay() },
        onNext = {
            if (roomName.isNotEmpty()) {
                viewModel.goToStep(ProfileStep.TIME_CAPSULE)
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

// Time Capsule Step
@Composable
private fun TimeCapsuleStep(viewModel: ProfileViewModel) {
    val days by viewModel.capsuleDays.collectAsState()
    val hours by viewModel.capsuleHours.collectAsState()

    OverlayStepContainer(
        title = "Create Room",
        subtitle = "Choose a Time Capsule",
        backText = "< Edit Room Name",
        onBack = { viewModel.goToStep(ProfileStep.ROOM_NAME) },
        onBackToProfile = { viewModel.closeOverlay() },
        onNext = { viewModel.goToStep(ProfileStep.NOTIFICATION) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NumberPickerColumn(
                label = "Days",
                value = days,
                range = 0..365,
                onValueChange = { viewModel.updateCapsuleTime(it, hours) }
            )
            NumberPickerColumn(
                label = "Hours",
                value = hours,
                range = 0..23,
                onValueChange = { viewModel.updateCapsuleTime(days, it) }
            )
        }
    }
}

// Notification Step
@Composable
private fun NotificationStep(viewModel: ProfileViewModel) {
    val days by viewModel.notifyDays.collectAsState()
    val hours by viewModel.notifyHours.collectAsState()

    OverlayStepContainer(
        title = "Create Room",
        subtitle = "Choose Notification Time",
        backText = "< Edit Time Capsule",
        onBack = { viewModel.goToStep(ProfileStep.TIME_CAPSULE) },
        onBackToProfile = { viewModel.closeOverlay() },
        onNext = { viewModel.goToStep(ProfileStep.ROOM_MODE) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NumberPickerColumn(
                label = "Days",
                value = days,
                range = 0..365,
                onValueChange = { viewModel.updateNotifyTime(it, hours) }
            )
            NumberPickerColumn(
                label = "Hours",
                value = hours,
                range = 0..23,
                onValueChange = { viewModel.updateNotifyTime(days, it) }
            )
        }
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
        onBack = { viewModel.goToStep(ProfileStep.NOTIFICATION) },
        onBackToProfile = { viewModel.closeOverlay() },
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

    OverlayStepContainer(
        title = "Create Room",
        subtitle = "\"$roomName\"\nwill be created.",
        backText = "< Edit Room Mode",
        onBack = { viewModel.goToStep(ProfileStep.ROOM_MODE) },
        onBackToProfile = { viewModel.closeOverlay() },
        onNext = {
            if (isCollaboration) {
                viewModel.goToStep(ProfileStep.COLLABORATION)
            } else {
                viewModel.finalizeRoom()
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

    OverlayStepContainer(
        title = "Collaboration",
        subtitle = "\"$roomName\"",
        backText = "< Back",
        onBack = { viewModel.goToStep(ProfileStep.FINALIZE_PUBLIC) },
        onBackToProfile = { viewModel.closeOverlay() },
        onNext = { viewModel.finalizeRoom() }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeButton(
                text = "Share Link",
                isSelected = shareLink,
                onClick = { shareLink = true; inviteRoomer = false },
                modifier = Modifier.fillMaxWidth()
            )
            ModeButton(
                text = "Invite Roomer",
                isSelected = inviteRoomer,
                onClick = { inviteRoomer = true; shareLink = false },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Time Capsule List Screen
@Composable
private fun TimeCapsuleListScreen(
    viewModel: ProfileViewModel,
    rooms: List<TimeCapsuleRoom>
) {
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
            Text(
                text = "Profile",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.clickable { viewModel.closeOverlay() }
            )
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = room.roomName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = buildString {
                    if (room.notificationDays > 0) append("${room.notificationDays}d ")
                    if (room.notificationHours > 0) append("${room.notificationHours}h")
                    if (isEmpty()) append("No notification")
                },
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${room.capsuleDays}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Days",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${room.capsuleHours}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Hours",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

// Room Detail Screen
@Composable
private fun RoomDetailScreen(viewModel: ProfileViewModel) {
    val room by viewModel.selectedRoom.collectAsState()

    room?.let { selectedRoom ->
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
                Text(
                    text = "Profile",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.clickable { viewModel.closeOverlay() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Media upload options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MediaButton(icon = Icons.Default.Photo, label = "Photo archive")
                MediaButton(icon = Icons.Default.AttachFile, label = "Choose file")
                MediaButton(icon = Icons.Default.CameraAlt, label = "Use camera")
                MediaButton(icon = Icons.Default.Mic, label = "Record Audio")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Memories",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No memories added yet.\nTap an option above to add content.",
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MediaButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { /* Handle media action */ }
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
    backText: String = "< Back",
    onBack: () -> Unit,
    onBackToProfile: (() -> Unit)? = null,
    onNext: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

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
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            content()

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = backText,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.clickable { onBack() }
            )

            onBackToProfile?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "< Back to Profile",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.clickable { it() }
                )
            }
        }

        // Next button (orb)
        OrbMenuSimple(
            modifier = Modifier.align(Alignment.CenterEnd),
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
