package com.dmb.bestbefore.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmb.bestbefore.ui.screens.login.LoginScreen
import com.dmb.bestbefore.ui.screens.opening.OpeningScreen
import com.dmb.bestbefore.ui.screens.profile.ProfileScreen
import com.dmb.bestbefore.ui.screens.room.RoomScreen
import com.dmb.bestbefore.ui.screens.signup.SignupScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.dmb.bestbefore.ui.screens.profile.RoomDetailScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier

object Routes {
    const val OPENING = "opening"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val HALLWAY = "hallway"
    const val PROFILE = "profile"
    const val ROOM = "room/{roomId}/{roomName}"
    const val NOTIFICATIONS = "notifications"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.OPENING
    ) {
        composable(Routes.OPENING) {
            val context = androidx.compose.ui.platform.LocalContext.current
            
            // Auto-login: only if Firebase user exists, email is verified, AND backend sync completed
            LaunchedEffect(Unit) {
                val sessionManager = com.dmb.bestbefore.data.local.SessionManager(context)
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val hasBackendSession = sessionManager.getUserId() != null

                when {
                    firebaseUser == null -> {
                        // No Firebase session — stay on opening screen
                    }
                    !firebaseUser.isEmailVerified -> {
                        // Firebase account exists but email not verified — clear & go to login
                        sessionManager.clearSession()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.OPENING) { inclusive = true }
                        }
                    }
                    hasBackendSession -> {
                        // Fully authenticated — auto-login
                        navController.navigate(Routes.PROFILE) {
                            popUpTo(Routes.OPENING) { inclusive = true }
                        }
                    }
                    else -> {
                        // Firebase verified but no backend session — clear & go to login
                        sessionManager.clearSession()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.OPENING) { inclusive = true }
                        }
                    }
                }
            }

            OpeningScreen(
                onNavigateToMain = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.OPENING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToSignup = {
                    navController.navigate(Routes.SIGNUP)
                },
                onLoginSuccess = {
                    // Navigate to PROFILE as the main screen (which contains Hallway)
                    navController.navigate(Routes.PROFILE) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SIGNUP) {
            SignupScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSignupSuccess = {
                    navController.navigate(Routes.PROFILE) {
                        popUpTo(Routes.SIGNUP) { inclusive = true }
                    }
                }
            )
        }

        // Hallway routed to ProfileScreen logic now
        composable(Routes.HALLWAY) {
             // Redirect legacy route to profile in case used
             LaunchedEffect(Unit) {
                 navController.navigate(Routes.PROFILE) {
                     popUpTo(Routes.HALLWAY) { inclusive = true }
                 }
             }
        }

        composable(Routes.PROFILE) {
            val profileViewModel: com.dmb.bestbefore.ui.screens.profile.ProfileViewModel = viewModel()
            val hallwayViewModel: com.dmb.bestbefore.ui.screens.hallway.HallwayViewModel = viewModel()
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToNotifications = {
                    navController.navigate(Routes.NOTIFICATIONS)
                },
                onNavigateToCreatorProfile = { userId ->
                    navController.navigate("creator_profile/$userId")
                },
                viewModel = profileViewModel,
                hallwayViewModel = hallwayViewModel
            )
        }

        composable("creator_profile/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            com.dmb.bestbefore.ui.screens.profile.CreatorProfileScreen(
                userId = userId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRoom = { roomId -> navController.navigate("room_detail/$roomId") }
            )
        }

        composable(Routes.NOTIFICATIONS) {
            com.dmb.bestbefore.ui.screens.notifications.NotificationScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRoom = { roomId ->
                    navController.navigate("room_detail/$roomId")
                }
            )
        }

        composable("room_detail/{roomId}") { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            val profileViewModel: com.dmb.bestbefore.ui.screens.profile.ProfileViewModel = viewModel()
            val context = androidx.compose.ui.platform.LocalContext.current
            
            val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetMultipleContents()
            ) { uris ->
                if (uris.isNotEmpty()) {
                    profileViewModel.updateSelectedMedia(uris)
                    profileViewModel.uploadMedia(context)
                }
            }

            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                uri?.let {
                    profileViewModel.updateSelectedMedia(listOf(it))
                    profileViewModel.uploadMedia(context)
                }
            }

            LaunchedEffect(roomId) {
                profileViewModel.handleDeepLink(roomId)
            }
            
            Box(Modifier.fillMaxSize()) {
                RoomDetailScreen(
                    viewModel = profileViewModel,
                    multiplePhotoPickerLauncher = multiplePhotoPickerLauncher,
                    filePickerLauncher = filePickerLauncher,
                    isRoomInRooming = false
                )
                
                // Override the internal goBack from ProfileViewModel to pop back stack directly
                val room by profileViewModel.selectedRoom.collectAsState()
                androidx.activity.compose.BackHandler(enabled = true) {
                    navController.popBackStack()
                }
                
                // Also listen to profileViewModel closing its own overlay
                LaunchedEffect(room) {
                    // if room becomes null after it was set, it means viewModel.goBack() was called
                    // or room was deleted
                    if (room == null && profileViewModel.isRefreshing.value == false) {
                        navController.popBackStack()
                    }
                }
            }
        }

        composable(Routes.ROOM) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            val roomName = backStackEntry.arguments?.getString("roomName") ?: ""
            RoomScreen(
                roomId = roomId,
                roomName = roomName,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}