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
    val isGlass: Boolean = false
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
        isGlass = false
    )

    val Glass = AppTheme(
        name = "Glass",
        backgroundColor = Color(0xFF0D0D0D),
        surfaceColor = Color(0x4D1C1C1E), // 30% opacity for glass effect
        primaryColor = Color(0xFF00D4FF),
        secondaryColor = Color(0x4D2C2C2E),
        textPrimaryColor = Color.White,
        textSecondaryColor = Color(0xFFB3B3B3),
        isGlass = true
    )

    val Midnight = AppTheme(
        name = "Midnight",
        backgroundColor = Color(0xFF0A0A0F),
        surfaceColor = Color(0xFF1A1A2E),
        primaryColor = Color(0xFFAF52DE),
        secondaryColor = Color(0xFF16213E),
        textPrimaryColor = Color(0xFFF0F0F0),
        textSecondaryColor = Color(0xFF9D9D9D),
        isGlass = false
    )

    val Vibrant = AppTheme(
        name = "Vibrant",
        backgroundColor = Color(0xFF1A0033),
        surfaceColor = Color(0xFF2D1B4E),
        primaryColor = Color(0xFFFF6B9D),
        secondaryColor = Color(0xFF4A2E6B),
        textPrimaryColor = Color.White,
        textSecondaryColor = Color(0xFFC7A4E0),
        isGlass = false
    )

    fun getThemeByName(name: String): AppTheme {
        return when (name) {
            "Glass" -> Glass
            "Midnight" -> Midnight
            "Vibrant" -> Vibrant
            else -> Default
        }
    }

    fun getAllThemes() = listOf(Default, Glass, Midnight, Vibrant)
}
