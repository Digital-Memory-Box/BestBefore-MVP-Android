package com.dmb.bestbefore.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * CompositionLocal providing the current BestBefore app theme palette.
 * Any composable in the tree can access `LocalBestBeforeColors.current` for theme colors.
 */
data class BestBeforeColorPalette(
    val background: Color,
    val surface: Color,
    val primary: Color,        // accent / primary action color
    val secondary: Color,      // secondary surface / card background
    val textPrimary: Color,
    val textSecondary: Color,
    val isGlass: Boolean
)

val LocalBestBeforeColors = compositionLocalOf {
    // Default palette — matches AppThemes.Default + blue accent
    BestBeforeColorPalette(
        background = Color(0xFF000000),
        surface = Color(0xFF1C1C1E),
        primary = Color(0xFF007AFF),
        secondary = Color(0xFF2C2C2E),
        textPrimary = Color.White,
        textSecondary = Color.Gray,
        isGlass = false
    )
}

@Composable
fun BestBeforeTheme(
    appTheme: AppTheme = AppThemes.Default,
    accentColor: Color = Color(0xFF007AFF),
    content: @Composable () -> Unit
) {
    // Build palette from the selected AppTheme, overriding primary with the user's accent color
    val palette = BestBeforeColorPalette(
        background = appTheme.backgroundColor,
        surface = appTheme.surfaceColor,
        primary = accentColor,
        secondary = appTheme.secondaryColor,
        textPrimary = appTheme.textPrimaryColor,
        textSecondary = appTheme.textSecondaryColor,
        isGlass = appTheme.isGlass
    )

    // Also push into MaterialTheme so Material3 components pick up colors automatically
    val colorScheme = darkColorScheme(
        primary = accentColor,
        secondary = appTheme.secondaryColor,
        tertiary = appTheme.textSecondaryColor,
        background = appTheme.backgroundColor,
        surface = appTheme.surfaceColor,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = appTheme.textPrimaryColor,
        onSurface = appTheme.textPrimaryColor,
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = appTheme.backgroundColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    CompositionLocalProvider(LocalBestBeforeColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}