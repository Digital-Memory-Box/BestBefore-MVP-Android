@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.dmb.bestbefore.ui.screens.hallway

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmb.bestbefore.ui.screens.notifications.NotificationViewModel
import com.dmb.bestbefore.data.models.CalendarEvent
import com.dmb.bestbefore.data.models.HallwayCard
import com.dmb.bestbefore.ui.components.AnimatedBackgroundView
import com.dmb.bestbefore.ui.components.OrbMenu
import com.dmb.bestbefore.ui.components.ProfileAvatar
import com.dmb.bestbefore.ui.theme.LocalBestBeforeColors
import com.dmb.bestbefore.ui.theme.ThemeState
import androidx.core.graphics.toColorInt
import com.dmb.bestbefore.ui.components.VideoPlayer
import kotlin.math.absoluteValue


// ═══════════════════════════════════════════════════════════════════════════
// BB-UI-04 → BB-UI-10: Hallway Screen
// Contains: Rooming (stacked cards), Hallway (carousel), Artists (carousel)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun HallwayScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToCreatorProfile: (String) -> Unit = {},
    onNavigateToNotifications: () -> Unit,
    onRoomingMusicClick: () -> Unit = {},
    onRoomingScanClick: () -> Unit = {},
    onOpenRoom: (HallwayCard) -> Unit = {},
    onCreateRoomClick: () -> Unit = {},
    onCameraClick: () -> Unit = {},
    userProfileImageUrl: String? = null,
    viewModel: HallwayViewModel = viewModel()
) {
    val cards by viewModel.filteredCards.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCardIndex by viewModel.selectedCardIndex.collectAsState()
    val selectedFilterTag by viewModel.selectedFilterTag.collectAsState()
    val isOrbMenuVisible by viewModel.isOrbMenuVisible.collectAsState()
    val showingSoundCloudModal by viewModel.showingSoundCloudModal.collectAsState()
    val isDescriptionExpanded by viewModel.isDescriptionExpanded.collectAsState()
    val activePagerPage by viewModel.activePagerPage.collectAsState()
    val availableTags by viewModel.availableTags.collectAsState()
    val areCollaboratorsExpanded by viewModel.areCollaboratorsExpanded.collectAsState()
    val cardImageIndices by viewModel.cardImageIndices.collectAsState()
    val savedRoomCards by viewModel.savedRoomCards.collectAsState()
    val similarModeSource by viewModel.similarModeSource.collectAsState()
    val roomingFilter by viewModel.roomingFilter.collectAsState()
    val isInitialLoading by viewModel.isInitialLoading.collectAsState()
    val serverStatus by viewModel.serverStatus.collectAsState()
    val notificationViewModel: NotificationViewModel = viewModel()
    val notificationCount by notificationViewModel.notifications.collectAsState()
    // Responsive orb diameter to avoid collapse/clipping on narrow phones.
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val orbWidth = (screenWidthDp * 0.82f).dp.coerceIn(300.dp, 420.dp)
    val contentEndInset = 0.dp

    // Refresh rooms data every time screen enters composition
    LaunchedEffect(Unit) {
        viewModel.refreshRooms()
        notificationViewModel.refresh()
    }

    // Derive the active card's theme name to drive the background color.
    // Rooming: first saved/collaborator card. Hallway/Artists: pager-active card.
    val activeCardTheme = when (currentTab) {
        BottomTab.ROOMING -> (savedRoomCards.firstOrNull() ?: cards.firstOrNull())
            ?.themeColorHex?.lowercase() ?: "default"
        else -> cards.getOrNull(activePagerPage)
            ?.themeColorHex?.lowercase() ?: "default"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
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
        // Full-screen animated background — color animates to match the active room's theme
        AnimatedBackgroundView(theme = activeCardTheme)

        Column(modifier = Modifier.fillMaxSize()) {

            when (currentTab) {
                // ═════════════════════════════════════════════════════════
                // BB-UI-04: ROOMING TAB
                // ═════════════════════════════════════════════════════════
                BottomTab.ROOMING -> {
                    RoomingContent(
                        collaboratorCards = cards,
                        savedCards = savedRoomCards,
                        searchQuery = searchQuery,
                        onSearchQueryChange = viewModel::setSearchQuery,
                        contentEndInset = contentEndInset,
                        onMusicClick = onRoomingMusicClick,
                        onScanClick = onRoomingScanClick,
                        onOpenRoom = onOpenRoom,
                        onRemoveSaved = { cardId -> viewModel.removeSavedRoom(cardId) },
                        roomingFilter = roomingFilter,
                        onFilterChange = { viewModel.setRoomingFilter(it) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // ═════════════════════════════════════════════════════════
                // BB-UI-05 → BB-UI-10: HALLWAY & ARTISTS TABS
                // ═════════════════════════════════════════════════════════
                else -> {
                    if (isInitialLoading) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = LocalBestBeforeColors.current.primary)
                                val loadingText = when (serverStatus) {
                                    HallwayViewModel.ServerStatus.WARMING_UP ->
                                        "Server is starting up...\nThis takes ~30 s after inactivity."
                                    HallwayViewModel.ServerStatus.READY ->
                                        if (currentTab == BottomTab.ARTISTS) "Loading artists..." else "Loading rooms..."
                                    else ->
                                        "Connecting..."
                                }
                                androidx.compose.material3.Text(
                                    text = loadingText,
                                    color = LocalBestBeforeColors.current.textSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        HallwayContent(
                            cards = cards,
                            searchQuery = searchQuery,
                            onSearchQueryChange = viewModel::setSearchQuery,
                            currentTab = currentTab,
                            selectedFilterTag = selectedFilterTag,
                            onFilterTagSelected = viewModel::setSelectedFilterTag,
                            onNavigateToCreatorProfile = onNavigateToCreatorProfile,
                            onNavigateToNotifications = onNavigateToNotifications,
                            onMusicClick = onRoomingMusicClick,
                            onShowSoundCloud = { viewModel.setSoundCloudModalVisible(true) },
                            onExpandDescription = { viewModel.setDescriptionExpanded(true) },
                            availableTags = availableTags,
                            isOrbMenuVisible = isOrbMenuVisible,
                            contentEndInset = contentEndInset,
                            selectedCardIndex = selectedCardIndex,
                            cardImageIndices = cardImageIndices,
                            areCollaboratorsExpanded = areCollaboratorsExpanded,
                            onToggleCollaborators = viewModel::toggleCollaboratorsExpanded,
                            onCollapseCollaborators = viewModel::collapseCollaborators,
                            onOpenRoom = onOpenRoom,
                            onImageIndexChange = viewModel::setCardImageIndex,
                            onPagerPageChanged = viewModel::setActivePagerPage,
                            similarModeSource = similarModeSource,
                            onEnterSimilarMode = viewModel::enterSimilarMode,
                            onExitSimilarMode = viewModel::exitSimilarMode,
                            onConnectRoom = viewModel::connectRoom,
                            notificationCount = notificationCount.size,
                            onDeleteMemory = { roomId, memoryId -> viewModel.deleteMemory(roomId, memoryId) },
                            modifier = Modifier.weight(1f)
                        )
                    }
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
                diameter = orbWidth,
                onProfileClick = onNavigateToProfile,
                onAddClick = onCreateRoomClick,
                onCameraClick = onCameraClick,
                profileImageUrl = userProfileImageUrl
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
    collaboratorCards: List<HallwayCard>,
    savedCards: List<HallwayCard>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    contentEndInset: Dp,
    onMusicClick: () -> Unit,
    onScanClick: () -> Unit,
    onOpenRoom: (HallwayCard) -> Unit,
    onRemoveSaved: (String) -> Unit,
    roomingFilter: HallwayViewModel.RoomingFilter = HallwayViewModel.RoomingFilter.NONE,
    onFilterChange: (HallwayViewModel.RoomingFilter) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalBestBeforeColors.current
    val cardHeight = 280.dp

    // Apply category and search filter
    val filteredSaved = if (roomingFilter == HallwayViewModel.RoomingFilter.COLLABORATED_ONLY) {
        emptyList()
    } else {
        savedCards.filter { card ->
            searchQuery.isBlank() || card.title.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredCollaborator = if (roomingFilter == HallwayViewModel.RoomingFilter.SAVED_ONLY) {
        emptyList()
    } else {
        collaboratorCards.filter { card ->
            searchQuery.isBlank() || card.title.contains(searchQuery, ignoreCase = true)
        }
    }

    val isEmpty = filteredSaved.isEmpty() && filteredCollaborator.isEmpty()

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = contentEndInset),
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
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onScanClick() }
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "Scan",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(24.dp)
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
            // ── Rooming Filters (Saved / Collaborated) ──────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // SAVED Button
                FilterChip(
                    label = "Saved",
                    isSelected = roomingFilter == HallwayViewModel.RoomingFilter.SAVED_ONLY,
                    onClick = {
                        if (roomingFilter == HallwayViewModel.RoomingFilter.SAVED_ONLY) onFilterChange(HallwayViewModel.RoomingFilter.NONE)
                        else onFilterChange(HallwayViewModel.RoomingFilter.SAVED_ONLY)
                    }
                )
                // COLLABORATED Button
                FilterChip(
                    label = "Collaborated",
                    isSelected = roomingFilter == HallwayViewModel.RoomingFilter.COLLABORATED_ONLY,
                    onClick = {
                        if (roomingFilter == HallwayViewModel.RoomingFilter.COLLABORATED_ONLY) onFilterChange(HallwayViewModel.RoomingFilter.NONE)
                        else onFilterChange(HallwayViewModel.RoomingFilter.COLLABORATED_ONLY)
                    }
                )
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
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = colors.textPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isBlank()) {
                            Text("Search by name, owner, or tags...", color = colors.textSecondary)
                        }
                        innerTextField()
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // ─── SAVED ROOMS SECTION ─────────────────────────────────────────
        if (roomingFilter != HallwayViewModel.RoomingFilter.COLLABORATED_ONLY) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Saved Rooms",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(Color(0xFF007AFF).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            filteredSaved.size.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF007AFF)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            if (filteredSaved.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(100.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Bookmark, null, tint = Color.Gray, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No saved rooms yet.", color = Color.Gray, fontSize = 13.sp)
                            Text("Open a room and tap Add to Rooming.", color = Color.Gray.copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                    }
                }
            } else {
                itemsIndexed(filteredSaved) { _, card ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(cardHeight)
                    ) {
                        RoomingCard(
                            card = card,
                            height = cardHeight.value.toInt(),
                            isSaved = true,
                            onClick = { onOpenRoom(card) },
                            onRemove = { onRemoveSaved(card.id) }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            item { Spacer(modifier = Modifier.height(28.dp)) }
        }

        // ─── COLLABORATORS SECTION ────────────────────────────────────────
        if (roomingFilter != HallwayViewModel.RoomingFilter.SAVED_ONLY) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = null,
                        tint = Color(0xFFAF52DE),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Collaborators",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(Color(0xFFAF52DE).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            filteredCollaborator.size.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFAF52DE)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            if (filteredCollaborator.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(100.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PersonAdd, null, tint = Color.Gray, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No collaborative rooms.", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                itemsIndexed(filteredCollaborator) { _, card ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(cardHeight)
                    ) {
                        RoomingCard(
                            card = card,
                            height = cardHeight.value.toInt(),
                            isSaved = false,
                            onClick = { onOpenRoom(card) },
                            onRemove = {}
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ── BB-UI-04: Rooming Card ──────────────────────────────────────────────
@Composable
private fun RoomingCard(
    card: HallwayCard,
    height: Int,
    isSaved: Boolean = false,
    onClick: () -> Unit,
    onRemove: () -> Unit = {}
) {
    val colors = LocalBestBeforeColors.current
    val themeColor = parseThemeColor(card.themeColorHex, fallback = colors.primary)
    val isLocked = com.dmb.bestbefore.utils.DateUtils.isLocked(card.unlockDate)
    val isViewer = card.isViewerOnly
    var showRemoveConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .border(2.dp, themeColor.copy(alpha = 0.75f), RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(themeColor.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.9f))
                )
            )
            .clickable { onClick() }
    ) {
        // Gradient or Photo background
        val searchKeyword = card.title.lowercase()
            .replace("'s", "")
            .split(" ")
            .filter { it.length > 3 && it !in listOf("room", "hallway", "best", "before", "collection") }
            .take(2)
            .joinToString(",")
            .takeIf { it.isNotBlank() } ?: "abstract"
        // Normalise photo URL: HTTP → use as-is; data:image → use as-is;
        // raw base64 (no prefix) → prepend data:image/jpeg;base64,;
        // anything else → fall back to placeholder.
        fun normalizePhotoUrl(url: String?): String? {
            if (url.isNullOrBlank()) return null
            return when {
                url.startsWith("http") || url.startsWith("data:image") -> url
                url.startsWith("data:") && url.contains("base64,") ->
                    "data:image/jpeg;base64," + url.substringAfter("base64,")
                url.length > 100 -> "data:image/jpeg;base64,$url"
                else -> null
            }
        }
        val rawUrl = if (!card.imageUrl.isNullOrBlank()) card.imageUrl
                     else card.photos.firstOrNull()?.url
        val roomImage = normalizePhotoUrl(rawUrl) ?: "https://loremflickr.com/640/480/$searchKeyword"

        Box(modifier = Modifier.fillMaxSize()) {
            coil.compose.AsyncImage(
                model = roomImage,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                alpha = 0.6f
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(themeColor.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )
        }

        // Saved badge top-right
        if (isSaved) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .background(Color(0xFF007AFF).copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Saved", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // Remove button for saved rooms (top-left corner)
        if (isSaved) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
                    .size(30.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .clickable { showRemoveConfirm = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // ── Explorer Mode banner (Viewer-only rooms) ──────────────────
        if (isViewer) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(if (isSaved) PaddingValues(start = 14.dp, top = 54.dp) else PaddingValues(14.dp))
                    .fillMaxWidth(0.92f)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1565C0).copy(alpha = 0.92f), Color(0xFF0D47A1).copy(alpha = 0.92f))
                        ),
                        RoundedCornerShape(14.dp)
                    )
                    .border(1.dp, Color(0xFF42A5F5).copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Globe icon in accent circle
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xFF1E88E5), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Explore,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "EXPLORER MODE",
                        color = Color(0xFF90CAF9),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        "Viewing memories in this public space.",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFF42A5F5).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF42A5F5), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "READ ONLY",
                        color = Color(0xFF90CAF9),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp
                    )
                }
            }
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
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            
            // Owner Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                ProfileAvatar(
                    imageUri = card.ownerProfilePic,
                    size = 24.dp,
                    accentColor = Color.White
                )
                Text(
                    text = card.ownerName ?: "artist",
                    fontSize = 12.sp,
                    color = colors.textPrimary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = if (isLocked) "Unlocks in: ${com.dmb.bestbefore.utils.DateUtils.formatCountdown(card.unlockDate)}"
                       else "Unlocked",
                fontSize = 13.sp,
                color = colors.textSecondary
            )
            Text(
                "Tap to view details >",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp)
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

    // Confirm remove dialog
    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            containerColor = Color(0xFF1C1C1E),
            title = { Text("Remove from Rooming", color = Color.White) },
            text = { Text("Remove \"${card.title}\" from your saved rooms?", color = Color.LightGray) },
            confirmButton = {
                TextButton(onClick = { onRemove(); showRemoveConfirm = false }) {
                    Text("Remove", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// BB-UI-05 → BB-UI-10: HALLWAY/ARTISTS CONTENT — Carousel + Details
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun HallwayContent(
    cards: List<HallwayCard>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    currentTab: BottomTab,
    selectedFilterTag: String?,
    onFilterTagSelected: (String?) -> Unit,
    onNavigateToCreatorProfile: (String) -> Unit = {},
    onNavigateToNotifications: () -> Unit,
    onMusicClick: () -> Unit,
    onShowSoundCloud: () -> Unit,
    onExpandDescription: () -> Unit,
    availableTags: List<String>,
    isOrbMenuVisible: Boolean,
    contentEndInset: Dp,
    selectedCardIndex: Int,
    cardImageIndices: Map<String, Int>,
    areCollaboratorsExpanded: Boolean,
    onToggleCollaborators: () -> Unit,
    onCollapseCollaborators: () -> Unit,
    onOpenRoom: (HallwayCard) -> Unit,
    onImageIndexChange: (String, Int) -> Unit,
    onPagerPageChanged: (Int) -> Unit,
    similarModeSource: HallwayCard? = null,
    onEnterSimilarMode: (HallwayCard) -> Unit = {},
    onExitSimilarMode: () -> Unit = {},
    onConnectRoom: (HallwayCard) -> Unit = {},
    notificationCount: Int,
    onDeleteMemory: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val colors = LocalBestBeforeColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = contentEndInset)
    ) {
        // ── Header ──────────────────────────────────────────────────
        HallwayHeader(
            title = if (currentTab == BottomTab.EVERYONE) "Hallway" else "Artists",
            onMusicClick = onMusicClick,
            onNavigateToNotifications = onNavigateToNotifications,
            notificationCount = notificationCount
        )

        // ── Search + Tags ───────────────────────────────────────────
        SearchBarAndTags(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            selectedTag = selectedFilterTag,
            onTagSelected = onFilterTagSelected,
            tags = availableTags,
            similarModeSourceName = similarModeSource?.title,
            onExitSimilarMode = onExitSimilarMode
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
            val safeIndex = pagerState.currentPage.coerceIn(0, maxOf(0, cards.size - 1))
            val activeCard = cards[safeIndex]
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

            // Keep carousel geometry stable but bias right inset when orb is visible.
            val carouselPadding = PaddingValues(
                start = 40.dp,
                end = if (isOrbMenuVisible) 84.dp else 40.dp
            )
            val configuration = LocalConfiguration.current
            val cardHeight = (configuration.screenHeightDp * 0.35f).dp.coerceAtMost(340.dp)
            val cardWidthFraction = 0.9f
            val cdButtonEndPadding = if (isOrbMenuVisible) 68.dp else 60.dp

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── Room Name + Location ────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
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
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cardHeight),
                        contentPadding = carouselPadding,
                        pageSpacing = 8.dp
                    ) { page ->
                        val card = cards[page]
                        val pageOffset = ((pagerState.currentPage - page) +
                                pagerState.currentPageOffsetFraction).absoluteValue
                        val parsedColor = parseThemeColor(card.themeColorHex)

                        // BB-UI-05: Glow dims as card moves off-center
                        val glowAlpha = 1f - (pageOffset * 1.5f).coerceIn(0f, 1f)

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            HallwayActiveCard(
                                card = card,
                                glowAlpha = glowAlpha,
                                themeColor = parsedColor,
                                accentColor = colors.primary,
                                currentImageIndex = cardImageIndices[card.id] ?: 0,
                                onImageIndexChange = { newIndex -> onImageIndexChange(card.id, newIndex) },
                                onOpenRoom = { onOpenRoom(card) },
                                onDeleteMemory = onDeleteMemory,
                                cardHeight = cardHeight,
                                widthFraction = cardWidthFraction
                            )
                        }
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
                        onSeeAllClick = onExpandDescription,
                        onNavigateToCreatorProfile = onNavigateToCreatorProfile,
                        onShowSimilarRooms = { onEnterSimilarMode(activeCard) },
                        isSimilarMode = similarModeSource != null,
                        onConnectRoom = { onConnectRoom(activeCard) }
                    )
                }

                // ── CD Button ───────────────────────────────────────
                if (!activeCard.backgroundMusic.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = cdButtonEndPadding, top = 46.dp)
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.34f), CircleShape)
                            .border(1.dp, themeColor.copy(alpha = 0.45f), CircleShape)
                            .clip(CircleShape)
                            .clickable { onShowSoundCloud() },
                        contentAlignment = Alignment.Center
                    ) {
                        CdGlyph(size = 24.dp)
                    }
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
    accentColor: Color,
    currentImageIndex: Int,
    onImageIndexChange: (Int) -> Unit,
    onOpenRoom: () -> Unit,
    onDeleteMemory: (String, String) -> Unit = { _, _ -> },
    cardHeight: Dp = 350.dp,
    widthFraction: Float = 0.9f
) {
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

    // Deletion dialog state
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) } // memoryId

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Memory") },
            text = { Text("Do you want to delete this memory from the room? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { 
                    onDeleteMemory(card.id, showDeleteConfirm!!)
                    showDeleteConfirm = null 
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(cardHeight)
            .scale(animatedScale)
    ) {
        // Layer 1: wide ambient halo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .alpha(dynamicGlowAlpha * 0.34f)
                .blur(32.dp)
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
                .blur(16.dp)
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

        // Layer 3: soft rim light — uses room themeColor
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(1.dp)
                .alpha(dynamicGlowAlpha * 0.82f)
                .border(
                    width = 1.8.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.7f),
                            themeColor.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
        )

        // Card body — semi-transparent so the AnimatedBackground orbs bleed through
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    2.dp,
                    themeColor.copy(alpha = (dynamicGlowAlpha * 0.9f).coerceAtLeast(0.5f)),
                    RoundedCornerShape(32.dp)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            themeColor.copy(alpha = 0.38f),
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onOpenRoom() },
                        onLongPress = {
                            if (card.isOwnedByMe || card.isCollaborator) {
                                card.photos.firstOrNull()?.let { showDeleteConfirm = it.id }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            val latestPhoto = card.photos.firstOrNull()
            if (latestPhoto != null) {
                val mediaUrl = latestPhoto.url
                val isVideo = mediaUrl.endsWith(".mp4", ignoreCase = true) || 
                              mediaUrl.endsWith(".mov", ignoreCase = true) || 
                              mediaUrl.endsWith(".webm", ignoreCase = true) ||
                              mediaUrl.contains("/video/", ignoreCase = true)

                if (isVideo) {
                    VideoPlayer(
                        videoUrl = mediaUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    coil.compose.AsyncImage(
                        model = mediaUrl,
                        contentDescription = "Room Media",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            } else {
                // Fallback to LoremFlickr
                val searchKeyword = card.title.lowercase()
                    .replace("'s", "")
                    .split(" ")
                    .filter { it.length > 3 && it !in listOf("room", "hallway", "best", "before", "collection") }
                    .take(2)
                    .joinToString(",")
                    .takeIf { it.isNotBlank() } ?: "abstract"
                
                coil.compose.AsyncImage(
                    model = "https://loremflickr.com/640/800/$searchKeyword",
                    contentDescription = "Dummy Media",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    alpha = 0.6f
                )
            }
            
            // Top sheen overlay
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
    onSeeAllClick: () -> Unit,
    onNavigateToCreatorProfile: (String) -> Unit = {},
    onShowSimilarRooms: () -> Unit = {},
    isSimilarMode: Boolean = false,
    onConnectRoom: () -> Unit = {}
    ) {
    val colors = LocalBestBeforeColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val isCollabRoom = card.collaboratorCount > 0
    val hasDescription = card.description.isNotBlank()
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
                    ProfileAvatar(
                        imageUri = card.ownerProfilePic,
                        size = 44.dp,
                        accentColor = Color.White,
                        onClick = { card.ownerId?.let { onNavigateToCreatorProfile(it) } }
                    )

                    // Username
                    val nameText = (card.ownerName?.takeIf { it.isNotBlank() }
                                   ?: card.ownerEmail?.substringBefore("@")
                                   ?: "artist")
                    Text(
                        text = nameText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 4.dp)
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

            // Real Time Capsule status
            val isLocked = com.dmb.bestbefore.utils.DateUtils.isLocked(card.unlockDate)
            if (isLocked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("🔒", fontSize = 12.sp)
                    Text(
                        text = "Unlocks in: ${com.dmb.bestbefore.utils.DateUtils.formatCountdown(card.unlockDate)}",
                        color = Color(0xFFFF9800),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (hasDescription || (hasTags && card.tags.size > 2)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "See All" — pill background for readability
                    Box(
                        modifier = Modifier
                            .background(
                                accentColor.copy(alpha = 0.18f),
                                RoundedCornerShape(20.dp)
                            )
                            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                            .clickable { onSeeAllClick() }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "See All",
                            color = accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isSimilarMode) {
                        Row(
                            modifier = Modifier
                                .clickable { onShowSimilarRooms() }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("✨", fontSize = 12.sp)
                            Text(
                                text = "Show Similar Rooms",
                                color = Color(0xFF007AFF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // "Connection" button in Similarity Mode
                        Box(
                            modifier = Modifier
                                .background(accentColor, RoundedCornerShape(100.dp))
                                .clickable { onConnectRoom() }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Link, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "Connection",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else if (isSimilarMode) {
                // If description is short but we are in similar mode, still show Connection button
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .background(accentColor, RoundedCornerShape(100.dp))
                        .clickable { onConnectRoom() }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Link, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Text(
                            text = "Connection",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
                    // Show actual collaborator accounts stacking upward
                    card.collaborators.forEach { collaborator ->
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
                            // Profile Avatar
                            ProfileAvatar(
                                imageUri = collaborator.profileImageUrl,
                                size = 32.dp,
                                accentColor = themeColor
                            )

                            Text(
                                text = collaborator.name ?: collaborator.email.substringBefore("@"),
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

// ── Filter Chip Component ──────────────────────────────────────────────
@Composable
fun FilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = LocalBestBeforeColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) colors.primary else Color.White.copy(alpha = 0.1f),
        contentColor = if (isSelected) Color.Black else Color.White,
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
fun HallwayHeader(
    title: String,
    onMusicClick: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    notificationCount: Int
) {
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
            Box {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = colors.textPrimary,
                    modifier = Modifier.clickable { onNavigateToNotifications() }
                )
                if (notificationCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .background(Color(0xFFFF3B30), CircleShape)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ── Search Bar + Filter Tags ────────────────────────────────────────────
@Composable
fun SearchBarAndTags(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedTag: String?,
    onTagSelected: (String?) -> Unit,
    tags: List<String>,
    similarModeSourceName: String? = null,
    onExitSimilarMode: () -> Unit = {}
) {
    val colors = LocalBestBeforeColors.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 15.dp)
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))

            if (similarModeSourceName != null) {
                // Similarity Tag Chip INSIDE the Search Bar
                Box(
                    modifier = Modifier
                        .background(Color(0xFF007AFF).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .border(0.5.dp, Color(0xFF007AFF).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Similar Rooms as $similarModeSourceName",
                            color = Color(0xFF007AFF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Exit Similarity Mode",
                            tint = Color(0xFF007AFF),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onExitSimilarMode() }
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (searchQuery.isBlank() && similarModeSourceName == null) {
                        Text("Search by name, owner, or tags...", color = colors.textSecondary, fontSize = 14.sp)
                    }
                    innerTextField()
                }
            )
        }

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
    val overlayBase = Color(0xFF090B12)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(overlayBase)
            .zIndex(100f)
    ) {
        // Layered tint so overlay keeps the room theme without becoming a flat full-screen color.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black,
                            overlayBase,
                            Color.Black
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.62f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            themeColor.copy(alpha = 0.34f),
                            themeColor.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 22.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header + CD action
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = card.title,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 62.dp)
                )

                if (!card.backgroundMusic.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.36f))
                            .border(1.dp, themeColor.copy(alpha = 0.55f), CircleShape)
                            .clickable { onShowSoundCloud() },
                        contentAlignment = Alignment.Center
                    ) {
                        CdGlyph(size = 28.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Hero area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(352.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                themeColor.copy(alpha = 0.28f),
                                Color(0xFF161A2A),
                                Color(0xFF0F111C)
                            )
                        )
                    )
                    .border(1.dp, themeColor.copy(alpha = 0.45f), RoundedCornerShape(30.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Hero Photo
                val searchKeyword = card.title.lowercase()
                    .replace("'s", "")
                    .split(" ")
                    .filter { it.length > 3 && it !in listOf("room", "hallway", "best", "before", "collection") }
                    .take(2)
                    .joinToString(",")
                    .takeIf { it.isNotBlank() } ?: "abstract"
                val rawMediaUrl = card.photos.firstOrNull()?.url
                val mediaUrl = when {
                    rawMediaUrl == null -> "https://loremflickr.com/640/800/$searchKeyword"
                    rawMediaUrl.startsWith("http") || rawMediaUrl.startsWith("data:image") -> rawMediaUrl
                    rawMediaUrl.startsWith("data:") && rawMediaUrl.contains("base64,") ->
                        "data:image/jpeg;base64," + rawMediaUrl.substringAfter("base64,")
                    rawMediaUrl.length > 100 -> "data:image/jpeg;base64,$rawMediaUrl"
                    else -> "https://loremflickr.com/640/800/$searchKeyword"
                }
                coil.compose.AsyncImage(
                    model = mediaUrl,
                    contentDescription = card.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(themeColor.copy(alpha = 0.85f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = "@${card.ownerEmail?.substringBefore("@") ?: "artist"}",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (card.description.isNotBlank()) card.description
                else "No description provided.",
                color = colors.textPrimary.copy(alpha = 0.92f),
                fontSize = 16.sp,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "See Less",
                color = themeColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onDismiss() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (card.tags.isNotEmpty()) {
                Text(
                    "TAGS",
                    color = colors.textSecondary.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(card.tags) { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, themeColor.copy(alpha = 0.30f), RoundedCornerShape(11.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                color = colors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun CdGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF596171),
                        Color(0xFF434B59),
                        Color(0xFF2F3642),
                        Color(0xFF171C25)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
    ) {
        // Matte inner ring for a darker graphite disk finish.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(size * 0.62f)
                .clip(CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                .background(Color.Black.copy(alpha = 0.32f))
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(size * 0.20f)
                .clip(CircleShape)
                .background(Color(0xFF0A0D13))
                .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = size * 0.18f, top = size * 0.14f)
                .size(size * 0.10f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.34f))
        )
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

// ── Helper: Parse theme color from name or hex string ───────────────────
private fun parseThemeColor(hex: String?, fallback: Color = Color(0xFF007AFF)): Color {
    if (hex == null) return fallback
    val named = when (hex.trim().lowercase()) {
        "ocean"             -> Color(0xFF00C6A2)
        "sunset"            -> Color(0xFFE8820C)
        "forest"            -> Color(0xFF22A84A)
        "cyberpunk"         -> Color(0xFFAA3FD6)
        "artist", "vibrant" -> Color(0xFFE01982)
        "midnight"          -> Color(0xFF8800BB)
        "glass"             -> Color(0xFFBBBBBB)
        "default"           -> return fallback
        else                -> null
    }
    if (named != null) return named
    return try { Color(hex.toColorInt()) } catch (_: Exception) { fallback }
}
