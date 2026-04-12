package com.dmb.bestbefore.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

// We reuse the standard local RoomObject data structure for compiling
// (Assumed imported from the same package as previously created comps)

@Composable
fun HallwayView() {
    var selectedTab by remember { mutableIntStateOf(1) } // 0: Roaming, 1: Hallway, 2: Artists
    var searchText by remember { mutableStateOf("") }
    var selectedFilterTag by remember { mutableStateOf<String?>(null) }
    
    var showingSoundCloudModal by remember { mutableStateOf(false) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    
    val selectedTheme by remember { mutableStateOf("Default") }
    val applyAccentToAll by remember { mutableStateOf(false) }
    
    // Mock Data to recreate the design natively
    val mockRooms = remember {
        listOf(
            RoomObject(name = "Room 1", ownerEmail = "dj@test.com", description = "Morning vibes.", tags = listOf("trip", "music"), themeColor = Color(0xFFE91E63)),
            RoomObject(name = "Room 2", ownerEmail = "artist@test.com", description = "Deep focus.", tags = listOf("science"), themeColor = Color(0xFF2196F3))
        )
    }
    
    var selectedIndex by remember { mutableIntStateOf(0) }
    val currentRoom = mockRooms.getOrElse(selectedIndex) { mockRooms.first() }
    val accentColor = currentRoom.themeColor
    val iconColor = if (applyAccentToAll) accentColor else Color.White
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (selectedTab == 1 || selectedTab == 2) {
                // --- Premium Header ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 25.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedTab == 1) "Hallway" else "Artists",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (applyAccentToAll && selectedTheme != "Default") accentColor else Color.White
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Icon(
                            imageVector = Icons.Default.List, // music.note.list fallback
                            contentDescription = "MiniPlayer",
                            tint = if (applyAccentToAll && selectedTheme != "Default") accentColor else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = if (applyAccentToAll && selectedTheme != "Default") accentColor else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                
                // --- Search Bar ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 15.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (applyAccentToAll && selectedTheme != "Default") accentColor else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Search...",
                        color = if (applyAccentToAll && selectedTheme != "Default") accentColor else Color.White.copy(alpha = 0.6f)
                    )
                }
                
                // --- Tag Filter Bar ---
                val presetTags = listOf("trip", "music", "science", "party", "family")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // All Button
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (selectedFilterTag == null) accentColor else Color.White.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                            .clickable { selectedFilterTag = null }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "All",
                            color = if (selectedFilterTag == null) iconColor else (if (applyAccentToAll) iconColor else Color.White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Loop tags
                    presetTags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (selectedFilterTag == tag) accentColor else Color.White.copy(alpha = 0.1f),
                                    shape = CircleShape
                                )
                                .clickable { selectedFilterTag = tag }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                color = if (selectedFilterTag == tag) iconColor else (if (applyAccentToAll) iconColor else Color.White),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // --- Horizontal Carousel Area ---
                // Title + Card Stack + CD Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .zIndex(2f),
                    contentAlignment = Alignment.TopTrailing
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = currentRoom.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (applyAccentToAll && selectedTheme != "Default") accentColor else Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        )
                        
                        Box(modifier = Modifier.height(320.dp).padding(top = 6.dp)) {
                            // Render previously requested CardStackView Component
                            CardStackView(
                                rooms = mockRooms,
                                initialSelectedIndex = selectedIndex,
                                isMenuHidden = false
                            )
                        }
                    }

                    // CD Button
                    Box(
                        modifier = Modifier
                            .padding(trailing = 20.dp, top = 4.dp)
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.12f), CircleShape)
                            .clip(CircleShape)
                            .clickable { showingSoundCloudModal = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow, // opticaldisc mock
                            contentDescription = "CD",
                            tint = if (applyAccentToAll && selectedTheme != "Default") accentColor else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                
                // --- Artist Detail Section ---
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(accentColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            }
                            val username = currentRoom.ownerEmail?.substringBefore("@") ?: "artist"
                            Text(
                                "@$username",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (applyAccentToAll && selectedTheme != "Default") accentColor else Color.White
                            )
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            currentRoom.tags.take(2).forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "#$tag",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (applyAccentToAll && selectedTheme != "Default") accentColor else Color.White
                                    )
                                }
                            }
                            if (currentRoom.tags.size > 2) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "+",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (applyAccentToAll && selectedTheme != "Default") accentColor else Color.White
                                    )
                                }
                            }
                        }
                    }
                    
                    Text(
                        text = currentRoom.description ?: "No description provided.",
                        fontSize = 14.sp,
                        color = (if (applyAccentToAll && selectedTheme != "Default") accentColor else Color.White).copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = "See All",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.clickable { isDescriptionExpanded = true }
                    )
                }
            } else {
                // Mock Roaming View for compilation and visual fallback
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Roaming View Content", color = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Bottom Nav
            HallwayBottomNav(
                selectedTab = selectedTab,
                accentColor = accentColor,
                applyAccentToAll = applyAccentToAll && selectedTheme != "Default",
                onTabSelected = { selectedTab = it }
            )
        }
        
        // --- SoundCloud Player Modal Overlay ---
        AnimatedVisibility(
            visible = showingSoundCloudModal,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.zIndex(2000f)
        ) {
            SoundCloudPlayerModal(
                room = currentRoom,
                onDismiss = { showingSoundCloudModal = false }
            )
        }
        
        // --- Expanded Description Full Screen ---
        AnimatedVisibility(
            visible = isDescriptionExpanded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.zIndex(1000f)
        ) {
            ArtistFullDetailView(
                room = currentRoom,
                email = currentRoom.ownerEmail ?: "",
                onDismiss = { isDescriptionExpanded = false }
            )
        }
    }
}

