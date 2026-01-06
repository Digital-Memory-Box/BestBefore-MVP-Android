package com.ozang.bestbefore_mvp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ozang.bestbefore_mvp.ui.screens.hallway.HallwayScreen
import com.ozang.bestbefore_mvp.ui.screens.login.LoginScreen
import com.ozang.bestbefore_mvp.ui.screens.opening.OpeningScreen
import com.ozang.bestbefore_mvp.ui.screens.profile.ProfileScreen
import com.ozang.bestbefore_mvp.ui.screens.room.RoomScreen
import com.ozang.bestbefore_mvp.ui.screens.signup.SignupScreen

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
                    navController.navigate(Routes.HALLWAY) {
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
                    navController.navigate(Routes.HALLWAY) {
                        popUpTo(Routes.SIGNUP) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HALLWAY) {
            HallwayScreen(
                onNavigateToProfile = {
                    navController.navigate(Routes.PROFILE)
                }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
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