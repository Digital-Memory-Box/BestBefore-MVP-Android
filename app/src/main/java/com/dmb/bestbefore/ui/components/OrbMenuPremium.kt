package com.dmb.bestbefore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity

/**
 * BB-UI-05 ve BB-UI-06 kurallarına uygun, sağa kaydırarak gizlenebilen (Drag-to-hide)
 * ve tema destekli kusursuz Cam Küre (Orb) menü.
 */
@Composable
fun OrbMenuPremium(
    isHidden: Boolean,
    onIsHiddenChange: (Boolean) -> Unit,
    accentColor: Color = Color(0xFF0D59F2),
    selectedTheme: String = "Default",
    applyAccentToAll: Boolean = false, // BB-UI-21 Kuralı için eklendi
    onEventClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    profileImageUrl: String? = null
) {
    // Kaydirma miktarini tutan state
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    // Shared geometry with OrbMenu so inset and visible orb area stay consistent.
    val orbDiameter = 240.dp
    val hitBoxWidth = OrbMenuLayout.visibleInset(orbDiameter)
    val orbOffsetX = OrbMenuLayout.horizontalOffset(orbDiameter)
    val density = LocalDensity.current
    val dragHideThresholdPx = with(density) { (hitBoxWidth * 0.42f).toPx() }

    val isGlass = selectedTheme == "Glass"
    val isMidnight = selectedTheme == "Midnight"

    // BB-UI-21 Kurali: Midnight temasinda ikonlar her zaman accent olur.
    // Diger temalarda Apply Accent seciliyse ikonlar beyaz olur.
    val iconTint = when {
        isMidnight -> accentColor
        applyAccentToAll -> Color.White
        isGlass -> accentColor
        else -> Color.White
    }

    if (!isHidden) {
        // En dış taşıyıcı kutu: Sürükleme hareketlerini dinler
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(hitBoxWidth)
                .offset(x = if (dragOffsetX > 0) dragOffsetX.dp else 0.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { _, dragAmount ->
                            // Sadece sağa kaydırmaya izin ver
                            if (dragAmount.x > 0) {
                                dragOffsetX += dragAmount.x
                            }
                        },
                        onDragEnd = {
                            // Orbit geometry-scaled threshold for hide interaction
                            if (dragOffsetX > dragHideThresholdPx) {
                                onIsHiddenChange(true)
                                dragOffsetX = 0f
                            } else {
                                // Yeterince çekilmediyse eski yerine geri yaylan (snap back)
                                dragOffsetX = 0f
                            }
                        }
                    )
                },
            contentAlignment = Alignment.CenterEnd // İçeriği ekranın sağ kenarına yaslar
        ) {

            // THE ORB (Az önce yaptığımız mükemmel görsel tasarım)
            Box(
                modifier = Modifier
                    .offset(x = orbOffsetX)
                    .size(orbDiameter)
                    // 1. KATMAN: Zemin Rengi
                    .background(
                        color = if (isMidnight) Color(0xFF121212).copy(alpha = 0.95f)
                        else if (isGlass) Color(0xFF2C2C2E).copy(alpha = 0.85f)
                        else accentColor,
                        shape = CircleShape
                    )
                    // 2. KATMAN: İç Parlama (Inner Glow)
                    .background(
                        brush = if (isGlass || isMidnight) {
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    accentColor.copy(alpha = if (isMidnight) 0.15f else 0.25f)
                                )
                            )
                        } else {
                            Brush.radialGradient(listOf(Color.Transparent, Color.Transparent))
                        },
                        shape = CircleShape
                    )
                    // 3. KATMAN: Keskin Çerçeve Sınırı
                    .then(
                        if (isMidnight || isGlass) Modifier.border(2.dp, accentColor.copy(alpha = 0.85f), CircleShape)
                        else Modifier
                    )
                    .clip(CircleShape)
            ) {
                // --- KUSURSUZ İKON HİZALAMALARI ---
                Box(modifier = Modifier.align(Alignment.Center).offset(x = (-63).dp, y = (-63).dp)) {
                    OrbButton(icon = Icons.Default.Event, contentDescription = "Events", onClick = onEventClick, tint = iconTint)
                }

                Box(modifier = Modifier.align(Alignment.Center).offset(x = (-86).dp, y = (-23).dp)) {
                    OrbButton(icon = Icons.Default.Search, contentDescription = "Search", onClick = onSearchClick, tint = iconTint)
                }

                Box(modifier = Modifier.align(Alignment.Center).offset(x = (-86).dp, y = 23.dp)) {
                    OrbButton(icon = Icons.Default.Add, contentDescription = "Add", onClick = onAddClick, tint = iconTint)
                }

                Box(modifier = Modifier.align(Alignment.Center).offset(x = (-63).dp, y = 63.dp)) {
                    ProfileAvatar(
                        imageUri = profileImageUrl,
                        size = 44.dp,
                        accentColor = if (isMidnight || isGlass) accentColor else Color.White,
                        onClick = onProfileClick
                    )
                }
            }
        }
    }
}

@Composable
private fun OrbButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 44.dp,
    iconSize: Dp = 26.dp,
    tint: Color = Color.White
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
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