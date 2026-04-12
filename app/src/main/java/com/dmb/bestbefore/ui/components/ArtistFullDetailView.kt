package com.dmb.bestbefore.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

@Composable
fun ArtistFullDetailView(
    room: RoomObject,
    email: String = "",
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Room Title Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = room.name,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // 2. Focused Card representation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Glow
                Box(
                    modifier = Modifier
                        .size(width = 240.dp, height = 360.dp)
                        .scale(1.1f)
                        .blur(40.dp)
                        .background(room.themeColor.copy(alpha = 0.4f), RoundedCornerShape(32.dp))
                )

                // Placeholder for UnsplashImageView
                Box(
                    modifier = Modifier
                        .size(width = 240.dp, height = 360.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF19192E), Color(0xFF14213D))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Simulates image cover area
                }
            }

            // 3. Artist Detail Block (Footer area)
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Profile + Username
                Row(
                    modifier = Modifier.clickable { /* showingExternalProfile mock */ },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(room.themeColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black)
                    }
                    
                    val userName = email.substringBefore("@").ifEmpty { "artist" }
                    Text("@$userName", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Description ScrollArea
                if (!room.description.isNullOrEmpty()) {
                    Box(modifier = Modifier.height(150.dp)) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(
                                text = room.description!!,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // See Less Button
                Text(
                    text = "See Less",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = room.themeColor,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { onDismiss() }
                )

                // Tags Section
                if (room.tags.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text("TAGS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            room.tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text("#$tag", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }

        // Top-Level CD Button ZStack mapping
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp, end = 20.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                        .clip(CircleShape)
                        .clickable { /* Show Soundcloud player modal mock */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "opticaldisc", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ArtistFullDetailViewPreviewUpdated() {
    ArtistFullDetailView(
        room = RoomObject(
            name = "Starlight", 
            theme = "cyberpunk", 
            description = "This is a detailed description spanning multiple lines describing the music elements inside.",
            tags = listOf("music", "vcl")
        ),
        email = "john@music.dev",
        onDismiss = {}
    )
}
