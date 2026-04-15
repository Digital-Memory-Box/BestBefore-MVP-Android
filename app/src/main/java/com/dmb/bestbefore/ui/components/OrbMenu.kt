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

/**
 * True oval shape (ellipse) to avoid the flat vertical side produced by rounded rectangles.
 */
private object OrbOvalShape : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): Outline = Outline.Generic(
        Path().apply {
            addOval(Rect(0f, 0f, size.width, size.height))
        }
    )
}

/**
 * Shared geometry contract for OrbMenu placement.
 * `visibleInset(width)` returns the horizontal space the orb occupies inside the screen.
 */
object OrbMenuLayout {
    private const val REVEAL_FRACTION = 0.55f

    fun horizontalOffset(width: Dp): Dp = width * REVEAL_FRACTION

    fun visibleInset(width: Dp): Dp = width - horizontalOffset(width)
}

/**
 * Floating orb menu component that appears on the right edge of the screen.
 * Gradient colors follow the current app theme (primaryColor → secondaryColor).
 */
@Composable
fun OrbMenu(
    modifier: Modifier = Modifier,
    width: Dp = 160.dp,
    height: Dp = 220.dp,
    primaryColor: Color = Color(0xFF0D59F2),
    secondaryColor: Color = Color(0xFF00D972),
    onSearchClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
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
            .width(width)
            .height(height)
            .offset(x = OrbMenuLayout.horizontalOffset(width))
            .clip(OrbOvalShape)
            .background(
                brush = if (isGlass) {
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f),
                            accent.copy(alpha = 0.08f),
                            primaryColor.copy(alpha = 0.05f),
                            secondaryColor.copy(alpha = 0.03f)
                        )
                    )
                } else {
                    Brush.radialGradient(listOf(baseBackground, baseBackground))
                }
            )
            .then(
                if (isMidnight) Modifier.border(1.dp, accent, OrbOvalShape)
                else Modifier
            )
    ) {
        // Inner lighter half-circle for depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(OrbOvalShape)
                .background(
                    if (isMidnight) Color.Black else Color.White.copy(alpha = 0.10f)
                )
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
                iconSize = 32.dp,
                tint = iconTint
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
                iconSize = 20.dp,
                tint = iconTint
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
                iconSize = 20.dp,
                tint = iconTint
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
                iconSize = 20.dp,
                tint = iconTint
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
