package com.example.smilesync.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages theme persistence and switching between light/dark modes.
 */
class ThemeManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "theme_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        
        const val THEME_LIGHT = 0
        const val THEME_DARK = 1

        @Volatile
        private var instance: ThemeManager? = null

        fun getInstance(context: Context): ThemeManager {
            return instance ?: synchronized(this) {
                instance ?: ThemeManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Get current theme mode (THEME_LIGHT or THEME_DARK)
     */
    fun getCurrentTheme(): Int {
        return prefs.getInt(KEY_THEME_MODE, THEME_DARK) // Default to dark
    }

    /**
     * Check if current theme is dark mode
     */
    fun isDarkMode(): Boolean {
        return getCurrentTheme() == THEME_DARK
    }

    /**
     * Save theme mode (no apply - use with recreate())
     */
    fun setTheme(themeMode: Int) {
        prefs.edit().putInt(KEY_THEME_MODE, themeMode).apply()
    }

    /**
     * Toggle between light and dark mode
     */
    fun toggleTheme() {
        val newTheme = if (isDarkMode()) THEME_LIGHT else THEME_DARK
        setTheme(newTheme)
    }
}
