package com.dmb.bestbefore.ui.screens.profile

import androidx.compose.animation.*
import com.dmb.bestbefore.data.models.HallwayCard
import com.dmb.bestbefore.ui.screens.hallway.BottomTab
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.verticalScroll
import com.dmb.bestbefore.data.models.TimeCapsuleRoom
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image



// --- EDIT ROOM SCREEN ---
@Composable
fun EditRoomScreen(viewModel: ProfileViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val roomName      by viewModel.roomName.collectAsState()
    val isPublic      by viewModel.isPublic.collectAsState()
    val tcEnabled     by viewModel.isTimeCapsuleEnabled.collectAsState()
    val days          by viewModel.capsuleDays.collectAsState()
    val hours         by viewModel.capsuleHours.collectAsState()
    val mins          by viewModel.capsuleMins.collectAsState()
    val theme         by viewModel.roomAtmosphereTheme.collectAsState()
    val music         by viewModel.selectedMusic.collectAsState()
    val rolling       by viewModel.rollingExpiration.collectAsState()
    val closure       by viewModel.scheduledClosureEnabled.collectAsState()
    val closureTime   by viewModel.scheduledClosureTime.collectAsState()
    val closureHour   by viewModel.scheduledClosureHour.collectAsState()
    val closureMin    by viewModel.scheduledClosureMinute.collectAsState()
    val tags          by viewModel.roomTags.collectAsState()
    val description   by viewModel.roomDescription.collectAsState()
    var tagInput      by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit Room", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Box(
                    modifier = Modifier.size(32.dp).background(Color(0xFF2C2C2E), CircleShape)
                        .clickable { viewModel.goBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }

            // â”€â”€â”€ Room Name â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Text("Room Name", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            BasicTextField(
                value = roomName,
                onValueChange = { viewModel.updateRoomName(it) },
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1C1C1E), RoundedCornerShape(10.dp)).padding(16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // â”€â”€â”€ Privacy â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Text("Privacy", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf(true to "Public" to Icons.Default.Public, false to "Private" to Icons.Default.Lock).forEach { (pair, icon) ->
                    val (pub, label) = pair
                    val sel = isPublic == pub
                    Box(
                        modifier = Modifier.weight(1f).height(90.dp)
                            .border(if (sel) 2.dp else 0.dp, if (sel) AccentBlue else Color.Transparent, RoundedCornerShape(12.dp))
                            .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp))
                            .clickable { viewModel.updateRoomMode(pub) }.padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                            Icon(icon, null, tint = if (sel) Color.White else Color.Gray)
                            Text(label, color = if (sel) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // â”€â”€â”€ Time Capsule â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp)).padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Enable Time Capsule", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Content hidden until timer ends.", color = Color.Gray, fontSize = 12.sp)
                }
                Switch(
                    checked = tcEnabled,
                    onCheckedChange = { viewModel.updateTimeCapsuleEnabled(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentBlue)
                )
            }

            if (tcEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Duration", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().background(CardDarkBg, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DurationStepper("Days",  days,  { viewModel.updateCapsuleDays(days - 1)  }, { viewModel.updateCapsuleDays(days + 1)  })
                    DurationStepper("Hours", hours, { viewModel.updateCapsuleHours(hours - 1) }, { viewModel.updateCapsuleHours(hours + 1) })
                    DurationStepper("Mins",  mins,  { viewModel.updateCapsuleMins(mins - 1)  }, { viewModel.updateCapsuleMins(mins + 1)  })
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // â”€â”€â”€ Music â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Text("Music", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp)).padding(8.dp)
            ) {
                val musicOpts = listOf("None", "Loft Beats", "Nature Ambience", "Lo-Fi", "Cinematic")
                musicOpts.forEach { m ->
                    val sel = music == m
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.updateSelectedMusic(m) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (sel) Icon(Icons.Default.CheckCircle, null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                        else Spacer(modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(m, color = if (sel) Color.White else Color.Gray, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // â”€â”€â”€ Rolling Expiration â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Text("Rolling Expiration", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Never", "1 Day", "7 Days", "30 Days").forEach { opt ->
                    val sel = rolling == opt
                    Box(
                        modifier = Modifier.weight(1f)
                            .background(if (sel) Color.White else Color.Transparent, RoundedCornerShape(18.dp))
                            .clickable { viewModel.updateRollingExpiration(opt) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(opt, color = if (sel) Color.Black else Color.Gray, fontSize = 12.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // â”€â”€â”€ Scheduled Closure â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Column(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp)).padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Scheduled Room Closure", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Lock to read-only after a specific date.", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = closure,
                        onCheckedChange = { viewModel.updateScheduledClosure(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentBlue)
                    )
                }
                if (closure) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Close on:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    InlineCalendar(
                        selectedMillis = closureTime,
                        hour = closureHour,
                        minute = closureMin,
                        onDateSelected = { viewModel.updateScheduledClosureTime(it) },
                        onTimeChanged = { h, m ->
                            viewModel.updateScheduledClosureHour(h)
                            viewModel.updateScheduledClosureMinute(m)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // â”€â”€â”€ Description â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Text("Description", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            BasicTextField(
                value = description,
                onValueChange = { viewModel.updateRoomDescription(it) },
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp, lineHeight = 20.sp),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.fillMaxWidth().height(90.dp)
                    .background(Color(0xFF1C1C1E), RoundedCornerShape(10.dp)).padding(12.dp),
                decorationBox = { inner ->
                    if (description.isEmpty()) Text("Add a descriptionâ€¦", color = Color.Gray, fontSize = 14.sp)
                    inner()
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // â”€â”€â”€ Tags â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Text("Tags", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1C1C1E), RoundedCornerShape(10.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    cursorBrush = SolidColor(Color.White),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (tagInput.isEmpty()) Text("Add tagâ€¦", color = Color.Gray, fontSize = 14.sp)
                        inner()
                    }
                )
                Icon(
                    Icons.Default.Add, null, tint = AccentBlue,
                    modifier = Modifier.size(24.dp).clickable {
                        if (tagInput.isNotBlank()) { viewModel.addRoomTag(tagInput.trim()); tagInput = "" }
                    }
                )
            }

            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        Row(
                            modifier = Modifier.background(AccentBlue.copy(alpha = 0.2f), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tag, color = AccentBlue, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Close, null, tint = AccentBlue, modifier = Modifier.size(14.dp).clickable { viewModel.removeRoomTag(tag) })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // â”€â”€â”€ Save Button â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Button(
                onClick = { viewModel.saveRoomEdits(context) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// --- EXISTING HELPERS ---

@Composable
fun CardStack(
    cards: List<HallwayCard>,
    selectedIndex: Int,
    onCardSelected: (Int) -> Unit,
    onCardTapped: (HallwayCard) -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (cards.isEmpty()) {
            Text("No cards available", color = Color.White, modifier = Modifier.align(Alignment.Center))
        } else {
            cards.forEachIndexed { index, card ->

                
                // Simplified Stack Logic for MVP visual
                if (index == selectedIndex) {
                    // Active card
                    Box(
                        modifier = Modifier
                            .offset(y = 0.dp)
                            .scale(1f)
                            .zIndex(100f)
                            .fillMaxWidth(0.8f)
                            .aspectRatio(0.7f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.DarkGray)
                            .clickable { onCardTapped(card) }
                    ) {
                        coil.compose.AsyncImage(
                            model = card.imageUrl,
                            contentDescription = card.title,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } 
            }
        }
    }
}

@Composable
fun BottomNavigation(
    currentTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
     Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
         Text(
             text = "Rooming",
             fontSize = if(currentTab == BottomTab.ROOMING) 20.sp else 18.sp,
             fontWeight = if(currentTab == BottomTab.ROOMING) FontWeight.Bold else FontWeight.Normal,
             color = if(currentTab == BottomTab.ROOMING) Color.White else Color.Gray,
             modifier = Modifier.clickable { onTabSelected(BottomTab.ROOMING) }
         )
         Text(
             text = "Hallway",
             fontSize = if(currentTab == BottomTab.EVERYONE) 20.sp else 18.sp,
             fontWeight = if(currentTab == BottomTab.EVERYONE) FontWeight.Bold else FontWeight.Normal,
             color = if(currentTab == BottomTab.EVERYONE) Color.White else Color.Gray,
             modifier = Modifier.clickable { onTabSelected(BottomTab.EVERYONE) }
         )
         Text(
             text = "Artist",
             fontSize = if(currentTab == BottomTab.ARTISTS) 20.sp else 18.sp,
             fontWeight = if(currentTab == BottomTab.ARTISTS) FontWeight.Bold else FontWeight.Normal,
             color = if(currentTab == BottomTab.ARTISTS) Color.White else Color.Gray,
             modifier = Modifier.clickable { /* Artist screen coming soon â€“ no-op */ }
         )
    }
}

@Composable
fun ExploreItem(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        color = Color.Gray,
        modifier = Modifier.clickable { }
    )
}

fun getRoomThemeColor(themeName: String): Color {
    return when(themeName) {
        "Ocean"     -> Color(0xFF00C6A2)
        "Sunset"    -> Color(0xFFE8820C)
        "Forest"    -> Color(0xFF22A84A)
        "Cyberpunk" -> Color(0xFFAA3FD6)
        else        -> Color(0xFF1A7AF8) // Default
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  SHARED CHROME: title bar + 5-dot progress + bottom buttons
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

val CardDarkBg = Color(0xFF1C1C1E)
val AccentBlue  = Color(0xFF1A7AF8)

@Composable
fun CreateRoomChrome(
    step: Int,           // 1-based, 1..4
    onDismiss: () -> Unit,
    onBack: (() -> Unit)?,  // null = hide Back button
    onNext: () -> Unit,
    nextLabel: String = "Next",
    nextEnabled: Boolean = true,
    themeColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        com.dmb.bestbefore.ui.components.AnimatedBackgroundView(baseColor = themeColor)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // â”€â”€ Header row â”€â”€
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Create Room",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF2C2C2E), androidx.compose.foundation.shape.CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // â”€â”€ 5-dot step indicator â”€â”€
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..5) {
                    val filled = i <= step
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (filled) AccentBlue else Color(0xFF3A3A3C),
                                androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    if (i < 5) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(
                                    if (i < step) AccentBlue else Color(0xFF3A3A3C)
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // â”€â”€ Body content â”€â”€
            Column(modifier = Modifier.weight(1f)) {
                content()
            }

            // â”€â”€ Bottom buttons â”€â”€
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = if (onBack != null) Arrangement.spacedBy(12.dp)
                                        else Arrangement.Center
            ) {
                if (onBack != null) {
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E))
                    ) {
                        Text("Back", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
                Button(
                    onClick = onNext,
                    enabled = nextEnabled,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (onBack != null) AccentBlue else Color(0xFF3A3A3C),
                        disabledContainerColor = Color(0xFF3A3A3C)
                    )
                ) {
                    Text(nextLabel, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  STEP 1 â€” What's the name? + Public/Private
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateRoomStep1(viewModel: ProfileViewModel) {
    val roomName by viewModel.roomName.collectAsState()
    val roomDescription by viewModel.roomDescription.collectAsState()
    val roomTags by viewModel.roomTags.collectAsState()
    
    val isPublic by viewModel.isPublic.collectAsState()
    val themeName by viewModel.roomAtmosphereTheme.collectAsState()

    var tagInput by remember { mutableStateOf("") }
    val suggestedTags = remember(tagInput) {
        if (tagInput.isBlank()) emptyList()
        else com.dmb.bestbefore.data.local.TagsDictionary.predefinedTags.filter { it.contains(tagInput, ignoreCase = true) && !roomTags.contains(it) }.take(5)
    }

    CreateRoomChrome(
        step = 1,
        onDismiss = { viewModel.closeOverlay() },
        onBack = null,
        onNext = {
            if (roomName.isNotBlank()) viewModel.goToStep(ProfileStep.ROOM_TIME_CAPSULE)
        },
        nextEnabled = roomName.isNotBlank(),
        themeColor = getRoomThemeColor(themeName)
    ) {
        Text(
            text = "What's the name?",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Give your room a unique title.",
            color = Color(0xFF8E8E93),
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Room Name field
        BasicTextField(
            value = roomName,
            onValueChange = { viewModel.updateRoomName(it) },
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            cursorBrush = SolidColor(Color.White),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .background(CardDarkBg, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 18.dp),
            decorationBox = { inner ->
                if (roomName.isEmpty()) Text("Room Name", color = Color(0xFF636366), fontSize = 16.sp)
                inner()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description field
        BasicTextField(
            value = roomDescription,
            onValueChange = { viewModel.updateRoomDescription(it) },
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .background(CardDarkBg, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 18.dp)
                .heightIn(min = 60.dp),
            decorationBox = { inner ->
                if (roomDescription.isEmpty()) Text("Description (Optional)", color = Color(0xFF636366), fontSize = 16.sp)
                inner()
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Tags Section
        Text("Tags", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        // Added Tags Display
        if (roomTags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                roomTags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .background(getRoomThemeColor(themeName).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .border(1.dp, getRoomThemeColor(themeName), RoundedCornerShape(16.dp))
                            .clickable { viewModel.removeRoomTag(tag) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = "#$tag", color = getRoomThemeColor(themeName), fontSize = 14.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Tag Input Field
        BasicTextField(
            value = tagInput,
            onValueChange = { tagInput = it },
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            cursorBrush = SolidColor(Color.White),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .background(CardDarkBg, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            decorationBox = { inner ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#", color = Color(0xFF636366), fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (tagInput.isEmpty()) Text("Add tag...", color = Color(0xFF636366), fontSize = 16.sp)
                        inner()
                    }
                    if (tagInput.isNotBlank()) {
                        Text(
                            text = "Add",
                            color = getRoomThemeColor(themeName),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                val newTag = tagInput.trim().lowercase().replace(" ", "-")
                                if (newTag.isNotEmpty()) {
                                    viewModel.addRoomTag(newTag)
                                    tagInput = ""
                                }
                            }
                        )
                    }
                }
            }
        )

        // Suggestion Row
        if (suggestedTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = suggestedTags,
                    key = { it }
                ) { suggestion ->
                    Text(
                        text = suggestion,
                        color = Color.LightGray,
                        modifier = Modifier
                            .background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.addRoomTag(suggestion)
                                tagInput = ""
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        Text("Privacy Status", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        // Public / Private cards
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            PrivacyCard(
                title = "Public",
                subtitle = "Anyone can see and join.",
                iconVector = Icons.Default.Language,
                selected = isPublic,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.updateRoomMode(true) }
            )
            PrivacyCard(
                title = "Private",
                subtitle = "Only visible to invited.",
                iconVector = Icons.Default.Lock,
                selected = !isPublic,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.updateRoomMode(false) }
            )
        }
    }
}

@Composable
fun PrivacyCard(
    title: String,
    subtitle: String,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(100.dp)
            .background(CardDarkBg, RoundedCornerShape(14.dp))
            .then(
                if (selected) Modifier.border(2.dp, AccentBlue, RoundedCornerShape(14.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            Icon(iconVector, null, tint = if (selected) Color.White else Color(0xFF8E8E93), modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = if (selected) Color.White else Color(0xFF8E8E93), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, color = Color(0xFF8E8E93), fontSize = 11.sp, lineHeight = 14.sp)
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  STEP 2 â€” Time Capsule? (Duration + Specific Date tabs)
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
fun CreateRoomStep2(viewModel: ProfileViewModel) {
    val enabled    by viewModel.isTimeCapsuleEnabled.collectAsState()
    val method     by viewModel.unlockMethod.collectAsState()
    val days       by viewModel.capsuleDays.collectAsState()
    val hours      by viewModel.capsuleHours.collectAsState()
    val mins       by viewModel.capsuleMins.collectAsState()
    val preset     by viewModel.selectedPreset.collectAsState()
    val targetTime by viewModel.targetTime.collectAsState()
    val targetHour by viewModel.targetHour.collectAsState()
    val targetMin  by viewModel.targetMinute.collectAsState()

    val themeName by viewModel.roomAtmosphereTheme.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current

    val calendarPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadCalendarEvents(context)
        } else {
            android.util.Log.d("ProfileScreen", "Calendar permission denied")
        }
    }

    LaunchedEffect(Unit) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CALENDAR
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.loadCalendarEvents(context)
        } else {
            calendarPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR)
        }
    }

    CreateRoomChrome(
        step = 2,
        onDismiss = { viewModel.closeOverlay() },
        onBack = { viewModel.goToStep(ProfileStep.ROOM_NAME) },
        onNext = { viewModel.goToStep(ProfileStep.ROOM_ATMOSPHERE) },
        themeColor = getRoomThemeColor(themeName)
    ) {
        androidx.compose.foundation.rememberScrollState().let { scroll ->
            Column(modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)) {

                Text("Time Capsule?", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Lock memories for a future date.", color = Color(0xFF8E8E93), fontSize = 14.sp)
                Spacer(modifier= Modifier.height(16.dp))

                // Enable toggle row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardDarkBg, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Time Capsule", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Content will be hidden until the timer ends.", color = Color(0xFF8E8E93), fontSize = 12.sp)
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { viewModel.updateTimeCapsuleEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentBlue
                        )
                    )
                }

                if (enabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    CalendarEventDropdown(viewModel)

                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Unlock Method", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Duration / Specific Date segmented control
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1C1C1E), RoundedCornerShape(10.dp))
                            .padding(4.dp)
                    ) {
                        listOf(UnlockMethod.DURATION to "Duration", UnlockMethod.SPECIFIC_DATE to "Specific Date").forEach { (m, label) ->
                            val sel = method == m
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (sel) Color(0xFF3A3A3C) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.updateUnlockMethod(m) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = Color.White, fontSize = 14.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (method == UnlockMethod.DURATION) {
                        // â”€â”€ Duration picker â”€â”€
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardDarkBg, RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            DurationStepper("Days",  days,  { viewModel.updateCapsuleDays(days - 1)  }, { viewModel.updateCapsuleDays(days + 1)  })
                            DurationStepper("Hours", hours, { viewModel.updateCapsuleHours(hours - 1) }, { viewModel.updateCapsuleHours(hours + 1) })
                            DurationStepper("Mins",  mins,  { viewModel.updateCapsuleMins(mins - 1)  }, { viewModel.updateCapsuleMins(mins + 1)  })
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "${days}d ${hours}h ${mins}m",
                            color = AccentBlue,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Preset chips
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("1 Week", "21 Days", "1 Month").forEach { p ->
                                val sel = preset == p
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (sel) AccentBlue else Color(0xFF2C2C2E),
                                            RoundedCornerShape(20.dp)
                                        )
                                        .clickable { viewModel.selectPreset(p) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(p, color = Color.White, fontSize = 14.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    } else {
                        // â”€â”€ Specific Date inline calendar â”€â”€
                        InlineCalendar(
                            selectedMillis = targetTime,
                            hour = targetHour,
                            minute = targetMin,
                            onDateSelected = { viewModel.updateTargetDate(it) },
                            onTimeChanged = { h, m -> viewModel.updateTargetTime(h, m) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun CalendarEventDropdown(viewModel: ProfileViewModel) {
    val events by viewModel.calendarEvents.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Link Calendar Event", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardDarkBg, RoundedCornerShape(14.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Event, contentDescription = "Event", tint = AccentBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                if (events.isEmpty()) {
                    Text("No upcoming event", color = Color(0xFF8E8E93), fontSize = 15.sp)
                } else {
                    Text("Select upcoming event...", color = Color.White, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Drop Down", tint = Color.White)
            }
            
            if (events.isNotEmpty()) {
                androidx.compose.material3.DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(CardDarkBg).fillMaxWidth(0.85f)
                ) {
                    val dateFormat = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                    events.forEach { event ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(event.title, color = Color.White, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    Text(dateFormat.format(event.startTime), color = Color(0xFF8E8E93), fontSize = 12.sp)
                                }
                            },
                            onClick = {
                                viewModel.applyCalendarEvent(event)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DurationStepper(
    label: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Color(0xFF8E8E93), fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Color(0xFF3A3A3C), androidx.compose.foundation.shape.CircleShape)
                    .clickable { onDecrement() },
                contentAlignment = Alignment.Center
            ) { Text("âˆ’", color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center) }
            Text(value.toString(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Color(0xFF3A3A3C), androidx.compose.foundation.shape.CircleShape)
                    .clickable { onIncrement() },
                contentAlignment = Alignment.Center
            ) { Text("+", color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center) }
        }
    }
}

@Composable
fun InlineCalendar(
    selectedMillis: Long,
    hour: Int,
    minute: Int,
    onDateSelected: (Long) -> Unit,
    onTimeChanged: (Int, Int) -> Unit
) {
    val todayMillis = remember {
        val c = java.util.Calendar.getInstance()
        c.set(java.util.Calendar.HOUR_OF_DAY, 0)
        c.set(java.util.Calendar.MINUTE, 0)
        c.set(java.util.Calendar.SECOND, 0)
        c.set(java.util.Calendar.MILLISECOND, 0)
        c.timeInMillis
    }

    var displayMonthMillis by remember { mutableStateOf(
        run {
            val c = java.util.Calendar.getInstance()
            c.set(java.util.Calendar.DAY_OF_MONTH, 1)
            c.set(java.util.Calendar.HOUR_OF_DAY, 0); c.set(java.util.Calendar.MINUTE, 0)
            c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0)
            c.timeInMillis
        }
    ) }

    val monthCal = remember(displayMonthMillis) {
        java.util.Calendar.getInstance().apply { timeInMillis = displayMonthMillis }
    }

    val monthName = remember(displayMonthMillis) {
        java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(displayMonthMillis))
    }

    val selectedDayMillis = remember(selectedMillis) {
        val c = java.util.Calendar.getInstance(); c.timeInMillis = selectedMillis
        c.set(java.util.Calendar.HOUR_OF_DAY, 0); c.set(java.util.Calendar.MINUTE, 0)
        c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0)
        c.timeInMillis
    }

    // Build day grid
    val daysInMonth = monthCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = monthCal.get(java.util.Calendar.DAY_OF_WEEK) // 1=Sun..7=Sat
    // Convert to Mon-based offset: Mon=0..Sun=6
    val startOffset = (firstDayOfWeek - 2 + 7) % 7

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardDarkBg, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            // Month navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(monthName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ChevronRight, null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                }
                Row {
                    IconButton(onClick = {
                        val c = java.util.Calendar.getInstance(); c.timeInMillis = displayMonthMillis
                        c.add(java.util.Calendar.MONTH, -1); displayMonthMillis = c.timeInMillis
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ChevronLeft, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = {
                        val c = java.util.Calendar.getInstance(); c.timeInMillis = displayMonthMillis
                        c.add(java.util.Calendar.MONTH, 1); displayMonthMillis = c.timeInMillis
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ChevronRight, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Day-of-week headers (Mon..Sun)
            val dayHeaders = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
            Row(modifier = Modifier.fillMaxWidth()) {
                dayHeaders.forEach { header ->
                    Text(header, color = Color(0xFF8E8E93), fontSize = 11.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Day cells
            val totalCells = startOffset + daysInMonth
            val rows = (totalCells + 6) / 7
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNum = cellIndex - startOffset + 1
                        val valid = dayNum in 1..daysInMonth
                        if (valid) {
                            val dayCal = java.util.Calendar.getInstance().apply {
                                timeInMillis = displayMonthMillis
                                set(java.util.Calendar.DAY_OF_MONTH, dayNum)
                            }
                            val dayMillis = run {
                                val c2 = dayCal.clone() as java.util.Calendar
                                c2.set(java.util.Calendar.HOUR_OF_DAY, 0); c2.set(java.util.Calendar.MINUTE, 0)
                                c2.set(java.util.Calendar.SECOND, 0); c2.set(java.util.Calendar.MILLISECOND, 0)
                                c2.timeInMillis
                            }
                            val isSelected = dayMillis == selectedDayMillis
                            val isToday = dayMillis == todayMillis
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .then(
                                        if (isSelected) Modifier.background(AccentBlue, androidx.compose.foundation.shape.CircleShape)
                                        else Modifier
                                    )
                                    .clickable { onDateSelected(dayCal.timeInMillis) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    color = when {
                                        isSelected -> Color.White
                                        isToday -> AccentBlue
                                        else -> Color.White
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // â”€â”€ Time picker â€” premium drum style â”€â”€
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2C2C2E), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // â”€â”€â”€ Hour drum â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(72.dp)
                    ) {
                        Icon(
                            Icons.Default.ExpandLess,
                            contentDescription = "Hour up",
                            tint = AccentBlue,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { onTimeChanged((hour + 1) % 24, minute) }
                        )
                        Text(
                            text = String.format("%02d", hour),
                            color = Color.White,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text("HH", color = Color(0xFF8E8E93), fontSize = 11.sp)
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = "Hour down",
                            tint = AccentBlue,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { onTimeChanged((hour - 1 + 24) % 24, minute) }
                        )
                    }

                    // â”€â”€â”€ Colon separator â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    Text(
                        text = ":",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 20.dp)
                    )

                    // â”€â”€â”€ Minute drum â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(72.dp)
                    ) {
                        Icon(
                            Icons.Default.ExpandLess,
                            contentDescription = "Minute up",
                            tint = AccentBlue,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { onTimeChanged(hour, (minute + 5) % 60) }
                        )
                        Text(
                            text = String.format("%02d", minute),
                            color = Color.White,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text("MM", color = Color(0xFF8E8E93), fontSize = 11.sp)
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = "Minute down",
                            tint = AccentBlue,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { onTimeChanged(hour, (minute - 5 + 60) % 60) }
                        )
                    }
                }
            }
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  STEP 3 â€” Atmosphere (Room Theme + Background Music)
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
fun CreateRoomStep3Atmosphere(viewModel: ProfileViewModel) {
    val selectedTheme by viewModel.roomAtmosphereTheme.collectAsState()
    val selectedMusic by viewModel.selectedMusic.collectAsState()

    // Theme data: name â†’ background color
    val themes = listOf(
        "Default"   to Color(0xFF1A7AF8),
        "Ocean"     to Color(0xFF00C6A2),
        "Sunset"    to Color(0xFFE8820C),
        "Forest"    to Color(0xFF22A84A),
        "Cyberpunk" to Color(0xFFAA3FD6)
    )

    // Music options: icon emoji + label
    data class MusicOption(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String)
    val musicOptions = listOf(
        MusicOption(Icons.AutoMirrored.Filled.VolumeOff,   "None"),
        MusicOption(Icons.Default.MusicNote,   "Lofi Beats"),
        MusicOption(Icons.Default.Star,        "Nature Ambience"),
        MusicOption(Icons.Default.Favorite,    "Minimal Piano")
    )

    CreateRoomChrome(
        step = 3,
        onDismiss = { viewModel.closeOverlay() },
        onBack = { viewModel.goToStep(ProfileStep.ROOM_TIME_CAPSULE) },
        onNext = { viewModel.goToStep(ProfileStep.ROOM_MEMORY_RULES) },
        themeColor = getRoomThemeColor(selectedTheme)
    ) {
        Text("Atmosphere", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Set the mood with background music.", color = Color(0xFF8E8E93), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))

        // â”€â”€ Room Theme â”€â”€
        Text("Room Theme", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            themes.forEach { (name, color) ->
                val isSelected = selectedTheme == name
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.updateSelectedTheme(name) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .then(
                                if (isSelected)
                                    Modifier.border(2.5.dp, Color.White, androidx.compose.foundation.shape.CircleShape)
                                else Modifier
                            )
                            .padding(if (isSelected) 3.dp else 0.dp)
                            .background(color, androidx.compose.foundation.shape.CircleShape)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = name,
                        color = if (isSelected) Color.White else Color(0xFF8E8E93),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // â”€â”€ Background Music â”€â”€
        Text("Background Music", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            musicOptions.forEach { option ->
                val isSelected = selectedMusic == option.label
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) Color(0xFF0D2B5E) else CardDarkBg,
                            RoundedCornerShape(14.dp)
                        )
                        .then(
                            if (isSelected)
                                Modifier.border(1.5.dp, AccentBlue, RoundedCornerShape(14.dp))
                            else Modifier
                        )
                        .clickable { viewModel.updateSelectedMusic(option.label) }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = option.label,
                            tint = if (isSelected) AccentBlue else Color(0xFF8E8E93),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = option.label,
                            color = if (isSelected) Color.White else Color(0xFF8E8E93),
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  STEP 4 â€” Memory Dump Rules
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
fun CreateRoomStep4(viewModel: ProfileViewModel) {
    val rolling  by viewModel.rollingExpiration.collectAsState()
    val closure  by viewModel.scheduledClosureEnabled.collectAsState()
    val closureTime  by viewModel.scheduledClosureTime.collectAsState()
    val closureHour  by viewModel.scheduledClosureHour.collectAsState()
    val closureMin   by viewModel.scheduledClosureMinute.collectAsState()
    val themeName by viewModel.roomAtmosphereTheme.collectAsState()

    CreateRoomChrome(
        step = 4,
        onDismiss = { viewModel.closeOverlay() },
        onBack = { viewModel.goToStep(ProfileStep.ROOM_ATMOSPHERE) },
        onNext = { viewModel.goToStep(ProfileStep.ROOM_INVITE) },
        themeColor = getRoomThemeColor(themeName)
    ) {
        androidx.compose.foundation.rememberScrollState().let { scroll ->
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(scroll)) {

                Text("Memory Dump Rules", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Configure auto-archival for drops.", color = Color(0xFF8E8E93), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(20.dp))

                // Rolling Expiration card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardDarkBg, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Text("Rolling Expiration (Snapchat Mode)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Automatically archive memories X days after they are posted.",
                        color = Color(0xFF8E8E93), fontSize = 12.sp, lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2C2C2E), RoundedCornerShape(22.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Never", "1 Day (24...)", "7 Days", "30 Days").forEach { option ->
                            val sel = rolling == option
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (sel) Color.White else Color.Transparent,
                                        RoundedCornerShape(18.dp)
                                    )
                                    .clickable { viewModel.updateRollingExpiration(option) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    option,
                                    color = if (sel) Color.Black else Color(0xFF8E8E93),
                                    fontSize = 12.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scheduled Room Closure card  â€” expands to show date picker when toggled ON
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardDarkBg, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Scheduled Room Closure", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "Lock the entire room into a read-only archive after a specific date.",
                                color = Color(0xFF8E8E93), fontSize = 12.sp, lineHeight = 16.sp
                            )
                        }
                        Switch(
                            checked = closure,
                            onCheckedChange = { viewModel.updateScheduledClosure(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentBlue
                            )
                        )
                    }

                    if (closure) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Close on:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        InlineCalendar(
                            selectedMillis = closureTime,
                            hour = closureHour,
                            minute = closureMin,
                            onDateSelected = { viewModel.updateScheduledClosureTime(it) },
                            onTimeChanged = { h, m ->
                                viewModel.updateScheduledClosureHour(h)
                                viewModel.updateScheduledClosureMinute(m)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  STEP 5 â€” Invite Friends
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
fun CreateRoomStep5(viewModel: ProfileViewModel) {
    val emails by viewModel.inviteEmails.collectAsState()
    var emailInput by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    val themeName by viewModel.roomAtmosphereTheme.collectAsState()

    CreateRoomChrome(
        step = 5,
        onDismiss = { viewModel.closeOverlay() },
        onBack = { viewModel.goToStep(ProfileStep.ROOM_MEMORY_RULES) },
        onNext = { viewModel.finalizeRoom(context) },
        nextLabel = "Create Room",
        themeColor = getRoomThemeColor(themeName)
    ) {
        Text("Invite Friends", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Add people and assign them roles (Viewer or Contributor).", color = Color(0xFF8E8E93), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(20.dp))

        // Email input + add button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BasicTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                cursorBrush = SolidColor(Color.White),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .background(CardDarkBg, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                decorationBox = { inner ->
                    if (emailInput.isEmpty()) Text("Friend's Email Address", color = Color(0xFF636366), fontSize = 15.sp)
                    inner()
                }
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF3A3A3C), androidx.compose.foundation.shape.CircleShape)
                    .clickable {
                        if (emailInput.isNotBlank()) {
                            viewModel.addInviteEmail(emailInput.trim())
                            emailInput = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        // Added emails list
        if (emails.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                emails.forEach { email ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardDarkBg, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, null, tint = Color(0xFF8E8E93), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(email, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Default.Close, null, tint = Color(0xFF8E8E93), modifier = Modifier
                                .size(18.dp)
                                .clickable { viewModel.removeInviteEmail(email) }
                        )
                    }
                }
            }
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  LEGACY HELPERS (kept so old OverlayStepContainer callers compile)
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
fun RoomNameStep(viewModel: ProfileViewModel) {
    CreateRoomStep1(viewModel)
}
@Composable
fun RoomTimeStep(viewModel: ProfileViewModel) {
    CreateRoomStep2(viewModel)
}
@Composable
fun RoomModeStep(viewModel: ProfileViewModel) {
    CreateRoomStep1(viewModel)
}
// Kept as alias for legacy callers
@Composable
fun CreateRoomStep3(viewModel: ProfileViewModel) {
    CreateRoomStep4(viewModel)
}
@Composable
fun CreateRoomStep4Alias(viewModel: ProfileViewModel) {
    CreateRoomStep5(viewModel)
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        text = content
    )
}


@Composable
fun TimeCapsuleListScreen(
    viewModel: ProfileViewModel,
    createdRooms: List<TimeCapsuleRoom>
) {
    val showOnlySaved by viewModel.showOnlySaved.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
         Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
             Row(verticalAlignment = Alignment.CenterVertically) {
                 IconButton(onClick = { viewModel.closeOverlay() }) {
                     Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                 }
                 Text(
                     if(showOnlySaved) "Saved Rooms" else "My Time Capsules", 
                     color = Color.White, 
                     fontSize = 20.sp, 
                     fontWeight = FontWeight.Bold
                 )
             }
             
             LazyColumn(
                 contentPadding = PaddingValues(vertical = 16.dp),
                 verticalArrangement = Arrangement.spacedBy(16.dp)
             ) {
                 items(createdRooms) { room ->
                     Card(
                        colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
                        modifier = Modifier.fillMaxWidth().height(80.dp).clickable { viewModel.selectRoom(room) }
                     ) {
                         Row(
                             modifier = Modifier.fillMaxSize().padding(16.dp),
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             Column {
                                 Text(room.roomName, color = Color.White, fontWeight = FontWeight.Bold)
                                 Text("Unlocks: ${java.text.SimpleDateFormat("MMM dd HH:mm").format(java.util.Date(room.unlockTime))}", color = Color.Gray, fontSize = 12.sp)
                             }
                         }
                     }
                 }
             }
         }
    }
}



@Composable
fun ModeOption(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) Color.White else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (selected) Color.White else Color.Gray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            color = if (selected) Color.White else Color.Gray,
            fontSize = 16.sp
        )
    }
}


@Composable
fun OverlayStepContainer(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onNext: () -> Unit,
    nextLabel: String = "Next",
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            TextButton(onClick = onNext) {
                Text(nextLabel, color = Color.White, fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = title,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = subtitle,
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        content()
    }
}
