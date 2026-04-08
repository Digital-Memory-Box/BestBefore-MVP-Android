package com.dmb.bestbefore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dmb.bestbefore.data.api.models.SoundCloudTrack
import com.dmb.bestbefore.ui.theme.LocalBestBeforeColors
import com.dmb.bestbefore.notifications.MusicPlayerManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicSelectorBottomSheet(
    viewModel: MusicViewModel,
    token: String,
    onDismissRequest: () -> Unit
) {
    val tracks by viewModel.tracks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val currentTrack by MusicPlayerManager.currentTrack.collectAsState()
    val isPlaying by MusicPlayerManager.isPlaying.collectAsState()

    val context = LocalContext.current
    val colors = LocalBestBeforeColors.current

    LaunchedEffect(Unit) {
        viewModel.loadPlaylist(token)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = colors.background,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.textSecondary) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = "Music", tint = colors.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "BestBefore Playlist",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text(text = error!!, color = Color.Red, fontSize = 14.sp)
                }
            } else if (tracks.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text(text = "No tracks found.", color = colors.textSecondary, fontSize = 14.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxHeight(0.6f)) {
                    items(tracks) { track ->
                        val isThisTrackPlaying = currentTrack?.id == track.id
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isThisTrackPlaying) {
                                        MusicPlayerManager.togglePlayPause(context)
                                    } else {
                                        viewModel.playTrack(context, track)
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            track.artworkUrl?.let { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Artwork",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.DarkGray)
                                )
                            } ?: Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    color = if (isThisTrackPlaying) colors.primary else colors.textPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = if (isThisTrackPlaying) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    color = colors.textSecondary,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            if (isThisTrackPlaying) {
                                // Show animated equalizer or play state indicator
                                Text(
                                    text = if (isPlaying) "Playing" else "Paused",
                                    color = colors.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
