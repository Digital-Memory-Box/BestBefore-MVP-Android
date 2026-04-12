package com.dmb.bestbefore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoomingView(
    onScan: () -> Unit = {},
    onRoomSelected: (RoomObject) -> Unit = {}
) {
    var searchText by remember { mutableStateOf("") }
    val isLoading = false // Mocked structure
    val mockRooms = listOf(
        RoomObject(name = "Ocean Soul", tags = listOf("nature"), capsuleDurationDays = 10, theme = "ocean"),
        RoomObject(name = "Cyber City", tags = listOf("tech"), capsuleDurationDays = 5, theme = "cyberpunk")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rooming",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Music",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )

                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.clickable { onScan() }, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = "Scan", tint = Color.White, modifier = Modifier.size(22.dp))
                            Text("Scan", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        }
                    }
                }
            }

            // Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Search by name, owner, or tags...", color = Color.Gray) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (mockRooms.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray.copy(0.3f), modifier = Modifier.size(50.dp))
                    Text("No public rooms discovered yet.", color = Color.Gray)
                }
            } else {
                // Featured
                if (mockRooms.isNotEmpty()) {
                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        RoomingCardView(room = mockRooms[0], height = 320, onClick = { onRoomSelected(mockRooms[0]) })
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Others
                Column(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(horizontal = 24.dp)) {
                    mockRooms.drop(1).forEach { room ->
                        RoomingCardView(room = room, height = 220, onClick = { onRoomSelected(room) })
                    }
                }
            }
        }
    }
}

@Composable
fun RoomingCardView(room: RoomObject, height: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .background(Color.DarkGray, RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        // Mock Image Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF14213D), Color.Black)), RoundedCornerShape(24.dp))
        )
        
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.8f))), RoundedCornerShape(24.dp))
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(room.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                text = "Time Capsule: ${room.capsuleDurationDays}d ${room.capsuleDurationHours}h",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                "Click to view details >",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Preview
@Composable
fun RoomingViewPreview() {
    RoomingView()
}
