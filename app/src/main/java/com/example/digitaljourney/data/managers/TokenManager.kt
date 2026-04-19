package com.example.digitaljourney.data.managers

import android.content.Context

object TokenManager {
    private const val PREFS_NAME = "digitaljourney"
    private const val KEY_SPOTIFY_ACCESS_TOKEN = "spotify_access_token"
    private const val KEY_SPOTIFY_REFRESH_TOKEN = "spotify_refresh_token"
    private const val WEATHER_API_KEY = "d6facaf40c47b2b1dc2f23e84f84781f"
    private const val KEY_CHESS_USERNAME = "chess_username"
    private const val KEY_LETR_USERNAME = "letr_username"
    private const val PREFS = "tokens"
    private const val GOOGLE_ACCESS_TOKEN = "google_access_token"
    private const val GOOGLE_REFRESH_TOKEN = "google_refresh_token"
    private const val GOOGLE_CALENDAR_SYNC_TOKEN = "google_calendar_sync_token"

    fun saveChessUsername(context: Context, username: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CHESS_USERNAME, username).apply()
    }

    fun getChessUsername(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CHESS_USERNAME, null)
    }

    fun saveLetrUsername(context: Context, username: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LETR_USERNAME, username).apply()
    }

    fun getLetrUsername(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LETR_USERNAME, null)
    }


    fun saveSpotifyTokens(context: Context, accessToken: String, refreshToken: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_SPOTIFY_ACCESS_TOKEN, accessToken)
            if (refreshToken != null) {
                putString(KEY_SPOTIFY_REFRESH_TOKEN, refreshToken)
            }
        }.apply()
    }

    fun getSpotifyAccessToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SPOTIFY_ACCESS_TOKEN, null)
    }

    fun saveGoogleTokens(context: Context, accessToken: String, refreshToken: String?) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(GOOGLE_ACCESS_TOKEN, accessToken)
            .putString(GOOGLE_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun getGoogleAccessToken(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(GOOGLE_ACCESS_TOKEN, null)
    }

    fun getRefreshToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SPOTIFY_REFRESH_TOKEN, null)
    }

    fun getWeatherApi(): String {
        return WEATHER_API_KEY
    }

    fun saveGoogleCalendarSyncToken(context: Context, syncToken: String) {
        val prefs = context.getSharedPreferences("tokens", Context.MODE_PRIVATE)
        prefs.edit().putString(GOOGLE_CALENDAR_SYNC_TOKEN, syncToken).apply()
    }

    fun getGoogleCalendarSyncToken(context: Context): String? {
        val prefs = context.getSharedPreferences("tokens", Context.MODE_PRIVATE)
        return prefs.getString(GOOGLE_CALENDAR_SYNC_TOKEN, null)
    }

    fun clearGoogleCalendarSyncToken(context: Context) {
        val prefs = context.getSharedPreferences("tokens", Context.MODE_PRIVATE)
        prefs.edit().remove(GOOGLE_CALENDAR_SYNC_TOKEN).apply()
    }
}
