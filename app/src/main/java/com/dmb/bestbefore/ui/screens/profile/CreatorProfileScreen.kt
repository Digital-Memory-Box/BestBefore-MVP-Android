package com.dmb.bestbefore.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.*
import androidx.compose.ui.text.style.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.dmb.bestbefore.data.api.models.PublicProfileDto
import com.dmb.bestbefore.data.api.models.PublicRoomDto
import java.text.DecimalFormat
import com.dmb.bestbefore.ui.components.ProfileAvatar

@Composable
fun CreatorProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onNavigateToRoom: (String, String) -> Unit,
    viewModel: CreatorProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val profile by viewModel.profileState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadProfile(context, userId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onNavigateBack() }
            )
            
            Text(
                text = "Creator Profile",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 28.dp), // Balance out the back icon
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        if (isLoading && profile == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF007AFF))
            }
        } else if (profile == null) {
            val errorMsg by viewModel.error.collectAsState()
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = errorMsg ?: "Failed to load profile.",
                    color = Color.Red,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            val p = profile!!
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // User Card (Prominent Header)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1C1C1E).copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile Photo
                        ProfileAvatar(
                            imageUri = p.profileImageUrl,
                            size = 100.dp,
                            accentColor = Color.White
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Name and Artist tag
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val nameText = if (p.name?.startsWith("@") == true) p.name else "@${p.name ?: "user"}"
                            Text(
                                text = nameText,
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val nameText2 = p.name?.takeIf { it.isNotBlank() } ?: "User"
                                Text(
                                    text = nameText2,
                                    color = Color.White,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                if (p.userType?.lowercase() == "artist") {
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF007AFF), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Artist",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            
                            if (!p.bio.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = p.bio!!,
                                    color = Color(0xFF8E8E93),
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Rooming", color = Color.Gray, fontSize = 12.sp)
                                    Text(
                                        text = formatCount(p.roomingCount),
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Roomers", color = Color.Gray, fontSize = 12.sp)
                                    Text(
                                        text = formatCount(p.roomersCount),
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Info Cards (Rooms / Memories)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF1C1C1E), RoundedCornerShape(20.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Icon(Icons.Default.Home, null, tint = Color(0xFF007AFF), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = p.publicRooms.size.toString(),
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = "Rooms", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF1C1C1E), RoundedCornerShape(20.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Icon(Icons.Default.PhotoLibrary, null, tint = Color(0xFFAF52DE), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = p.memoriesCount.toString(),
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = "Memories", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
                
                item {
                    Text(
                        text = "Public Rooms",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                
                // Public Rooms Grid
                gridItems(
                    items = p.publicRooms,
                    columnCount = 2,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) { room ->
                    PublicRoomCard(room = room, onClick = { onNavigateToRoom(room.id, room.name) })
                }
            }
        }
    }
}

@Composable
fun PublicRoomCard(room: PublicRoomDto, onClick: () -> Unit) {
    val themeColor = parseThemeColor(room.theme)
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        val searchKeyword = room.name.lowercase()
            .replace("'s", "")
            .split(" ")
            .filter { it.length > 3 && it !in listOf("room", "hallway", "best", "before", "collection") }
            .take(2)
            .joinToString(",")
            .takeIf { it.isNotBlank() } ?: "abstract"
        val roomImage = room.photos.firstOrNull()?.url ?: "https://loremflickr.com/640/800/$searchKeyword"

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            themeColor.copy(alpha = 0.6f),
                            themeColor.copy(alpha = 0.2f),
                            Color(0xFF121212)
                        )
                    )
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (room.photos.isNotEmpty()) {
                AsyncImage(
                    model = room.photos.first().url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF42A5F5),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = room.name,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        
        if (!room.description.isNullOrEmpty()) {
            Text(
                text = room.description,
                color = Color.Gray,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private fun formatCount(count: Int): String {
    if (count < 1000) return count.toString()
    val df = DecimalFormat("#.#")
    return df.format(count / 1000.0) + "k"
}

// Same logic as in HallwayScreen
private fun parseThemeColor(hex: String?, fallback: Color = Color(0xFF007AFF)): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val cleanHex = if (hex.startsWith("#")) hex else "#$hex"
        Color(android.graphics.Color.parseColor(cleanHex))
    } catch (e: Exception) {
        fallback
    }
}

/**
 * Extension for LazyColumn to support grid-like layouts for a dynamic number of items.
 */
fun <T> androidx.compose.foundation.lazy.LazyListScope.gridItems(
    items: List<T>,
    columnCount: Int,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(12.dp),
    itemContent: @Composable (T) -> Unit
) {
    val rowCount = (items.size + columnCount - 1) / columnCount
    items(rowCount) { rowIndex ->
        Row(
            modifier = modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = horizontalArrangement
        ) {
            for (columnIndex in 0 until columnCount) {
                val itemIndex = rowIndex * columnCount + columnIndex
                if (itemIndex < items.size) {
                    Box(modifier = Modifier.weight(1f)) {
                        itemContent(items[itemIndex])
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
