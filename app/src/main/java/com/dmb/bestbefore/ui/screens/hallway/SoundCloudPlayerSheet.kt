package com.dmb.bestbefore.ui.screens.hallway

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.dmb.bestbefore.data.models.HallwayCard

@Composable
fun SoundCloudPlayerSheet(
    card: HallwayCard,
    themeColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val trackTitle = card.backgroundMusic?.takeIf { it.isNotBlank() } ?: "Unknown Track"
    val artistHandle = card.ownerEmail?.substringBefore("@")?.takeIf { it.isNotBlank() } ?: "artist"
    
    val isDirectUrl = trackTitle.startsWith("https://soundcloud.com/")
    val displayTitle = if (isDirectUrl) trackTitle.substringAfterLast("/") else trackTitle
    
    val soundCloudQuery = Uri.encode("$displayTitle $artistHandle")
    val soundCloudUrl = if (isDirectUrl) trackTitle else "https://soundcloud.com/search?q=$soundCloudQuery"

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable { onDismiss() }
        )

        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.95f), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp, bottom = 24.dp)
                            .size(width = 44.dp, height = 4.dp)
                            .background(Color.White.copy(alpha = 0.3f), CircleShape)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Artwork with room-theme glow
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(210.dp)
                                    .blur(70.dp)
                                    .alpha(0.7f)
                                    .background(themeColor, CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(260.dp)
                                    .background(themeColor.copy(alpha = 0.15f), RoundedCornerShape(22.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.List,
                                    contentDescription = null,
                                    tint = themeColor,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                        }

                        // Metadata
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = displayTitle,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            if (!isDirectUrl) {
                                Text(
                                    text = "@$artistHandle",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.55f)
                                )
                            }
                        }

                        // Listen on SoundCloud
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp)
                                .background(Color(0xFFFF5500), RoundedCornerShape(16.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    val intent = Intent(Intent.ACTION_VIEW, soundCloudUrl.toUri())
                                    runCatching { context.startActivity(intent) }
                                }
                                .padding(vertical = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Listen on SoundCloud",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
