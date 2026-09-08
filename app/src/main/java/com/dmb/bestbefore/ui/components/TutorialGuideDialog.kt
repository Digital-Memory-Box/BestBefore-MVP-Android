package com.dmb.bestbefore.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dmb.bestbefore.R
import kotlinx.coroutines.launch

@Composable
fun TutorialGuideDialog(
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .padding(horizontal = 20.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.06f))
                            )
                        ),
                        RoundedCornerShape(28.dp)
                    ),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF141416)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── Pager for the 4 tutorial slides ──────────────────────
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) { page ->
                        TutorialSlideContent(page = page)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Bottom Action Row: Skip (Left) | Dots (Center) | Next/Done (Right) ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Bottom: Skip Button
                        TextButton(
                            onClick = onDismiss,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.tutorial_skip),
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Center: Animated Pager Dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(4) { index ->
                                val isSelected = pagerState.currentPage == index
                                val width by animateDpAsState(
                                    targetValue = if (isSelected) 22.dp else 6.dp,
                                    label = "pager_dot_width"
                                )
                                val dotColor by animateColorAsState(
                                    targetValue = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.22f),
                                    label = "pager_dot_color"
                                )
                                Box(
                                    modifier = Modifier
                                        .height(6.dp)
                                        .width(width)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                            }
                        }

                        // Right Bottom: Next or Get Started Button
                        Button(
                            onClick = {
                                if (pagerState.currentPage < 3) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                } else {
                                    onDismiss()
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = if (pagerState.currentPage == 3) {
                                    stringResource(R.string.tutorial_get_started)
                                } else {
                                    stringResource(R.string.tutorial_next)
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorialSlideContent(page: Int) {
    val badge: String
    val title: String
    val desc: String

    when (page) {
        0 -> {
            badge = stringResource(R.string.tutorial_step1_badge)
            title = stringResource(R.string.tutorial_step1_title)
            desc = stringResource(R.string.tutorial_step1_desc)
        }
        1 -> {
            badge = stringResource(R.string.tutorial_step2_badge)
            title = stringResource(R.string.tutorial_step2_title)
            desc = stringResource(R.string.tutorial_step2_desc)
        }
        2 -> {
            badge = stringResource(R.string.tutorial_step3_badge)
            title = stringResource(R.string.tutorial_step3_title)
            desc = stringResource(R.string.tutorial_step3_desc)
        }
        else -> {
            badge = stringResource(R.string.tutorial_step4_badge)
            title = stringResource(R.string.tutorial_step4_title)
            desc = stringResource(R.string.tutorial_step4_desc)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Screenshot / UI illustration Mockup
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0C0C0E))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            when (page) {
                0 -> HallwayIllustration()
                1 -> ArtistsIllustration()
                2 -> RoomingIllustration()
                3 -> CreateCapsuleIllustration()
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Step category badge
        Box(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = badge,
                color = Color(0xFF00E5FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Title
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Description
        Text(
            text = desc,
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

// ── Slide 1: Hallway Carousel Illustration ────────────────────────────────────
@Composable
private fun HallwayIllustration() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.15f), Color.Transparent)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Mockup card
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(160.dp)
                .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF004D40).copy(alpha = 0.6f), Color(0xFF101014))
                    )
                )
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Summer '26", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                    Text("Alex Rivera", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                }
            }
        }

        // Left and Right peek indicators
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 10.dp)
                .size(32.dp, 80.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.05f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-10).dp)
                .size(32.dp, 80.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.05f))
        )
    }
}

// ── Slide 2: Artists Illustration ─────────────────────────────────────────────
@Composable
private fun ArtistsIllustration() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFE040FB).copy(alpha = 0.15f), Color.Transparent)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Artist Avatar with vibrant border
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .border(2.dp, Brush.linearGradient(listOf(Color(0xFFE040FB), Color(0xFF7C4DFF))), CircleShape)
                    .background(Color(0xFF251336)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = null,
                    tint = Color(0xFFE040FB),
                    modifier = Modifier.size(26.dp)
                )
            }

            Text("Elena Vance", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)

            Box(
                modifier = Modifier
                    .width(190.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFFE040FB).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E1428))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Neon Horizons", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Curated Space", color = Color(0xFFE040FB), fontSize = 9.sp)
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

// ── Slide 3: Rooming & QR Illustration ─────────────────────────────────────────
@Composable
private fun RoomingIllustration() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF2979FF).copy(alpha = 0.15f), Color.Transparent)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // QR Scan Quick Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2979FF), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Saved", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Collaborated", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Text("Scan", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Saved room card mockup
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF2979FF).copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .background(Color(0xFF111827))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("High School Reunion", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Unlocked • 18 Memories", color = Color.Gray, fontSize = 10.sp)
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF007AFF).copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("Saved", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Slide 4: Create & Seal Capsule Illustration ───────────────────────────────
@Composable
private fun CreateCapsuleIllustration() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFFF9100).copy(alpha = 0.15f), Color.Transparent)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Capsule creation summary card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFFFF9100).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E1710))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Lock, null, tint = Color(0xFFFF9100), modifier = Modifier.size(14.dp))
                        Text("Sealed Time Capsule", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Unlock Date: Dec 31, 2026", color = Color(0xFFFFB74D), fontSize = 9.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.PhotoCamera, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                        Icon(Icons.Default.Mic, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                        Icon(Icons.Default.GraphicEq, null, tint = Color(0xFFFF9100), modifier = Modifier.size(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Orb Menu (+) Action button simulation
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFFF9100), Color(0xFFE65100))
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
    }
}
