package com.dmb.bestbefore.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

enum class InterfaceTheme {
    DEFAULT, GLASS, MIDNIGHT, VIBRANT
}

data class AppThemeState(
    val currentTheme: InterfaceTheme = InterfaceTheme.DEFAULT,
    val globalAccentColor: Color = Color(0xFF007AFF),
    val applyAccentToAll: Boolean = false,
    val syncWithRoomThemes: Boolean = false
)

val LocalAppTheme = compositionLocalOf { AppThemeState() }
val LocalThemeUpdater = compositionLocalOf<(AppThemeState) -> Unit> { {} }

@Composable
fun ProvideAppTheme(
    initialState: AppThemeState = AppThemeState(),
    content: @Composable () -> Unit
) {
    var themeState by remember { mutableStateOf(initialState) }

    CompositionLocalProvider(
        LocalAppTheme provides themeState,
        LocalThemeUpdater provides { newState -> themeState = newState }
    ) {
        content()
    }
}

// Utility Extension to quickly grab colors based on rules
@Composable
fun AppThemeState.getPrimaryContentColor(): Color {
    // If Apply Accent To All is true and we aren't bypassing exceptions
    return if (applyAccentToAll && currentTheme != InterfaceTheme.DEFAULT) {
        globalAccentColor
    } else {
        Color.White
    }
}
