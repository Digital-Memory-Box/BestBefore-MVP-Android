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
    onProfileClick: () -> Unit = {}
) {
    // Kaydırma miktarını tutan state
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

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
                .width(120.dp) // 240.dp olan dairenin sadece görünen yarısı kadar bir hit-box açıyoruz
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
                            // Eğer 50 pikselden fazla sağa çekildiyse gizle
                            if (dragOffsetX > 50f) {
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
                    .offset(x = 120.dp) // Dairenin yarısını ekran dışına atar
                    .size(240.dp)
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
                    OrbButton(icon = Icons.Default.Person, contentDescription = "Profile", onClick = onProfileClick, tint = iconTint)
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