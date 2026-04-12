package com.dmb.bestbefore.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.abs

@Composable
fun CardStackView(
    rooms: List<RoomObject>,
    initialSelectedIndex: Int,
    isMenuHidden: Boolean,
    isGlowDisabled: Boolean = false,
    isDimmed: Boolean = false,
    onProximityChange: ((Int, Double) -> Unit)? = null,
    onTap: (RoomObject) -> Unit = {}
) {
    var selectedIndex by remember { mutableIntStateOf(initialSelectedIndex) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    
    val cardWidth = 110f
    val spacing = 0f

    val roomImageIndices = remember { mutableStateMapOf<String, Int>() }

    val offsetX by animateFloatAsState(
        targetValue = if (isMenuHidden) 0f else -45f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow), label = "menuHideOffset"
    )

    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = offsetX.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDrag = { change, amount ->
                        totalDragX += amount.x
                        totalDragY += amount.y
                        
                        if (abs(totalDragX) > abs(totalDragY)) {
                            dragOffset = totalDragX
                        }
                        
                        rooms.forEachIndexed { i, _ ->
                            val offset = (i - selectedIndex).toFloat()
                            val absOffset = abs(offset + dragOffset / (cardWidth + spacing))
                            onProximityChange?.invoke(i, absOffset.toDouble())
                        }
                    },
                    onDragEnd = {
                        val hDist = totalDragX
                        val vDist = totalDragY
                        val threshold = 50f
                        
                        if (abs(hDist) > abs(vDist)) {
                            // Horizontal Drag (Scroll)
                            if (hDist > threshold && selectedIndex > 0) {
                                selectedIndex -= 1
                            } else if (hDist < -threshold && selectedIndex < rooms.size - 1) {
                                selectedIndex += 1
                            }
                            dragOffset = 0f
                        } else {
                            // Vertical Drag (Image Cycle)
                            val activeRoom = rooms[selectedIndex]
                            // Using tags as mock for preview query count.
                            val previewCount = if(activeRoom.tags.isNotEmpty()) activeRoom.tags.size else 1
                            val currentIndex = roomImageIndices[activeRoom.id] ?: 0
                            
                            if (vDist < -30) {
                                roomImageIndices[activeRoom.id] = (currentIndex + 1) % previewCount
                            } else if (vDist > 30) {
                                roomImageIndices[activeRoom.id] = (currentIndex - 1 + previewCount) % previewCount
                            }
                            dragOffset = 0f
                        }
                    }
                )
            }
    ) {
        // Arrange items by Z-order to handle overlaps naturally
        val sortedIndices = rooms.indices.sortedBy { i ->
            val offset = i - selectedIndex
            val absOffset = abs(offset + dragOffset / (cardWidth + spacing))
            -absOffset
        }
        
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            sortedIndices.forEach { index ->
                val room = rooms[index]
                val offset = index - selectedIndex
                val translationX = offset * (cardWidth + spacing) + dragOffset
                val absOffset = abs(offset + dragOffset / (cardWidth + spacing))
                
                val scale = (1.0f - minOf(absOffset * 0.15f, 0.4f)).coerceIn(0f, 1f)
                val alpha = (1.0f - minOf(absOffset * 0.5f, 0.8f)).coerceIn(0f, 1f)
                val glowOpacity = maxOf(0f, 1.0f - abs(offset + dragOffset / (cardWidth + spacing)))
                
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            this.translationX = translationX
                            this.scaleX = scale
                            this.scaleY = scale
                            this.alpha = alpha
                        }
                        .zIndex((rooms.size - absOffset).toFloat())
                ) {
                    StackCardView(
                        room = room,
                        glowOpacity = if (isGlowDisabled) 0f else glowOpacity,
                        isActive = index == selectedIndex && !isDimmed,
                        imageIndex = roomImageIndices[room.id] ?: 0,
                        previewCount = if(room.tags.isNotEmpty()) room.tags.size else 1,
                        onTap = {
                            if (index == selectedIndex) onTap(room)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StackCardView(
    room: RoomObject,
    glowOpacity: Float,
    isActive: Boolean,
    imageIndex: Int,
    previewCount: Int,
    onTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(180.dp)
            .height(300.dp)
            .clickable { onTap() },
        contentAlignment = Alignment.BottomStart
    ) {
        // Broad Glow
        Box(modifier = Modifier
            .matchParentSize()
            .scale(1.2f)
            .blur(35.dp)
            .background(room.themeColor.copy(alpha = glowOpacity * 0.35f), RoundedCornerShape(32.dp)))
        
        // Vibrant Inner Glow
        Box(modifier = Modifier
            .matchParentSize()
            .scale(1.03f)
            .blur(15.dp)
            .background(room.themeColor.copy(alpha = glowOpacity * 0.5f), RoundedCornerShape(32.dp)))
            
        // UnsplashImageView proxy (placeholder gradient matching requested visuals)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF19192E), Color(0xFF14213D))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("Preview ${imageIndex + 1}", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }
        
        // View indicator
        if (isActive && previewCount > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 15.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha=0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("${imageIndex + 1}/$previewCount", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CardStackViewPreview() {
    Box(modifier = Modifier.background(Color.Black).fillMaxSize()) {
        CardStackView(
            rooms = listOf(
                RoomObject(name = "Test", themeColor = Color.Magenta, tags = listOf("A", "B", "C")),
                RoomObject(name = "Test2", themeColor = Color.Cyan)
            ),
            initialSelectedIndex = 0,
            isMenuHidden = false
        )
    }
}
