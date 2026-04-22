package com.dmb.bestbefore.ui.theme

import androidx.compose.ui.graphics.Color

data class AppTheme(
    val name: String,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val primaryColor: Color,
    val secondaryColor: Color,
    val textPrimaryColor: Color,
    val textSecondaryColor: Color,
    val isGlass: Boolean = false,
    val orbColor1: Color = Color(0xFF0D59F2),
    val orbColor2: Color = Color(0xFF00D972)
)

object AppThemes {
    val Default = AppTheme(
        name = "Default",
        backgroundColor = Color(0xFF000000),
        surfaceColor = Color(0xFF1C1C1E),
        primaryColor = Color(0xFF007AFF),
        secondaryColor = Color(0xFF2C2C2E),
        textPrimaryColor = Color.White,
        textSecondaryColor = Color.Gray,
        isGlass = false,
        orbColor1 = Color(0xFF0D59F2),
        orbColor2 = Color(0xFF00D972)
    )

    val Glass = AppTheme(
        name = "Glass",
        backgroundColor = Color(0xFF050510),
        surfaceColor = Color(0x33FFFFFF), // Frosted glass
        primaryColor = Color(0xFF00D4FF),
        secondaryColor = Color(0x1AFFFFFF),
        textPrimaryColor = Color.White,
        textSecondaryColor = Color(0xFFCCCCCC),
        isGlass = true,
        orbColor1 = Color(0xFF00D4FF),
        orbColor2 = Color(0xFF007AFF)
    )

    val Midnight = AppTheme(
        name = "Midnight",
        backgroundColor = Color(0xFF020205),
        surfaceColor = Color(0xFF0A0A1F),
        primaryColor = Color(0xFFBF5AF2),
        secondaryColor = Color(0xFF1C1C44),
        textPrimaryColor = Color(0xFFF0F0F0),
        textSecondaryColor = Color(0xFF8E8E93),
        isGlass = false,
        orbColor1 = Color(0xFF5E5CE6),
        orbColor2 = Color(0xFFBF5AF2)
    )

    val Vibrant = AppTheme(
        name = "Vibrant",
        backgroundColor = Color(0xFF120024),
        surfaceColor = Color(0xFF2D0059),
        primaryColor = Color(0xFFFF2D55),
        secondaryColor = Color(0xFF4A0080),
        textPrimaryColor = Color.White,
        textSecondaryColor = Color(0xFFFF9FF3),
        isGlass = false,
        orbColor1 = Color(0xFFFF2D55),
        orbColor2 = Color(0xFF5856D6)
    )

    fun getThemeByName(name: String): AppTheme {
        return when (name) {
            "Glass" -> Glass
            "Midnight" -> Midnight
            else -> Default
        }
    }

    fun getAllThemes() = listOf(Default, Glass, Midnight)
}
