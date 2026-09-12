package com.example.smilesync.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.smilesync.model.Dentist
import com.google.gson.Gson

/**
 * Persists the Sanctum Bearer token and the logged-in dentist's profile
 * in private SharedPreferences so they survive app restarts.
 */
class TokenManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveToken(token: String) = prefs.edit().putString(KEY_TOKEN, token).apply()

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun clearToken() = prefs.edit().remove(KEY_TOKEN).apply()

    fun saveDentist(dentist: Dentist) =
        prefs.edit().putString(KEY_DENTIST, gson.toJson(dentist)).apply()

    fun getDentist(): Dentist? = try {
        val json = prefs.getString(KEY_DENTIST, null) ?: return null
        gson.fromJson(json, Dentist::class.java)
    } catch (e: Exception) { null }

    fun clearDentist() = prefs.edit().remove(KEY_DENTIST).apply()

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    fun logout() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_DENTIST).apply()
    }

    companion object {
        private const val PREFS_NAME = "smilesync_auth"
        private const val KEY_TOKEN  = "auth_token"
        private const val KEY_DENTIST = "auth_dentist"

        @Volatile private var instance: TokenManager? = null

        fun getInstance(context: Context): TokenManager =
            instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext).also { instance = it }
            }
    }
}
