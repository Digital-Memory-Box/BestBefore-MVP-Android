package com.dmb.bestbefore.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dmb.bestbefore.ui.screens.hallway.HallwayScreen
import com.dmb.bestbefore.ui.screens.login.LoginScreen
import com.dmb.bestbefore.ui.screens.opening.OpeningScreen
import com.dmb.bestbefore.ui.screens.profile.ProfileScreen
import com.dmb.bestbefore.ui.screens.room.RoomScreen
import com.dmb.bestbefore.ui.screens.signup.SignupScreen

object Routes {
    const val OPENING = "opening"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val HALLWAY = "hallway"
    const val PROFILE = "profile"
    const val ROOM = "room/{roomId}/{roomName}"
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
            
            // Check for potential auto-login
            LaunchedEffect(Unit) {
                val sessionManager = com.dmb.bestbefore.data.local.SessionManager(context)
                if (sessionManager.isLoggedIn()) {
                    // Auto-login active: skip to profile
                    navController.navigate(Routes.PROFILE) {
                        popUpTo(Routes.OPENING) { inclusive = true }
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
                onNavigateToRoom = { _, _ ->
                    // Navigate to PROFILE as the main screen (which contains Hallway)
                    navController.navigate(Routes.PROFILE) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SIGNUP) {
            SignupScreen(
                onNavigateBack = { _ ->
                    navController.popBackStack()
                },
                onSignupSuccess = { _ ->
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
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
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