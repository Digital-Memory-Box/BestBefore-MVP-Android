package com.dmb.bestbefore.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME = "selected_theme"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val DEFAULT_THEME = "Default"
        private const val DEFAULT_ACCENT_COLOR = 0xFF007AFF.toInt() // iOS blue
    }

    fun saveTheme(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    fun getTheme(): String {
        return prefs.getString(KEY_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
    }

    fun saveAccentColor(color: Color) {
        prefs.edit().putInt(KEY_ACCENT_COLOR, color.toArgb()).apply()
    }

    fun getAccentColor(): Color {
        val colorInt = prefs.getInt(KEY_ACCENT_COLOR, DEFAULT_ACCENT_COLOR)
        return Color(colorInt)
    }
}
