package com.ozang.bestbefore_mvp.ui.screens.hallway

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ozang.bestbefore_mvp.data.models.HallwayCard
import com.ozang.bestbefore_mvp.ui.components.OrbMenu

@Composable
fun HallwayScreen(
    onNavigateToProfile: () -> Unit,
    viewModel: HallwayViewModel = viewModel()
) {
    val cards by viewModel.cards.collectAsState()
    val selectedIndex by viewModel.selectedCardIndex.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 60.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hallway",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "All",
                fontSize = 28.sp,
                color = Color.White.copy(alpha = 0.4f)
            )
        }

        // Main content row
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, bottom = 74.dp)
        ) {
            // Card stack (left side ~64% of screen)
            Box(
                modifier = Modifier
                    .weight(0.64f)
                    .fillMaxHeight()
                    .padding(start = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CardStack(
                    cards = cards,
                    selectedIndex = selectedIndex,
                    onCardSelected = { viewModel.selectCard(it) }
                )
            }

            // Side info panel (right side ~36% of screen)
            Column(
                modifier = Modifier
                    .weight(0.36f)
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp)
            ) {
                // Selected card info
                if (cards.isNotEmpty() && selectedIndex in cards.indices) {
                    val selectedCard = cards[selectedIndex]
                    
                    Spacer(modifier = Modifier.height(80.dp))
                    
                    Text(
                        text = selectedCard.title,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Time Capsule: ${selectedCard.timeCapsuleDays} Days",
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Description",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedCard.description,
                        fontSize = 11.sp,
                        color = Color(0xFFAAAAAA),
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "> Full Description",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.clickable { /* Handle full description */ }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Explore More section
                Text(
                    text = "Explore More",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                ExploreItem(text = "> New York City")
                Spacer(modifier = Modifier.height(8.dp))
                ExploreItem(text = "> Daily Trip")
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Bottom navigation
        BottomNavigation(
            currentTab = currentTab,
            onTabSelected = { viewModel.selectTab(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Orb menu (right edge, centered vertically)
        OrbMenu(
            modifier = Modifier.align(Alignment.CenterEnd),
            onProfileClick = onNavigateToProfile,
            onAddClick = { /* Handle add */ },
            onSearchClick = { /* Handle search */ },
            onChatClick = { /* Handle chat */ }
        )
    }
}

@Composable
private fun CardStack(
    cards: List<HallwayCard>,
    selectedIndex: Int,
    onCardSelected: (Int) -> Unit
) {
    var dragOffset by remember { mutableStateOf(0f) }
    
    Box(
        modifier = Modifier
            .width(225.dp)
            .height(400.dp)
            .pointerInput(selectedIndex, cards.size) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 50 && selectedIndex > 0) {
                            onCardSelected(selectedIndex - 1)
                        } else if (dragOffset < -50 && selectedIndex < cards.size - 1) {
                            onCardSelected(selectedIndex + 1)
                        }
                        dragOffset = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        dragOffset += dragAmount
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        cards.forEachIndexed { index, card ->
            val offset = index - selectedIndex
            val absOffset = kotlin.math.abs(offset)
            
            val scale = 1f - (absOffset * 0.1f).coerceAtMost(0.3f)
            val alpha = 1f - (absOffset * 0.25f).coerceAtMost(0.7f)
            val translationY = offset * 70f
            val zIndex = (cards.size - absOffset).toFloat()
            
            StackCardItem(
                card = card,
                modifier = Modifier
                    .zIndex(zIndex)
                    .scale(scale)
                    .alpha(alpha)
                    .offset(y = translationY.dp)
                    .clickable { onCardSelected(index) }
            )
        }
    }
}

@Composable
private fun StackCardItem(
    card: HallwayCard,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(200.dp)
            .height(280.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E)
                    )
                )
            ),
        contentAlignment = Alignment.BottomStart
    ) {
        // Card gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        ),
                        startY = 150f
                    )
                )
        )
        
        Text(
            text = card.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun ExploreItem(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A1A))
            .padding(10.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun BottomNavigation(
    currentTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(74.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            text = "Rooming",
            isSelected = currentTab == BottomTab.ROOMING,
            onClick = { onTabSelected(BottomTab.ROOMING) }
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onTabSelected(BottomTab.EVERYONE) }
        ) {
            if (currentTab == BottomTab.EVERYONE) {
                Text(
                    text = "▽",
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
            Text(
                text = "Everyone",
                fontSize = if (currentTab == BottomTab.EVERYONE) 20.sp else 18.sp,
                fontWeight = if (currentTab == BottomTab.EVERYONE) FontWeight.Bold else FontWeight.Normal,
                color = if (currentTab == BottomTab.EVERYONE) Color.White else Color.White.copy(alpha = 0.4f)
            )
        }
        
        BottomNavItem(
            text = "Artists",
            isSelected = currentTab == BottomTab.ARTISTS,
            onClick = { onTabSelected(BottomTab.ARTISTS) }
        )
    }
}

@Composable
private fun BottomNavItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        fontSize = 18.sp,
        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.clickable { onClick() }
    )
}
