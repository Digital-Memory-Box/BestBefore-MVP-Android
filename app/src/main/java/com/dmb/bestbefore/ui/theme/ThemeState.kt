package com.dmb.bestbefore.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.dmb.bestbefore.data.local.SessionManager

/**
 * Global observable theme state that triggers recomposition across the entire app
 * when theme or accent color changes. Backed by SessionManager for persistence.
 */
object ThemeState {
    var currentTheme by mutableStateOf(AppThemes.Default)
        private set

    var currentAccent by mutableStateOf(Color(0xFF007AFF))
        private set

    var applyAccentToAll by mutableStateOf(false)
        private set

    var syncAccentWithRoom by mutableStateOf(false)
        private set

    fun init(context: Context) {
        val sessionManager = SessionManager(context)
        currentTheme = AppThemes.getThemeByName(sessionManager.getTheme())
        val accentHex = sessionManager.getAccentColor()
        try {
            currentAccent = Color(android.graphics.Color.parseColor(accentHex))
        } catch (e: Exception) {
            currentAccent = Color(0xFF007AFF)
        }
    }

    fun selectTheme(context: Context, theme: AppTheme) {
        currentTheme = theme
        SessionManager(context).saveTheme(theme.name)
    }

    fun selectAccent(context: Context, color: Color) {
        currentAccent = color
        val hex = String.format("#%06X", (0xFFFFFF and color.toArgb()))
        SessionManager(context).saveAccentColor(hex)
    }

    fun updateApplyAccentToAll(enabled: Boolean) {
        applyAccentToAll = enabled
    }

    fun updateSyncAccentWithRoom(enabled: Boolean) {
        syncAccentWithRoom = enabled
    }

    fun syncAccent(color: Color) {
        if (syncAccentWithRoom) {
            currentAccent = color
        }
    }
}

// Extension to get argb from Color if not available
private fun Color.toArgb(): Int {
    return (this.alpha * 255.0f + 0.5f).toInt() shl 24 or
           ((this.red * 255.0f + 0.5f).toInt() shl 16) or
           ((this.green * 255.0f + 0.5f).toInt() shl 8) or
           (this.blue * 255.0f + 0.5f).toInt()
}
