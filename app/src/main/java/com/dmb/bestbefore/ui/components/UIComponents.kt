package com.dmb.bestbefore.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class UserPrivacyStatus { NONE, PUBLIC_ACCOUNT, PRIVATE_ACCOUNT }

@Composable
fun SharedUserCard(
    name: String,
    biography: String,
    roomingCount: String,
    roomersCount: String,
    accentColor: Color,
    privacyStatus: UserPrivacyStatus = UserPrivacyStatus.NONE
) {
    var isBioExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayName = if (name.isEmpty()) "Username" else if (name.startsWith("@")) name else "@$name"
                    Text(
                        text = displayName,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
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
                        Text("Rooming", fontSize = 12.sp, color = Color.Gray)
                        Text(roomingCount, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column {
                        Text("Roomers", fontSize = 12.sp, color = Color.Gray)
                        Text(roomersCount, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(accentColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = accentColor, modifier = Modifier.size(32.dp))
            }
        }

        if (biography.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.animateContentSize()) {
                Text(
                    text = biography,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
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
    }
}

@Composable
fun UIStatsCard(title: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.Home, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(title, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun TabButton(title: String, isSelected: Boolean, color: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color.Gray
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
            tint = if (isSelected) Color.White else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else Color.Gray
        )
        Spacer(modifier = Modifier.weight(1f))
        if (isSelected) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = tintColor)
        }
    }
}
