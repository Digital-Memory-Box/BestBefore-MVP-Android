package com.dmb.bestbefore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
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
 * Gradient colors follow the current app theme (primaryColor → secondaryColor).
 */
@Composable
fun OrbMenu(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    primaryColor: Color = Color(0xFF0D59F2),
    secondaryColor: Color = Color(0xFF00D972),
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
                    colors = listOf(primaryColor, secondaryColor)
                )
            )
    ) {
        // Inner lighter half-circle for depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(topStart = (size / 2) - 12.dp, bottomStart = (size / 2) - 12.dp))
                .background(Color.White.copy(alpha = 0.10f))
        )

        // Large centered profile button
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-25).dp)
        ) {
            OrbButton(
                icon = Icons.Default.Person,
                contentDescription = "Profile",
                onClick = onProfileClick,
                size = 56.dp,
                iconSize = 32.dp
            )
        }

        // Message button — top of the arc
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = (-25).dp, y = 25.dp)
        ) {
            OrbButton(
                icon = Icons.Default.Email,
                contentDescription = "Messages",
                onClick = onChatClick,
                size = 40.dp,
                iconSize = 20.dp
            )
        }

        // Add button — middle of the arc
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 5.dp)
        ) {
            OrbButton(
                icon = Icons.Default.Add,
                contentDescription = "Add",
                onClick = onAddClick,
                size = 40.dp,
                iconSize = 20.dp
            )
        }

        // Search button — bottom of the arc
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = (-25).dp, y = (-25).dp)
        ) {
            OrbButton(
                icon = Icons.Default.Search,
                contentDescription = "Search",
                onClick = onSearchClick,
                size = 40.dp,
                iconSize = 20.dp
            )
        }
    }
}

@Composable
private fun OrbButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 40.dp,
    iconSize: Dp = 24.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}
