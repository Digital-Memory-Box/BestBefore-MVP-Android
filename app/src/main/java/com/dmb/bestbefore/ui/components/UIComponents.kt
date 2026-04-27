package com.dmb.bestbefore.ui.components

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.dmb.bestbefore.utils.Base64BitmapCache
import com.dmb.bestbefore.ui.theme.LocalBestBeforeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class UserPrivacyStatus { NONE, PUBLIC_ACCOUNT, PRIVATE_ACCOUNT }

@Composable
fun SharedUserCard(
    name: String,
    biography: String,
    roomingCount: String,
    roomersCount: String,
    accentColor: Color,
    privacyStatus: UserPrivacyStatus = UserPrivacyStatus.NONE,
    profileImageUri: Any? = null,
    tags: List<String> = emptyList()
) {
    var isBioExpanded by remember { mutableStateOf(false) }
    val themeColors = LocalBestBeforeColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayName = if (name.isEmpty()) "Username" else if (name.startsWith("@")) name else "@$name"
                    Text(
                        text = displayName,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.textPrimary
                    )

                    if (privacyStatus != UserPrivacyStatus.NONE) {
                        val isPrivate = privacyStatus == UserPrivacyStatus.PRIVATE_ACCOUNT
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    if (isPrivate) Color(0xFFFFA500).copy(0.15f) else Color(0xFF4CAF50).copy(0.15f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = if (isPrivate) Icons.Default.Lock else Icons.Default.Place,
                                contentDescription = null,
                                tint = if (isPrivate) Color(0xFFFFA500) else Color(0xFF4CAF50),
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = if (isPrivate) "Private" else "Public",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isPrivate) Color(0xFFFFA500) else Color(0xFF4CAF50)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column {
                        Text("Rooming", fontSize = 12.sp, color = themeColors.textSecondary)
                        Text(roomingCount, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
                    }
                    Column {
                        Text("Roomers", fontSize = 12.sp, color = themeColors.textSecondary)
                        Text(roomersCount, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
                    }
                }
            }

            ProfileAvatar(
                imageUri = profileImageUri,
                size = 84.dp,
                accentColor = accentColor
            )
        }

        if (biography.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.animateContentSize()) {
                Text(
                    text = biography,
                    fontSize = 14.sp,
                    color = themeColors.textPrimary.copy(alpha = 0.82f),
                    maxLines = if (isBioExpanded) Int.MAX_VALUE else 3
                )

                if (biography.length > 50) {
                    Text(
                        text = if (isBioExpanded) "See Less" else "See All",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.clickable { isBioExpanded = !isBioExpanded }
                    )
                }
            }
        }

        // Profile Tags section
        if (tags.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(tags) { tag ->
                    Row(
                        modifier = Modifier
                            .background(
                                accentColor.copy(alpha = 0.15f),
                                RoundedCornerShape(50.dp)
                            )
                            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(50.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Tag,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = tag,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UIStatsCard(
    title: String,
    value: String,
    color: Color,
    icon: ImageVector = Icons.Default.Home,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalBestBeforeColors.current
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
            Text(title, fontSize = 12.sp, color = themeColors.textSecondary)
        }
    }
}

@Composable
fun TabButton(title: String, isSelected: Boolean, color: Color, onClick: () -> Unit) {
    val themeColors = LocalBestBeforeColors.current
    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) themeColors.textPrimary else themeColors.textSecondary
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (isSelected) color else Color.Transparent)
        )
    }
}

@Composable
fun MusicPresetOption(title: String, isSelected: Boolean, tintColor: Color, onClick: () -> Unit) {
    val themeColors = LocalBestBeforeColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                if (isSelected) tintColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Done, // Placeholder icon
            contentDescription = null,
            tint = if (isSelected) themeColors.textPrimary else themeColors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) themeColors.textPrimary else themeColors.textSecondary
        )
        Spacer(modifier = Modifier.weight(1f))
        if (isSelected) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = tintColor)
        }
    }
}
@Composable
fun ProfileAvatar(
    imageUri: Any?, // Can be Uri, String (URL or Base64), or null
    size: androidx.compose.ui.unit.Dp,
    accentColor: Color = Color(0xFF007AFF),
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    
    Box(
        modifier = modifier
            .size(size)
            .background(accentColor, CircleShape)
            .border(1.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            .clip(CircleShape)
            .then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        val modelStr = imageUri?.toString()
        if (!modelStr.isNullOrBlank()) {
            if (modelStr.startsWith("data:") && modelStr.contains("base64,")) {
                // Decode base64 on IO thread and cache result — avoids janking the main thread
                // with large profile images stored as data URIs (same pattern as AsyncBase64Image).
                var bitmap by remember(modelStr) {
                    mutableStateOf(Base64BitmapCache.get(modelStr))
                }
                LaunchedEffect(modelStr) {
                    if (bitmap == null) {
                        val decoded = withContext(Dispatchers.Default) {
                            try {
                                val clean = modelStr.substringAfter("base64,")
                                val bytes = android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
                                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (_: Exception) { null }
                        }
                        if (decoded != null) {
                            Base64BitmapCache.put(modelStr, decoded)
                            bitmap = decoded
                        }
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                // While decoding: accent-colored circle (already the Box background)
            } else {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Person)
                )
            }
        } else {
            val isLight = accentColor.luminance() > 0.5f
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = if (isLight) Color.Black.copy(alpha = 0.6f) else Color.White,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}
