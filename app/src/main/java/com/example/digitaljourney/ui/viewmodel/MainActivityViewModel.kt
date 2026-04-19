package com.example.digitaljourney.ui.viewmodel

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.digitaljourney.data.managers.GoogleCalendarAuth
import com.example.digitaljourney.data.repositories.SettingsRepository
import com.example.digitaljourney.data.repositories.CalendarRepository
import com.example.digitaljourney.data.repositories.LocationRepositoryImpl
import com.example.digitaljourney.data.managers.LogSyncManager
import com.example.digitaljourney.data.repositories.AuthRepository
import com.example.digitaljourney.data.repositories.PhotosRepositoryImpl
import com.example.digitaljourney.data.repositories.SpotifyRepositoryImpl
import com.example.digitaljourney.data.repositories.WeatherRepository
import com.example.digitaljourney.model.LogCollectorWorker
import com.example.digitaljourney.notifications.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val authRepository = AuthRepository(appContext)
    private val settingsRepository = SettingsRepository(appContext)

    private val _googleAuthNeedsResolution = mutableStateOf<PendingIntent?>(null)
    val googleAuthNeedsResolution: State<PendingIntent?> = _googleAuthNeedsResolution

    private val _googleAuthError = mutableStateOf<String?>(null)
    val googleAuthError: State<String?> = _googleAuthError

    private val _spotifyAuthError = mutableStateOf<String?>(null)
    val spotifyAuthError: State<String?> = _spotifyAuthError


    private val _darkModeEnabled = mutableStateOf(settingsRepository.isDarkModeEnabled())
    val darkModeEnabled: State<Boolean> = _darkModeEnabled

    private val _notificationsEnabled = mutableStateOf(settingsRepository.isNotificationsEnabled())
    val notificationsEnabled: State<Boolean> = _notificationsEnabled

    private val _spotifyLoginIntent = mutableStateOf<Intent?>(null)
    val spotifyLoginIntent: State<Intent?> = _spotifyLoginIntent

    fun initializeApp() {
        scheduleLogCollector()
        runInitialSync()
    }

    private fun scheduleLogCollector() {
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            "log_collector",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<LogCollectorWorker>(15, TimeUnit.MINUTES).build()
        )
    }

    private fun runInitialSync() {
        viewModelScope.launch(Dispatchers.IO) {
            val sync = LogSyncManager(
                appContext,
                SpotifyRepositoryImpl(),
                PhotosRepositoryImpl(),
                LocationRepositoryImpl(),
                WeatherRepository(),
                CalendarRepository
            )
            sync.syncNow()
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

    fun startSpotifyLogin() {
        _spotifyLoginIntent.value = authRepository.buildSpotifyLoginIntent()
    }

    fun clearSpotifyLoginIntent() {
        _spotifyLoginIntent.value = null
    }

    fun handleSpotifyAuthResult(
        response: net.openid.appauth.AuthorizationResponse?,
        ex: net.openid.appauth.AuthorizationException?
    ) {
        if (response == null) {
            _spotifyAuthError.value = ex?.message ?: "Spotify auth failed"
            return
        }

        viewModelScope.launch {
            authRepository.exchangeSpotifyToken(response)
                .onFailure {
                    _spotifyAuthError.value = it.message ?: "Spotify auth failed"
                }
        }
    }

    fun startGoogleLogin(activity: android.app.Activity) {
        viewModelScope.launch {
            try {
                val result = GoogleCalendarAuth.signInAndAuthorize(activity)
                onGoogleLoginResult(result)
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                _googleAuthError.value = "Google sign-in was canceled or account reauth failed"
            } catch (e: androidx.credentials.exceptions.GetCredentialException) {
                _googleAuthError.value = "Credential Manager failed: ${e.message}"
            } catch (e: Exception) {
                _googleAuthError.value = "Unexpected Google sign-in failure"
            }
        }
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

    override fun onCleared() {
        super.onCleared()
        authRepository.dispose()
    }
}