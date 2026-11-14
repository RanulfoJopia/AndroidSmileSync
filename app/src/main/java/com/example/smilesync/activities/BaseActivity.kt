package com.example.smilesync.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smilesync.R
import com.example.smilesync.utils.ThemeManager
import com.example.smilesync.utils.NetworkUtils
import com.example.smilesync.utils.UiUtils

/**
 * Base activity that applies the saved theme to all activities.
 * All app activities should extend this class.
 */
open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before super.onCreate() and setContentView()
        applyTheme()
        super.onCreate(savedInstanceState)
    }

    private fun applyTheme() {
        val themeManager = ThemeManager.getInstance(this)
        val isDarkMode = themeManager.isDarkMode()
        
        setTheme(
            if (isDarkMode) R.style.Theme_SmileSync_Dark
            else R.style.Theme_SmileSync_Light
        )
    }

    /**
     * Handle network errors with user-friendly messages
     */
    protected fun handleNetworkError(e: Exception, customMessage: String? = null) {
        val message = when {
            !NetworkUtils.isNetworkAvailable(this) -> 
                "No internet connection. Please check your network settings."
            NetworkUtils.isNetworkError(e) -> 
                NetworkUtils.getErrorMessage(e)
            else -> 
                customMessage ?: "An error occurred: ${e.message}"
        }
        UiUtils.toast(this, message)
    }
}
