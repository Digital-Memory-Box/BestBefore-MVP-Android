package com.dmb.bestbefore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dmb.bestbefore.ui.theme.ThemeState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration

import androidx.compose.foundation.shape.CircleShape

/**
 * Shared geometry contract for OrbMenu placement ensuring a perfect half circle.
 */
object OrbMenuLayout {
    private const val REVEAL_FRACTION = 0.5f     // Exactly half
    private const val DIAMETER_RATIO = 0.55f     // 0.55 ratio
    private const val MIN_DIAMETER = 600f
    private const val MAX_DIAMETER = 1200f

    fun horizontalOffset(diameter: Dp): Dp = diameter * REVEAL_FRACTION
    fun visibleInset(diameter: Dp): Dp = diameter - horizontalOffset(diameter)

    fun computeDiameter(screenHeightDp: Float): Dp {
        val raw = screenHeightDp * DIAMETER_RATIO
        return raw.coerceIn(MIN_DIAMETER, MAX_DIAMETER).dp
    }
}

/**
 * Floating orb menu component that appears on the right edge of the screen.
 * Gradient colors follow the current app theme (primaryColor → secondaryColor).
 */
@Composable
fun OrbMenu(
    modifier: Modifier = Modifier,
    diameter: Dp = OrbMenuLayout.computeDiameter(
        LocalConfiguration.current.screenHeightDp.toFloat()
    ),
    primaryColor: Color = Color(0xFF0D59F2),
    secondaryColor: Color = Color(0xFF00D972),
    onAddClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onCameraClick: () -> Unit = {}
) {
    val appTheme = ThemeState.currentTheme
    val accent = ThemeState.currentAccent
    val applyAccent = ThemeState.applyAccentToAll
    val isGlass = appTheme.name == "Glass"
    val isMidnight = appTheme.name == "Midnight"

    val iconTint = when {
        isMidnight -> accent
        isGlass -> if (applyAccent) Color.White else accent
        else -> Color.White
    }

    val baseBackground = when {
        isGlass -> Color.White.copy(alpha = 0.1f)
        isMidnight -> Color.Black
        else -> accent
    }

    Box(
        modifier = modifier
            .size(diameter)
            .offset(x = OrbMenuLayout.horizontalOffset(diameter))
            .clip(CircleShape)
            .background(Color(0xFF007AFF)) // Solid blue like the screenshot
    ) {

        // Add button — top arc
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = -(diameter * 0.38f), y = -(diameter * 0.25f))
        ) {
            OrbButton(
                icon = Icons.Default.Add,
                contentDescription = "Add",
                onClick = onAddClick,
                size = 48.dp,
                iconSize = 28.dp,
                tint = Color.White
            )
        }

        // Profile button — middle
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = -(diameter * 0.38f), y = 0.dp)
        ) {
            OrbButton(
                icon = Icons.Default.Person,
                contentDescription = "Profile",
                onClick = onProfileClick,
                size = 64.dp,
                iconSize = 36.dp,
                tint = Color.White
            )
        }

        // Camera button — bottom
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = -(diameter * 0.38f), y = (diameter * 0.25f))
        ) {
            OrbButton(
                icon = androidx.compose.material.icons.Icons.Default.CameraAlt,
                contentDescription = "Camera",
                onClick = onCameraClick,
                size = 48.dp,
                iconSize = 28.dp,
                tint = Color.White
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
    iconSize: Dp = 24.dp,
    tint: Color = Color.White
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
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
