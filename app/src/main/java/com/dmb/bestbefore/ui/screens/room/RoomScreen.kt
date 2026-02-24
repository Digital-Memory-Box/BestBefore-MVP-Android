package com.dmb.bestbefore.ui.screens.room

import android.Manifest
import android.content.pm.PackageManager

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmb.bestbefore.data.models.CalendarEvent
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RoomScreen(
    roomId: String,
    roomName: String,
    onNavigateBack: () -> Unit,
    viewModel: RoomViewModel = viewModel()
) {
    val context = LocalContext.current
    val showProfileMenu by viewModel.showProfileMenu.collectAsState()
    val showTimeCapsuleDialog by viewModel.showTimeCapsuleDialog.collectAsState()
    val showRoomInfoDialog by viewModel.showRoomInfoDialog.collectAsState()
    val showCalendarDialog by viewModel.showCalendarDialog.collectAsState()
    val lockEndTime by viewModel.lockEndTime.collectAsState()
    val countdownText by viewModel.countdownText.collectAsState()
    val calendarEvents by viewModel.calendarEvents.collectAsState()



    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.toggleCalendarDialog()
        }
    }



    LaunchedEffect(roomId) {
        viewModel.initialize(roomId, roomName)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1F1F1F))
    ) {
        // Simplified 3D view placeholder
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("3D Room View", color = Color.White, fontSize = 24.sp)
        }

        // Top bar icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.toggleTimeCapsuleDialog() }) {
                Icon(imageVector = Icons.Default.DateRange, contentDescription = "Time Capsule", tint = Color.White)
            }

            IconButton(onClick = { viewModel.toggleProfileMenu() }) {
                Icon(imageVector = Icons.Default.Person, contentDescription = "Profile", tint = Color.White)
            }
        }

        // Bottom bar icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                    viewModel.toggleCalendarDialog()
                } else {
                    calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                }
            }) {
                Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Calendar", tint = Color.White)
            }

            IconButton(onClick = { viewModel.toggleRoomInfoDialog() }) {
                Icon(imageVector = Icons.Default.Info, contentDescription = "Room Info", tint = Color.White)
            }
        }

        // Time capsule lock overlay
        if (lockEndTime != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    modifier = Modifier.padding(top = 80.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.8f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Time Capsule", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(countdownText, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showProfileMenu) {
            ProfileMenuDialog(
                roomName = roomName,
                onDismiss = { viewModel.toggleProfileMenu() },
                onRenameRoom = { newName -> viewModel.renameRoom(newName) },
                onDeleteRoom = { viewModel.deleteRoom(); onNavigateBack() },
                onLogout = onNavigateBack
            )
        }

        if (showTimeCapsuleDialog) {
            TimeCapsuleDialog(
                onDismiss = { viewModel.toggleTimeCapsuleDialog() },
                onStart = { h, m, s -> viewModel.startTimeCapsule(h, m, s) }
            )
        }

        if (showRoomInfoDialog) {
            RoomInfoDialog(roomName = roomName, roomId = roomId, onDismiss = { viewModel.toggleRoomInfoDialog() })
        }

        if (showCalendarDialog) {
            CalendarEventsDialog(
                events = calendarEvents,
                onDismiss = { viewModel.toggleCalendarDialog() },
                onEventSelected = { event -> viewModel.createRoomFromCalendarEvent(event); viewModel.toggleCalendarDialog() }
            )
        }
    }
}

@Composable
fun ProfileMenuDialog(roomName: String, onDismiss: () -> Unit, onRenameRoom: (String) -> Unit, onDeleteRoom: () -> Unit, onLogout: () -> Unit) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Account") },
        text = {
            Column {
                Text("Room: $roomName", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { showRenameDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Rename Room", modifier = Modifier.fillMaxWidth()) }
                TextButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.fillMaxWidth()) { Text("Delete Room", color = Color.Red, modifier = Modifier.fillMaxWidth()) }
                TextButton(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("Share Room", modifier = Modifier.fillMaxWidth()) }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Log Out", color = Color.Red, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(roomName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Room") },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Room Name") }) },
            confirmButton = { TextButton(onClick = { onRenameRoom(newName); showRenameDialog = false; onDismiss() }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Room") },
            text = { Text("Are you sure? This cannot be undone.") },
            confirmButton = { TextButton(onClick = { onDeleteRoom(); showDeleteConfirm = false }) { Text("Delete", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun RoomInfoDialog(roomName: String, roomId: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Room Info") },
        text = { Column { Text("Name: $roomName", fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(8.dp)); Text("ID: $roomId", fontSize = 12.sp, color = Color.Gray) } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

@Composable
fun TimeCapsuleDialog(onDismiss: () -> Unit, onStart: (Int, Int, Int) -> Unit) {
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(15) }
    var seconds by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Time Capsule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Set lock duration:")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hours", fontSize = 12.sp)
                        OutlinedTextField(value = hours.toString(), onValueChange = { hours = it.toIntOrNull()?.coerceIn(0, 23) ?: 0 }, modifier = Modifier.width(70.dp))
                    }
                    Text(":")
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minutes", fontSize = 12.sp)
                        OutlinedTextField(value = minutes.toString(), onValueChange = { minutes = it.toIntOrNull()?.coerceIn(0, 59) ?: 0 }, modifier = Modifier.width(70.dp))
                    }
                    Text(":")
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Seconds", fontSize = 12.sp)
                        OutlinedTextField(value = seconds.toString(), onValueChange = { seconds = it.toIntOrNull()?.coerceIn(0, 59) ?: 0 }, modifier = Modifier.width(70.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onStart(hours, minutes, seconds); onDismiss() }) { Text("Start") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun CalendarEventsDialog(events: List<CalendarEvent>, onDismiss: () -> Unit, onEventSelected: (CalendarEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Room from Calendar") },
        text = {
            if (events.isEmpty()) {
                Text("No upcoming events found")
            } else {
                LazyColumn {
                    items(events) { event ->
                        EventItem(event = event, onClick = { onEventSelected(event) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EventItem(event: CalendarEvent, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = event.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = dateFormat.format(event.startTime), fontSize = 12.sp, color = Color.Gray)
            if (event.location != null) {
                Text(text = event.location, fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}