package com.dmb.bestbefore.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dmb.bestbefore.ui.components.RoomObject // Reusing mock model from earlier conversations

enum class KnockStatus {
    IDLE, REQUESTED, ACCEPTED
}

enum class PrivacyStatus {
    PRIVATE_ACCOUNT, PUBLIC_ACCOUNT
}

@Composable
fun ExternalUserProfileView(
    room: RoomObject,
    email: String,
    onDismiss: () -> Unit
) {
    var currentStatus by remember { mutableStateOf(KnockStatus.IDLE) }
    
    val userName = email.substringBefore("@").ifEmpty { "artist" }
    val accentColor = room.themeColor
    
    val buttonLabel = when (currentStatus) {
        KnockStatus.IDLE -> "Knock"
        KnockStatus.REQUESTED -> "Knocking..."
        KnockStatus.ACCEPTED -> "Rooming"
    }

    val buttonColorTarget = when (currentStatus) {
        KnockStatus.IDLE -> accentColor
        KnockStatus.REQUESTED -> Color.White.copy(alpha = 0.12f)
        KnockStatus.ACCEPTED -> Color.White.copy(alpha = 0.2f)
    }

    val buttonColor by animateColorAsState(
        targetValue = buttonColorTarget,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "btnColorAnim"
    )
    
    val buttonTextColor = if (currentStatus == KnockStatus.IDLE) Color.Black else Color.White
    
    val isPrivateAccount = userName.lowercase() == "maya" || userName.lowercase() == "artist1"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp) // chevron.down in bold
                        .clickable { onDismiss() }
                )
                
                Text(
                    text = "Creator Profile",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Transparent, // Hidden for symmetry
                    modifier = Modifier.size(32.dp)
                )
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // User Card (Mock implementation based on SwiftUI usage)
                UserCard(
                    name = userName,
                    biography = "Creative mind exploring the digital horizon.",
                    roomingCount = "124",
                    roomersCount = "2.8k",
                    accentColor = accentColor,
                    privacyStatus = if (isPrivateAccount) PrivacyStatus.PRIVATE_ACCOUNT else PrivacyStatus.PUBLIC_ACCOUNT
                )

                // Stats Overview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    ExternalStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Rooms",
                        value = "8",
                        icon = Icons.Default.Home, // Using fallback material icon
                        color = accentColor
                    )
                    ExternalStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Memories",
                        value = "452",
                        icon = Icons.Default.Photo, // Using fallback material icon
                        color = Color(0xFF9C27B0) // Purple
                    )
                }

                // User's Rooms List
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Featured Rooms",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    // Fixed 2x2 grid representing LazyVGrid
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FeaturedRoomCard(modifier = Modifier.weight(1f), name = room.name, color = accentColor)
                        FeaturedRoomCard(modifier = Modifier.weight(1f), name = "Morning Pulse", color = Color(0xFF009688)) // Teal
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FeaturedRoomCard(modifier = Modifier.weight(1f), name = "Neon Nights", color = Color(0xFF9C27B0)) // Purple
                        FeaturedRoomCard(modifier = Modifier.weight(1f), name = "Echoes", color = Color(0xFFFF9800)) // Orange
                    }
                }

                // Knock Button
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(buttonColor)
                        .border(
                            BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            if (currentStatus == KnockStatus.IDLE) {
                                currentStatus = if (isPrivateAccount) KnockStatus.REQUESTED else KnockStatus.ACCEPTED
                            } else {
                                currentStatus = KnockStatus.IDLE
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (currentStatus == KnockStatus.REQUESTED) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(20.dp) // ProgressView scaling
                                    .scale(0.8f),
                                strokeWidth = 2.dp
                            )
                        }
                        Text(
                            text = buttonLabel,
                            color = buttonTextColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Component: External Stat Card
@Composable
fun ExternalStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = color,
            modifier = Modifier.size(20.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

// Component: Featured Room Card
@Composable
fun FeaturedRoomCard(
    modifier: Modifier = Modifier,
    name: String,
    color: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(color.copy(alpha = 0.4f), color.copy(alpha = 0.1f))
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Using placeholder "star" for "sparkles"
            Icon(
                imageVector = Icons.Default.Person, // Fallback icon conceptually 
                contentDescription = null,
                tint = color
            )
        }
        
        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// Injected mock UserCard for matching the missing Swift definition
@Composable
fun UserCard(
    name: String,
    biography: String,
    roomingCount: String,
    roomersCount: String,
    accentColor: Color,
    privacyStatus: PrivacyStatus
) {
    // A standard aesthetic card bridging the missing gap
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "@$name", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = biography, color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = roomingCount, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(text = "Rooming", color = Color.Gray, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = roomersCount, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(text = "Roomers", color = Color.Gray, fontSize = 12.sp)
                }
            }
            if (privacyStatus == PrivacyStatus.PRIVATE_ACCOUNT) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.background(Color.Black.copy(alpha=0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(text = "Private Account", color = Color.Gray, fontSize = 10.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExternalUserProfileViewPreview() {
    ExternalUserProfileView(
        room = RoomObject(
            name = "Studio Vibes",
            ownerEmail = null,
            description = null,
            tags = emptyList(),
            theme = "sunset"
        ),
        email = "maya@example.com", // Will trigger private account logic
        onDismiss = {}
    )
}
