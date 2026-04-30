package com.dmb.bestbefore.ui.screens.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmb.bestbefore.data.models.AppNotification
import com.dmb.bestbefore.data.models.NotificationType
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

private val AccentBlue = Color(0xFF1A7AF8)
private val AccentGreen = Color(0xFF30D158)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoom: (String) -> Unit = {},
    viewModel: NotificationViewModel = viewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    androidx.activity.compose.BackHandler {
        onNavigateBack()
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notifications",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearAll() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Delete All",
                                tint = Color.Red
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { padding ->
        var isRefreshing by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    viewModel.refresh()
                    kotlinx.coroutines.delay(500)
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No new notifications", color = Color.Gray, fontSize = 16.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                ) {
                    items(
                        items = notifications,
                        key = { it.id }
                    ) { notification ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.removeNotification(notification.id)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val color = when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.EndToStart -> Color.Red
                                    else -> Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Swipe to delete",
                                        tint = Color.White
                                    )
                                }
                            }
                        ) {
                            CollapsibleNotificationCard(
                                notification = notification,
                                onDelete = { viewModel.removeNotification(notification.id) },
                                onNavigateToRoom = onNavigateToRoom,
                                onAccept = { n -> n.relatedRoomId?.let { viewModel.acceptInvite(it, n.id) } },
                                onDecline = { n -> n.relatedRoomId?.let { viewModel.declineInvite(it, n.id) } },
                                onApproveJoin = { n ->
                                    val roomId = n.relatedRoomId
                                    val requester = n.requesterEmail
                                    if (roomId.isNullOrBlank() || requester.isNullOrBlank()) {
                                        Toast.makeText(context, "Cannot process: notification data incomplete. Pull down to refresh.", Toast.LENGTH_LONG).show()
                                        return@CollapsibleNotificationCard
                                    }
                                    viewModel.approveJoinRequest(roomId, requester, n.id)
                                },
                                onDenyJoin = { n ->
                                    val roomId = n.relatedRoomId
                                    val requester = n.requesterEmail
                                    if (roomId.isNullOrBlank() || requester.isNullOrBlank()) {
                                        Toast.makeText(context, "Cannot process: notification data incomplete. Pull down to refresh.", Toast.LENGTH_LONG).show()
                                        return@CollapsibleNotificationCard
                                    }
                                    viewModel.denyJoinRequest(roomId, requester, n.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CollapsibleNotificationCard(
    notification: AppNotification,
    onDelete: () -> Unit,
    onNavigateToRoom: (String) -> Unit,
    onAccept: (AppNotification) -> Unit = {},
    onDecline: (AppNotification) -> Unit = {},
    onApproveJoin: (AppNotification) -> Unit = {},
    onDenyJoin: (AppNotification) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val sdf = remember {
        java.text.SimpleDateFormat("MMM dd 'at' hh:mm a", java.util.Locale.getDefault())
    }
    val dateString = sdf.format(java.util.Date(notification.timestamp))

    // Join requests start expanded so the owner sees the actions immediately
    LaunchedEffect(notification.id) {
        if (notification.type == NotificationType.JOIN_REQUEST) {
            expanded = true
        }
    }

    // Accent color for the card left border based on type
    val accentColor = when (notification.type) {
        NotificationType.JOIN_REQUEST -> AccentGreen
        NotificationType.INVITATION -> AccentBlue
        else -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp))
    ) {
        // ── Colored top stripe for JOIN_REQUEST / INVITATION ───────────────────
        if (accentColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(accentColor, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            )
        }

        // ── Header row ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    expanded = !expanded
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Type icon
            if (notification.type == NotificationType.JOIN_REQUEST) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 0.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            // Title + timestamp stacked on the left; they get all remaining space
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateString,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }

            // Fixed-width icon cluster on the right
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Trash icon
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete notification",
                        tint = Color(0xFFFF453A),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Collapse/expand chevron
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // ── Expandable body ────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow)),
            exit = shrinkVertically(animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            ) {
                HorizontalDivider(color = Color(0xFF3A3A3C), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = notification.message,
                    color = Color(0xFFAAAAAA),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                // ── INVITATION: invitee-side Accept / Decline ──────────────────
                if (notification.type == NotificationType.INVITATION && notification.relatedRoomId != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onDecline(notification) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                        ) {
                            Text("Decline")
                        }
                        Button(
                            onClick = { onAccept(notification) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Accept", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ── JOIN_REQUEST: owner-side Approve / Deny ────────────────────
                if (notification.type == NotificationType.JOIN_REQUEST && notification.relatedRoomId != null) {
                    val requesterName = notification.requesterEmail
                        ?.substringBefore("@")
                        ?: "Someone"
                    val roomName = notification.relatedRoomName ?: "this room"

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "\"$requesterName\" wants to join \"$roomName\"",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onDenyJoin(notification) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF453A)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF453A))
                        ) {
                            Text("Deny", fontWeight = FontWeight.Medium)
                        }
                        Button(
                            onClick = { onApproveJoin(notification) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGreen,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Approve", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
