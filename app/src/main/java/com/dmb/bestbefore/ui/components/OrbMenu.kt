package com.dmb.bestbefore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Camera
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
    diameter: Dp = 420.dp, // Biraz daha büyütüldü
    onAddClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onCameraClick: () -> Unit = {}
) {
    // İkonların merkeze uzaklığı - Swift versiyonundaki sıkı görünüm için biraz artırıldı
    val buttonRadius = diameter * 0.42f

    Box(
        modifier = modifier
            .size(diameter)
            // Orbu sağa daha fazla iterek (0.5f -> 0.72f) boşluğu azalttım
            .offset(x = diameter * 0.82f)
            .clip(CircleShape)
            .background(Color(0xFF007AFF))
    ) {
        // İkonlar merkeze göre sol tarafa offsetlenir.

        // 1. Add Button (Üst Kavis)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(
                    x = -(buttonRadius * 0.88f), // Daha sola çekildi (0.75f -> 0.88f)
                    y = -(buttonRadius * 0.52f)  // Kavis uyumu için hafif yukarı (0.50f -> 0.52f)
                )
        ) {
            OrbButton(
                icon = Icons.Default.Add,
                contentDescription = "Add",
                onClick = onAddClick,
                size = 46.dp,
                iconSize = 30.dp
            )
        }

        // 2. Profile Button (Tam Orta)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = -buttonRadius, y = 0.dp)
        ) {
            OrbButton(
                icon = Icons.Default.Person,
                contentDescription = "Profile",
                onClick = onProfileClick,
                size = 60.dp,
                iconSize = 38.dp
            )
        }

        // 3. Camera Button (Alt Kavis)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(
                    x = -(buttonRadius * 0.88f), // Daha sola çekildi (0.75f -> 0.88f)
                    y = (buttonRadius * 0.52f)   // Kavis uyumu için hafif aşağı (0.50f -> 0.52f)
                )
        ) {
            OrbButton(
                icon = Icons.Default.Camera,
                contentDescription = "Camera",
                onClick = onCameraClick,
                size = 46.dp,
                iconSize = 30.dp
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