@Composable
fun HallwayBottomNav(
    selectedTab: Int,
    accentColor: Color,
    applyAccentToAll: Boolean,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(horizontal = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TabItem("Roaming", selectedTab == 0, accentColor, applyAccentToAll) { onTabSelected(0) }
        TabItem("Hallway", selectedTab == 1, accentColor, applyAccentToAll) { onTabSelected(1) }
        TabItem("Artists", selectedTab == 2, accentColor, applyAccentToAll) { onTabSelected(2) }
    }
}

@Composable
fun TabItem(
    title: String,
    isSelected: Boolean,
    accentColor: Color,
    applyAccentToAll: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (isSelected) {
            Text("▽", fontSize = 10.sp, color = if (applyAccentToAll) accentColor else Color.White)
        } else {
            Spacer(modifier = Modifier.height(13.dp))
        }
        Text(
            text = title,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) (if (applyAccentToAll) accentColor else Color.White) else Color.Gray
        )
    }
}

@Composable
fun SoundCloudPlayerModal(
    room: RoomObject,
    onDismiss: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Semi-transparent Backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable { onDismiss() }
        )
        
        // Modal Sheet
        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.Black.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(40.dp)
                    )
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(40.dp)
                    ),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 60.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp, bottom = 30.dp)
                            .size(width = 40.dp, height = 4.dp)
                            .background(Color.White.copy(alpha = 0.3f), CircleShape)
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(35.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // High-Res Artwork with Theme-Aware Glow
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .blur(80.dp)
                                    .alpha(0.6f)
                                    .background(room.themeColor, CircleShape)
                            )
                            // Artwork Placeholder
                            Box(
                                modifier = Modifier
                                    .size(280.dp)
                                    .background(room.themeColor.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.List, // music.note fallback
                                    contentDescription = null,
                                    tint = room.themeColor,
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                        }
                        
                        // Metadata Section
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = room.name.uppercase(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 30.dp)
                            )
                            
                            Text(
                                text = "jergkoppf",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 15.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.List, // waveform fallback
                                    contentDescription = null,
                                    tint = room.themeColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "NOW PLAYING FROM ${room.name.uppercase()}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = room.themeColor,
                                    letterSpacing = 2.sp
                                )
                            }
                        }

                        // Premium Listen Button (Orange)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp)
                                .background(Color(0xFFFF5500), RoundedCornerShape(16.dp))
                                .padding(vertical = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Listen on SoundCloud", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HallwayViewPreview() {
    HallwayView()
}
