package com.example.digitaljourney.data

import android.content.Context

object TokenManager {
    private const val PREFS_NAME = "digitaljourney"
    private const val KEY_SPOTIFY_ACCESS_TOKEN = "spotify_access_token"
    private const val KEY_SPOTIFY_REFRESH_TOKEN = "spotify_refresh_token"
    private const val WEATHER_API_KEY = "d6facaf40c47b2b1dc2f23e84f84781f"
    private const val KEY_CHESS_USERNAME = "chess_username"

    fun saveChessUsername(context: Context, username: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CHESS_USERNAME, username).apply()
    }

    fun getChessUsername(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CHESS_USERNAME, null)
    }

    fun saveTokens(context: Context, accessToken: String, refreshToken: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_SPOTIFY_ACCESS_TOKEN, accessToken)
            if (refreshToken != null) {
                putString(KEY_SPOTIFY_REFRESH_TOKEN, refreshToken)
            }
        }.apply()
    }

    fun getAccessToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SPOTIFY_ACCESS_TOKEN, null)
    }

    fun getRefreshToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SPOTIFY_REFRESH_TOKEN, null)
    }

    fun getWeatherApi(context: Context): String {
        return WEATHER_API_KEY
    }
}
