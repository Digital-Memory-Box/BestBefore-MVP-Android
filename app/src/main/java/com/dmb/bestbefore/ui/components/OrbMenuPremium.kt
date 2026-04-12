package com.dmb.bestbefore.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import উদ্যোগে androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun OrbMenuPremium(
    isHidden: Boolean,
    onIsHiddenChange: (Boolean) -> Unit,
    accentColor: Color = Color.Blue,
    selectedTheme: String = "Default",
    onAdd: () -> Unit = {},
    onChat: () -> Unit = {},
    onScan: () -> Unit = {},
    onProfile: () -> Unit = {},
    onSearch: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(true) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    val iconColor = if (selectedTheme == "Midnight" || selectedTheme == "Glass") accentColor else Color.White

    Box(
        modifier = Modifier
            .width(170.dp)
            .height(400.dp)
            .background(Color.Transparent),
        contentAlignment = Alignment.CenterEnd
    ) {
        if (!isHidden) {
            Box(
                modifier = Modifier
                    .offset(x = if (dragOffsetX > 0) dragOffsetX.dp else 0.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { _, dragAmount ->
                                if (dragAmount.x > 0) {
                                    dragOffsetX += dragAmount.x
                                }
                            },
                            onDragEnd = {
                                if (dragOffsetX > 40f) {
                                    onIsHiddenChange(true)
                                    dragOffsetX = 0f
                                } else {
                                    dragOffsetX = 0f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.CenterEnd
            ) {
                if (isExpanded) {
                    Box(
                        modifier = Modifier
                            .size(340.dp)
                            .offset(x = 170.dp)
                            .shadow(
                                elevation = if (selectedTheme == "Glass") 12.dp else 15.dp,
                                shape = CircleShape,
                                ambientColor = accentColor,
                                spotColor = accentColor
                            )
                            .background(
                                color = if (selectedTheme == "Midnight") Color.Black else (if (selectedTheme == "Glass") accentColor.copy(0.12f) else accentColor),
                                shape = CircleShape
                            )
                            .then(
                                if (selectedTheme == "Midnight") Modifier.border(2.dp, accentColor, CircleShape)
                                else if (selectedTheme == "Glass") Modifier.border(1.5f, Color.White.copy(0.7f), CircleShape)
                                else Modifier
                            )
                    )

                    // Icons
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Plus Icon
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = iconColor,
                            modifier = Modifier
                                .offset(x = (-135).dp)
                                .size(22.dp)
                                .clickable { onAdd() }
                        )

                        // Vertical Arc
                        Column(
                            verticalArrangement = Arrangement.spacedBy(40.dp),
                            modifier = Modifier.offset(x = (-95).dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Chat",
                                tint = iconColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { onChat() }
                            )

                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = iconColor,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { onProfile() }
                            )

                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = iconColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { onSearch() }
                            )
                        }
                    }
                } else {
                    // Collapsed State
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .offset(x = 30.dp)
                            .background(
                                color = if (selectedTheme == "Midnight") Color.Black else (if (selectedTheme == "Glass") accentColor.copy(0.12f) else accentColor),
                                shape = CircleShape
                            )
                            .then(
                                if (selectedTheme == "Midnight") Modifier.border(2.dp, accentColor, CircleShape)
                                else if (selectedTheme == "Glass") Modifier.border(1.5f, Color.White.copy(0.7f), CircleShape)
                                else Modifier
                            )
                            .clickable { isExpanded = true }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun OrbMenuPremiumPreview() {
    OrbMenuPremium(isHidden = false, onIsHiddenChange = {}, accentColor = Color.Magenta)
}
