package com.ozang.bestbefore_mvp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Floating orb menu component that appears on the right edge of the screen.
 * Contains action buttons for Search, Chat, Add, and Profile.
 */
@Composable
fun OrbMenu(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    onSearchClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .size(size)
            .offset(x = size / 2) // Half off-screen to the right
            .clip(RoundedCornerShape(topStart = size / 2, bottomStart = size / 2))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF0D59F2),
                        Color(0xFF00D972)
                    )
                )
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .padding(start = 24.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.Start
        ) {
            OrbButton(
                icon = Icons.Default.Search,
                contentDescription = "Search",
                onClick = onSearchClick
            )
            OrbButton(
                icon = Icons.Default.Person, // Using Person as placeholder for Chat
                contentDescription = "Chat",
                onClick = onChatClick
            )
            OrbButton(
                icon = Icons.Default.Add,
                contentDescription = "Add",
                onClick = onAddClick
            )
            OrbButton(
                icon = Icons.Default.Person,
                contentDescription = "Profile",
                onClick = onProfileClick
            )
        }
    }
}

/**
 * Simple orb menu with just a center button (used for navigation/next actions)
 */
@Composable
fun OrbMenuSimple(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    label: String = "",
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .size(size)
            .offset(x = size / 2)
            .clip(RoundedCornerShape(topStart = size / 2, bottomStart = size / 2))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF0D59F2),
                        Color(0xFF00D972)
                    )
                )
            )
            .clickable { onClick() },
        contentAlignment = Alignment.CenterStart
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                color = Color.White,
                modifier = Modifier.padding(start = 24.dp)
            )
        }
    }
}

@Composable
private fun OrbButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
