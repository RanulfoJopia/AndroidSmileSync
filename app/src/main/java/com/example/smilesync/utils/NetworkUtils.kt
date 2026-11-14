package com.example.smilesync.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object NetworkUtils {
    
    /**
     * Check if device has active internet connection
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
        }
    }
    
    /**
     * Get user-friendly error message from exception
     */
    fun getErrorMessage(e: Exception): String {
        return when (e) {
            is UnknownHostException -> "Unable to reach server. Please check your internet connection."
            is SocketTimeoutException -> "Connection timeout. Please check your internet connection and try again."
            is IOException -> "Network error. Please check your internet connection."
            else -> e.message ?: "An unexpected error occurred. Please try again."
        }
    }
    
    /**
     * Check if exception is network-related
     */
    fun isNetworkError(e: Exception): Boolean {
        return e is UnknownHostException || 
               e is SocketTimeoutException || 
               e is IOException
    }
}
