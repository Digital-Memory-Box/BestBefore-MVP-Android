package com.dmb.bestbefore.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
                .background(Color.Black.copy(alpha = 0.82f))
                .padding(horizontal = 16.dp, vertical = 24.dp),
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
                                listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.08f))
                            )
                        ),
                        RoundedCornerShape(28.dp)
                    ),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF131316)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── Pager for the 4 tutorial slides ──────────────────────
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        beyondViewportPageCount = 1
                    ) { page ->
                        TutorialSlideContent(page = page)
                    }

                    Spacer(modifier = Modifier.height(22.dp))

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
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.tutorial_skip),
                                color = Color.White.copy(alpha = 0.60f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Center: Animated Pager Dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(4) { index ->
                                val isSelected = pagerState.currentPage == index
                                val width by animateDpAsState(
                                    targetValue = if (isSelected) 24.dp else 7.dp,
                                    label = "pager_dot_width"
                                )
                                val dotColor by animateColorAsState(
                                    targetValue = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.25f),
                                    label = "pager_dot_color"
                                )
                                Box(
                                    modifier = Modifier
                                        .height(7.dp)
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
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
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
    val imageRes: Int

    when (page) {
        0 -> {
            badge = stringResource(R.string.tutorial_step1_badge)
            title = stringResource(R.string.tutorial_step1_title)
            desc = stringResource(R.string.tutorial_step1_desc)
            imageRes = R.drawable.tutorial_screen_hallway
        }
        1 -> {
            badge = stringResource(R.string.tutorial_step2_badge)
            title = stringResource(R.string.tutorial_step2_title)
            desc = stringResource(R.string.tutorial_step2_desc)
            imageRes = R.drawable.tutorial_screen_artists
        }
        2 -> {
            badge = stringResource(R.string.tutorial_step3_badge)
            title = stringResource(R.string.tutorial_step3_title)
            desc = stringResource(R.string.tutorial_step3_desc)
            imageRes = R.drawable.tutorial_screen_rooming
        }
        else -> {
            badge = stringResource(R.string.tutorial_step4_badge)
            title = stringResource(R.string.tutorial_step4_title)
            desc = stringResource(R.string.tutorial_step4_desc)
            imageRes = R.drawable.tutorial_screen_capsule
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Actual App Screenshot Preview Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0A0A0C))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Step category badge
        Box(
            modifier = Modifier
                .background(Color(0xFF00E5FF).copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = badge,
                color = Color(0xFF00E5FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Enlarged Title
        Text(
            text = title,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Enlarged and Clear Description
        Text(
            text = desc,
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}

