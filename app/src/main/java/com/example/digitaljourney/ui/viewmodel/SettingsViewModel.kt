package com.example.digitaljourney.ui.viewmodel

import android.app.Application
import android.app.PendingIntent
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitaljourney.data.repositories.AuthRepository
import com.example.digitaljourney.data.managers.GoogleCalendarAuth
import com.example.digitaljourney.data.repositories.SettingsRepository
import com.example.digitaljourney.data.managers.TokenManager
import com.example.digitaljourney.notifications.NotificationScheduler
import kotlinx.coroutines.launch
import com.example.digitaljourney.data.repositories.LogRepository
import kotlinx.coroutines.Dispatchers

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val settingsRepository = SettingsRepository(appContext)
    private val authRepository = AuthRepository(appContext)

    private val _darkModeEnabled = mutableStateOf(settingsRepository.isDarkModeEnabled())
    val darkModeEnabled: State<Boolean> = _darkModeEnabled

    private val _notificationsEnabled = mutableStateOf(settingsRepository.isNotificationsEnabled())
    val notificationsEnabled: State<Boolean> = _notificationsEnabled

    private val _googleAuthNeedsResolution = mutableStateOf<PendingIntent?>(null)
    val googleAuthNeedsResolution: State<PendingIntent?> = _googleAuthNeedsResolution

    private val _googleAuthError = mutableStateOf<String?>(null)
    val googleAuthError: State<String?> = _googleAuthError

    private val _spotifyAuthError = mutableStateOf<String?>(null)
    val spotifyAuthError: State<String?> = _spotifyAuthError

    private val logRepository = LogRepository(application)

    private val _totalLogs = mutableStateOf(0)
    val totalLogs: State<Int> = _totalLogs

    fun loadTotalLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            _totalLogs.value = logRepository.getAllLogsCount()
        }
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        _darkModeEnabled.value = enabled
        settingsRepository.setDarkModeEnabled(enabled)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        settingsRepository.setNotificationsEnabled(enabled)

        if (enabled) {
            NotificationScheduler.scheduleDailyReminder(appContext)
        } else {
            NotificationScheduler.cancelDailyReminder(appContext)
        }
    }

    fun saveChessUsername(username: String) {
        val trimmed = username.trim()
        if (trimmed.isNotEmpty()) {
            TokenManager.saveChessUsername(appContext, trimmed)
        }
    }

    fun onGoogleLoginResult(result: GoogleCalendarAuth.AuthResult) {
        viewModelScope.launch {
            when (result) {
                is GoogleCalendarAuth.AuthResult.Token -> {
                    authRepository.onGoogleAccessTokenReceived(result.accessToken)
                        .onFailure { _googleAuthError.value = it.message ?: "Google auth failed" }
                }

                is GoogleCalendarAuth.AuthResult.NeedsResolution -> {
                    _googleAuthNeedsResolution.value = result.pendingIntent
                }

                is GoogleCalendarAuth.AuthResult.Error -> {
                    _googleAuthError.value = result.message
                }
            }
        }
    }

    fun continueGoogleLoginAfterResolution() {
        viewModelScope.launch {
            when (val result = authRepository.requestGoogleCalendarAccess()) {
                is GoogleCalendarAuth.AuthResult.Token -> {
                    authRepository.onGoogleAccessTokenReceived(result.accessToken)
                        .onFailure { _googleAuthError.value = it.message ?: "Google auth failed" }
                }

                is GoogleCalendarAuth.AuthResult.Error -> {
                    _googleAuthError.value = result.message
                }

                is GoogleCalendarAuth.AuthResult.NeedsResolution -> {
                    _googleAuthError.value = "Authorization still requires resolution"
                }
            }
        }
    }

    fun onSpotifyTokenExchangeFailed(message: String) {
        _spotifyAuthError.value = message
    }

    fun clearGoogleResolution() {
        _googleAuthNeedsResolution.value = null
    }

    fun clearGoogleAuthError() {
        _googleAuthError.value = null
    }

    fun clearSpotifyAuthError() {
        _spotifyAuthError.value = null
    }
}