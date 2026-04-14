@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.dmb.bestbefore.ui.screens.hallway

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmb.bestbefore.data.models.HallwayCard
import com.dmb.bestbefore.ui.components.OrbMenu
import com.dmb.bestbefore.ui.theme.LocalBestBeforeColors
import com.dmb.bestbefore.ui.theme.ThemeState
import androidx.core.graphics.toColorInt
import kotlin.math.absoluteValue

// ═══════════════════════════════════════════════════════════════════════════
// BB-UI-04 → BB-UI-10: Hallway Screen
// Contains: Rooming (stacked cards), Hallway (carousel), Artists (carousel)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun HallwayScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    viewModel: HallwayViewModel = viewModel()
) {
    val cards by viewModel.filteredCards.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val selectedCardIndex by viewModel.selectedCardIndex.collectAsState()
    val selectedFilterTag by viewModel.selectedFilterTag.collectAsState()
    val isOrbMenuVisible by viewModel.isOrbMenuVisible.collectAsState()
    val showingSoundCloudModal by viewModel.showingSoundCloudModal.collectAsState()
    val isDescriptionExpanded by viewModel.isDescriptionExpanded.collectAsState()
    val activePagerPage by viewModel.activePagerPage.collectAsState()
    val areCollaboratorsExpanded by viewModel.areCollaboratorsExpanded.collectAsState()
    val cardImageIndices by viewModel.cardImageIndices.collectAsState()
    val orbWidth = 160.dp
    val orbHeight = 220.dp

    // BB-UI-06: When orb hides, all elements shift toward center
    val centerShiftOffset by animateDpAsState(
        targetValue = if (isOrbMenuVisible) 0.dp else 40.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "centerShift"
    )

    val colors = LocalBestBeforeColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    // BB-UI-06: Swipe RIGHT → orb menu disappears
                    val isRightSwipe = dragAmount > 20
                    // BB-UI-06: Swipe LEFT from right edge → orb menu reappears
                    val isEdgeSwipeLeft = dragAmount < -20 && change.position.x > size.width * 0.7f
                    if (isRightSwipe) {
                        viewModel.setOrbMenuVisible(false)
                    } else if (isEdgeSwipeLeft) {
                        viewModel.setOrbMenuVisible(true)
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            when (currentTab) {
                // ═════════════════════════════════════════════════════════
                // BB-UI-04: ROOMING TAB
                // ═════════════════════════════════════════════════════════
                BottomTab.ROOMING -> {
                    RoomingContent(
                        cards = cards,
                        modifier = Modifier.weight(1f)
                    )
                }

                // ═════════════════════════════════════════════════════════
                // BB-UI-05 → BB-UI-10: HALLWAY & ARTISTS TABS
                // ═════════════════════════════════════════════════════════
                else -> {
                    HallwayContent(
                        cards = cards,
                        currentTab = currentTab,
                        selectedFilterTag = selectedFilterTag,
                        onFilterTagSelected = { viewModel.setSelectedFilterTag(it) },
                        onNavigateToNotifications = onNavigateToNotifications,
                        onShowSoundCloud = { viewModel.setSoundCloudModalVisible(true) },
                        onExpandDescription = { viewModel.setDescriptionExpanded(true) },
                        centerShiftOffset = centerShiftOffset,
                        isOrbMenuVisible = isOrbMenuVisible,
                        selectedCardIndex = selectedCardIndex,
                        cardImageIndices = cardImageIndices,
                        areCollaboratorsExpanded = areCollaboratorsExpanded,
                        onToggleCollaborators = { viewModel.toggleCollaboratorsExpanded() },
                        onCollapseCollaborators = { viewModel.collapseCollaborators() },
                        onImageIndexChange = { cardId, index -> viewModel.setCardImageIndex(cardId, index) },
                        onPagerPageChanged = { viewModel.setActivePagerPage(it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Bottom Navigation ───────────────────────────────────
            BottomNavigation(
                currentTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        }

        // ── Orb Menu ────────────────────────────────────────────────
        // BB-UI-06: Animated enter/exit from right edge
        AnimatedVisibility(
            visible = isOrbMenuVisible,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
            ),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            OrbMenu(
                width = orbWidth,
                height = orbHeight,
                onProfileClick = onNavigateToProfile,
                onAddClick = { },
                onSearchClick = { },
                onChatClick = { }
            )
        }

        // ── SoundCloud Modal ────────────────────────────────────────
        if (showingSoundCloudModal && currentTab != BottomTab.ROOMING && cards.isNotEmpty()) {
            val currentCard = cards.getOrNull(activePagerPage)
            if (currentCard != null) {
                val themeColor = parseThemeColor(currentCard.themeColorHex)
                SoundCloudPlayerSheet(
                    card = currentCard,
                    themeColor = themeColor,
                    onDismiss = { viewModel.setSoundCloudModalVisible(false) }
                )
            }
        }

        // ── BB-UI-10: Expanded Description Overlay ──────────────────
        if (isDescriptionExpanded && cards.isNotEmpty()) {
            val currentCard = cards.getOrNull(activePagerPage)
            if (currentCard != null) {
                val themeColor = parseThemeColor(currentCard.themeColorHex)
                ExpandedDescriptionOverlay(
                    card = currentCard,
                    themeColor = themeColor,
                    onShowSoundCloud = { viewModel.setSoundCloudModalVisible(true) },
                    onDismiss = { viewModel.setDescriptionExpanded(false) }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// BB-UI-04: ROOMING CONTENT — Stacked vertical cards
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun RoomingContent(
    cards: List<HallwayCard>,
    modifier: Modifier = Modifier
) {
    val colors = LocalBestBeforeColors.current
    val cardHeight = 280.dp
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            // ── Header ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 25.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rooming",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = "Filter",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { /* Scan action */ }
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Scan",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            "Scan",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }

        item {
            // ── Search Bar ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 15.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = colors.textSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Search by name, owner, or tags...",
                    color = colors.textSecondary
                )
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }

        if (cards.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        null,
                        tint = Color.Gray.copy(alpha = 0.3f),
                        modifier = Modifier.size(50.dp)
                    )
                    Text("No rooms discovered yet.", color = Color.Gray)
                }
            }
        } else {
            itemsIndexed(cards) { _, card ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(cardHeight)
                ) {
                    RoomingCard(card = card, height = cardHeight.value.toInt())
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ── BB-UI-04: Rooming Card ──────────────────────────────────────────────
@Composable
private fun RoomingCard(card: HallwayCard, height: Int) {
    val themeColor = parseThemeColor(card.themeColorHex)
    val colors = LocalBestBeforeColors.current
    val isLocked = card.timeCapsuleDays > 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.DarkGray)
            .clickable { /* Navigate to room */ }
    ) {
        // Gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(themeColor.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.9f))
                    )
                )
        )

        // Settings icon (top-right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }

        // Bottom info
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = card.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = "Time Capsule: ${card.timeCapsuleDays}d 0h 0m",
                fontSize = 14.sp,
                color = colors.textSecondary
            )
            Text(
                "Click to view details >",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Locked badge
            if (isLocked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("🔒", fontSize = 12.sp)
                    Text(
                        "Locked",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// BB-UI-05 → BB-UI-10: HALLWAY/ARTISTS CONTENT — Carousel + Details
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun HallwayContent(
    cards: List<HallwayCard>,
    currentTab: BottomTab,
    selectedFilterTag: String?,
    onFilterTagSelected: (String?) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onShowSoundCloud: () -> Unit,
    onExpandDescription: () -> Unit,
    centerShiftOffset: Dp,
    isOrbMenuVisible: Boolean,
    selectedCardIndex: Int,
    cardImageIndices: Map<String, Int>,
    areCollaboratorsExpanded: Boolean,
    onToggleCollaborators: () -> Unit,
    onCollapseCollaborators: () -> Unit,
    onImageIndexChange: (String, Int) -> Unit,
    onPagerPageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalBestBeforeColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        // ── Header ──────────────────────────────────────────────────
        HallwayHeader(
            title = if (currentTab == BottomTab.EVERYONE) "Hallway" else "Artists",
            onNavigateToNotifications = onNavigateToNotifications
        )

        // ── Search + Tags ───────────────────────────────────────────
        SearchBarAndTags(
            selectedTag = selectedFilterTag,
            onTagSelected = onFilterTagSelected
        )

        if (cards.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No rooms found.", color = Color.Gray, fontSize = 20.sp)
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { cards.size })
            val activeCard = cards[pagerState.currentPage]
            val themeColor = parseThemeColor(activeCard.themeColorHex)

            LaunchedEffect(selectedCardIndex, cards.size) {
                val coercedIndex = selectedCardIndex.coerceIn(0, cards.size - 1)
                if (pagerState.currentPage != coercedIndex) {
                    pagerState.scrollToPage(coercedIndex)
                }
            }

            LaunchedEffect(themeColor, ThemeState.syncAccentWithRoom) {
                ThemeState.syncAccent(themeColor)
            }

            // Notify parent of current page for overlays
            LaunchedEffect(pagerState.currentPage) {
                onPagerPageChanged(pagerState.currentPage)
            }

            // BB-UI-06: Animated content padding based on orb visibility
            // When orb is visible, carousel is slightly left-shifted (default)
            // When orb is hidden, everything shifts rightward toward center
            val carouselPadding by animateDpAsState(
                targetValue = if (isOrbMenuVisible) 48.dp else 24.dp,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
                label = "carouselPadding"
            )

            // BB-UI-06: CD button end padding animates with orb
            val cdButtonEndPadding by animateDpAsState(
                targetValue = if (isOrbMenuVisible) 60.dp else 16.dp,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
                label = "cdButtonEnd"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column {
                    // ── Room Name + Location ────────────────────────
                    // BB-UI-06: Room name shifts with centerShiftOffset
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(x = centerShiftOffset)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = activeCard.title,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            if (!activeCard.location.isNullOrEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📍", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = activeCard.location,
                                        fontSize = 14.sp,
                                        color = colors.textSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }

                    // ── Carousel ────────────────────────────────────
                    // BB-UI-06: Carousel shifts with centerShiftOffset
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .offset(x = centerShiftOffset),
                        contentPadding = PaddingValues(horizontal = carouselPadding),
                        pageSpacing = 16.dp
                    ) { page ->
                        val card = cards[page]
                        val pageOffset = ((pagerState.currentPage - page) +
                                pagerState.currentPageOffsetFraction).absoluteValue
                        val parsedColor = parseThemeColor(card.themeColorHex)

                        // BB-UI-05: Glow dims as card moves off-center
                        val glowAlpha = 1f - (pageOffset * 1.5f).coerceIn(0f, 1f)

                        HallwayActiveCard(
                            card = card,
                            glowAlpha = glowAlpha,
                            themeColor = parsedColor,
                            currentImageIndex = cardImageIndices[card.id] ?: 0,
                            onImageIndexChange = { newIndex -> onImageIndexChange(card.id, newIndex) }
                        )
                    }

                    // ── Card Details ────────────────────────────────
                    Spacer(modifier = Modifier.height(16.dp))
                    ActiveCardDetails(
                        card = activeCard,
                        themeColor = themeColor,
                        accentColor = colors.primary,
                        hasLocation = !activeCard.location.isNullOrEmpty(),
                        showAllCollaborators = areCollaboratorsExpanded,
                        onToggleCollaborators = onToggleCollaborators,
                        onDismissCollaborators = onCollapseCollaborators,
                        onSeeAllClick = onExpandDescription
                    )
                }

                // ── CD Button ───────────────────────────────────────
                // BB-UI-06: CD button animates with orb visibility
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = cdButtonEndPadding, top = 50.dp)
                        .offset(x = centerShiftOffset)
                        .width(64.dp)
                        .height(34.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(999.dp))
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onShowSoundCloud() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, "Play Music", tint = themeColor)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// BB-UI-05: Active Card with Glow + Vertical Image Swipe (1/N)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun HallwayActiveCard(
    card: HallwayCard,
    glowAlpha: Float,
    themeColor: Color,
    currentImageIndex: Int,
    onImageIndexChange: (Int) -> Unit
) {
    val imagesList = card.photos.ifEmpty { listOf("mock_bg") }
    val maxImages = imagesList.size.coerceAtMost(5)
    val colors = LocalBestBeforeColors.current

    // Subtle pulse to mimic the richer Swift glow language.
    val glowPulse = rememberInfiniteTransition(label = "hallwayGlowPulse")
    val pulseScale by glowPulse.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by glowPulse.animateFloat(
        initialValue = 0.9f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val dynamicGlowAlpha = glowAlpha * pulseAlpha

    // Animate scale: active card is 1f, side cards are 0.92f
    val animatedScale by animateFloatAsState(
        targetValue = if (glowAlpha >= 0.9f) 1f else 0.92f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "cardScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .scale(animatedScale)
            .pointerInput(card.id) {
                detectVerticalDragGestures(
                    onDragEnd = { }
                ) { change, dragAmount ->
                    change.consume()
                    if (dragAmount < -30) {
                        // Swipe UP → next image
                        onImageIndexChange((currentImageIndex + 1) % maxImages)
                    } else if (dragAmount > 30) {
                        // Swipe DOWN → previous image
                        onImageIndexChange(if (currentImageIndex - 1 < 0)
                            maxImages - 1 else currentImageIndex - 1)
                    }
                }
            }
    ) {
        // Layer 1: wide ambient halo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .alpha(dynamicGlowAlpha * 0.34f)
                .blur(60.dp)
                .scale(1.2f * pulseScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            themeColor.copy(alpha = 0.88f),
                            themeColor.copy(alpha = 0.42f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(34.dp)
                )
        )

        // Layer 2: inner bloom with white core for a richer neon feel
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .alpha(dynamicGlowAlpha * 0.7f)
                .blur(24.dp)
                .scale(1.08f * pulseScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.8f),
                            themeColor.copy(alpha = 0.95f),
                            themeColor.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
        )

        // Layer 3: soft rim light
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(1.dp)
                .alpha(dynamicGlowAlpha * 0.82f)
                .border(
                    width = 1.8.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.92f),
                            themeColor.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.55f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
        )

        // Card body
        Box(
        modifier = Modifier
            .fillMaxSize()
            .border(
                2.dp,
                themeColor.copy(alpha = dynamicGlowAlpha * 0.86f),
                RoundedCornerShape(32.dp)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF19192E), Color(0xFF14213D))
                )
            ),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Top sheen overlay to emulate Swift-style polished card surface.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f)
                    .align(Alignment.TopCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.14f),
                                Color.White.copy(alpha = 0.04f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Image index indicator (1/N) — always shown
            Box(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${currentImageIndex + 1}/$maxImages",
                    color = colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// BB-UI-07 → BB-UI-09: Card Details — Owner, Collaborators, Tags, Description
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun ActiveCardDetails(
    card: HallwayCard,
    themeColor: Color,
    accentColor: Color,
    hasLocation: Boolean,
    showAllCollaborators: Boolean,
    onToggleCollaborators: () -> Unit,
    onDismissCollaborators: () -> Unit,
    onSeeAllClick: () -> Unit
) {
    val colors = LocalBestBeforeColors.current
    val isCollabRoom = card.collaboratorCount > 0
    val hasDescription = card.description.isNotBlank() &&
            card.description != "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do usermod temper..."
    val hasTags = card.tags.isNotEmpty()

    // BB-UI-07: Entire details section is wrapped in a Box to allow
    // collaborator overlay to stack on TOP with highest z-index
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // ── Owner Row + Tags ────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Avatar + Username + Collaborator button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(themeColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.Black
                        )
                    }

                    // Username
                    Text(
                        text = "@${card.ownerEmail?.substringBefore("@") ?: "artist"}",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    // BB-UI-07: Collaborator toggle button
                    if (isCollabRoom) {
                        Box(
                            modifier = Modifier
                                .background(
                                    themeColor.copy(alpha = 0.2f),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    themeColor.copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onToggleCollaborators() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (showAllCollaborators) "show less"
                                else "+${card.collaboratorCount} more",
                                color = themeColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Right: Tags
                // BB-UI-08: Max 2 tags + "+" for non-collab rooms
                // BB-UI-08: Only "+ tags" for collab rooms
                // BB-UI-09: Empty when no tags
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!isCollabRoom && hasTags) {
                        card.tags.take(2).forEach { tag ->
                            TagChip("#$tag", themeColor)
                        }
                        if (card.tags.size > 2) {
                            TagChip("+", themeColor)
                        }
                    } else if (isCollabRoom && hasTags) {
                        TagChip("+ tags", themeColor)
                    }
                    // BB-UI-09: No tags → nothing rendered (empty)
                }
            }

            // ── Description ─────────────────────────────────────────
            // BB-UI-08: Location rooms → max 1 line
            // BB-UI-08: Collab rooms → max 1 line
            // BB-UI-09: No description → show placeholder
            val descMaxLines = when {
                hasLocation || isCollabRoom -> 1
                else -> 2
            }

            Text(
                text = if (hasDescription) card.description
                else "No description provided.",
                color = if (hasDescription) colors.textPrimary.copy(alpha = 0.7f)
                else colors.textSecondary,
                fontSize = 14.sp,
                maxLines = descMaxLines,
                overflow = TextOverflow.Ellipsis
            )

            // See All button — shown when description is truncatable or tags > 2
            // BB-UI-09: Not shown when no description AND no tags
            if (hasDescription || (hasTags && card.tags.size > 2)) {
                Text(
                    text = "See All",
                    color = accentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onSeeAllClick() }
                )
            }
        }

        // ── BB-UI-07: Collaborator Overlay ──────────────────────────
        // Stacks UPWARD from the owner row, highest z-index layer
        // Dark background behind each avatar+nickname
        AnimatedVisibility(
            visible = showAllCollaborators,
            enter = fadeIn(tween(200)) + expandVertically(
                expandFrom = Alignment.Bottom,
                animationSpec = tween(300)
            ),
            exit = fadeOut(tween(200)) + shrinkVertically(
                shrinkTowards = Alignment.Bottom,
                animationSpec = tween(200)
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(y = (-50).dp)
                .zIndex(100f)
        ) {
            // Tap-to-dismiss wrapper
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onDismissCollaborators() }
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    // Show collaborator accounts stacking upward
                    for (i in card.collaboratorCount downTo 1) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .background(
                                    Color.Black.copy(alpha = 0.85f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            // Small avatar with dark bg
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(themeColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "@collaborator$i",
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Tag Chip Component ──────────────────────────────────────────────────
@Composable
fun TagChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Header ──────────────────────────────────────────────────────────────
@Composable
fun HallwayHeader(title: String, onNavigateToNotifications: () -> Unit) {
    val colors = LocalBestBeforeColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 25.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Icon(Icons.AutoMirrored.Filled.List, null, tint = colors.textPrimary)
            Icon(
                Icons.Default.Notifications, null, tint = colors.textPrimary,
                modifier = Modifier.clickable { onNavigateToNotifications() }
            )
        }
    }
}

// ── Search Bar + Filter Tags ────────────────────────────────────────────
@Composable
fun SearchBarAndTags(selectedTag: String?, onTagSelected: (String?) -> Unit) {
    val colors = LocalBestBeforeColors.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 15.dp)
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = colors.textSecondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Search...", color = colors.textSecondary)
        }

        val tags = listOf("trip", "music", "science", "party", "family")
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .background(
                            if (selectedTag == null) colors.primary
                            else Color.White.copy(alpha = 0.1f),
                            CircleShape
                        )
                        .clickable { onTagSelected(null) }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        "All",
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            items(tags) { tag ->
                Box(
                    modifier = Modifier
                        .background(
                            if (selectedTag == tag) colors.primary
                            else Color.White.copy(alpha = 0.1f),
                            CircleShape
                        )
                        .clickable { onTagSelected(tag) }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        "#$tag",
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// BB-UI-10: Expanded Description Overlay (See All)
// Full-screen overlay with room theme color background
// Layout: Title → Card → Avatar+Name → Description → See Less → TAGS
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun ExpandedDescriptionOverlay(
    card: HallwayCard,
    themeColor: Color,
    onShowSoundCloud: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalBestBeforeColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColor)
            .zIndex(100f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // ── Title + CD Button Row ───────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()) {
                // Room name centered
                Text(
                    text = card.title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 56.dp) // Space for CD button
                )

                // CD button (top right) — BB-UI-10
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .clip(CircleShape)
                        .clickable { onShowSoundCloud() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Card Placeholder (shows room image area) ────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder for actual room image (AsyncImage would go here)
                Text(
                    text = card.title,
                    color = colors.textSecondary.copy(alpha = 0.6f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Avatar + Username ────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(themeColor.copy(alpha = 0.8f), CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "@${card.ownerEmail?.substringBefore("@") ?: "artist"}",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Full Description ─────────────────────────────────────
            Text(
                text = if (card.description.isNotBlank()) card.description
                else "No description provided.",
                color = colors.textPrimary,
                fontSize = 16.sp,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── See Less ─────────────────────────────────────────────
            Text(
                text = "See Less",
                color = colors.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onDismiss() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── TAGS Section ─────────────────────────────────────────
            // BB-UI-10: All tags fully displayed at the bottom
            if (card.tags.isNotEmpty()) {
                Text(
                    "TAGS",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Wrap tags in a flow-like row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(card.tags) { tag ->
                        TagChip("#$tag", colors.textPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ── Bottom Navigation ───────────────────────────────────────────────────
@Composable
private fun BottomNavigation(currentTab: BottomTab, onTabSelected: (BottomTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        BottomNavItem("Rooming", currentTab == BottomTab.ROOMING) {
            onTabSelected(BottomTab.ROOMING)
        }
        BottomNavItem("Hallway", currentTab == BottomTab.EVERYONE) {
            onTabSelected(BottomTab.EVERYONE)
        }
        BottomNavItem("Artists", currentTab == BottomTab.ARTISTS) {
            onTabSelected(BottomTab.ARTISTS)
        }
    }
}

@Composable
private fun BottomNavItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = LocalBestBeforeColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        if (isSelected) {
            Text("▽", fontSize = 12.sp, color = colors.textPrimary)
        }
        Text(
            text = text,
            fontSize = if (isSelected) 18.sp else 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) colors.textPrimary else colors.textSecondary
        )
    }
}

// ── Helper: Parse theme color from hex string ───────────────────────────
private fun parseThemeColor(hex: String?): Color {
    return hex?.let {
        try {
            Color(it.toColorInt())
        } catch (_: Exception) {
            Color(0xFF007AFF)
        }
    } ?: Color(0xFF007AFF)
}
