package com.dmb.bestbefore.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.dmb.bestbefore.data.local.PreferencesManager

/**
 * Global observable theme state that triggers recomposition across the entire app
 * when theme or accent color changes. Backed by PreferencesManager for persistence.
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

    private var prefsManager: PreferencesManager? = null

    fun init(context: Context) {
        if (prefsManager == null) {
            prefsManager = PreferencesManager(context.applicationContext)
        }
        currentTheme = AppThemes.getThemeByName(prefsManager!!.getTheme())
        currentAccent = prefsManager!!.getAccentColor()
    }

    fun selectTheme(theme: AppTheme) {
        currentTheme = theme
        prefsManager?.saveTheme(theme.name)
    }

    fun selectAccent(color: Color) {
        currentAccent = color
        prefsManager?.saveAccentColor(color)
    }

    fun updateApplyAccentToAll(enabled: Boolean) {
        applyAccentToAll = enabled
    }

    fun updateSyncAccentWithRoom(enabled: Boolean) {
        syncAccentWithRoom = enabled
        if (!enabled) {
            currentAccent = prefsManager?.getAccentColor() ?: currentAccent
        }
    }

    fun syncAccent(color: Color) {
        if (syncAccentWithRoom) {
            currentAccent = color
        }
    }
}
