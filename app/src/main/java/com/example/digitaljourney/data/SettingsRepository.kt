package com.example.digitaljourney.data

import android.content.Context

// manages settings
class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun isNotificationsEnabled(): Boolean {
        return prefs.getBoolean("notifications_enabled", false)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
    }
}