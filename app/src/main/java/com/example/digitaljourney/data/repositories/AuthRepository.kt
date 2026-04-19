package com.example.digitaljourney.data.repositories

import android.content.Context
import android.content.Intent
import com.example.digitaljourney.data.managers.GoogleCalendarAuth
import com.example.digitaljourney.data.managers.SpotifyAuthManager
import com.example.digitaljourney.data.managers.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.TokenResponse
import kotlin.coroutines.resume

class AuthRepository(context: Context) {

    private val appContext = context.applicationContext
    private val authService = AuthorizationService(appContext)

    fun buildSpotifyLoginIntent(): Intent {
        val authRequest = SpotifyAuthManager.buildAuthRequest()
        return authService.getAuthorizationRequestIntent(authRequest)
    }

    suspend fun exchangeSpotifyToken(
        response: AuthorizationResponse
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            suspendCancellableTokenExchange(response).fold(
                onSuccess = { tokenResponse ->
                    val accessToken = tokenResponse.accessToken
                    if (accessToken != null) {
                        TokenManager.saveSpotifyTokens(
                            appContext,
                            accessToken,
                            tokenResponse.refreshToken
                        )
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Spotify access token was null"))
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestGoogleCalendarAccess(): GoogleCalendarAuth.AuthResult {
        return withContext(Dispatchers.IO) {
            GoogleCalendarAuth.requestCalendarAccess(appContext)
        }
    }

    suspend fun onGoogleAccessTokenReceived(accessToken: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                TokenManager.saveGoogleTokens(appContext, accessToken, null)
                CalendarRepository.syncCalendarLogs(appContext)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun suspendCancellableTokenExchange(
        response: AuthorizationResponse
    ): Result<TokenResponse> =
        suspendCancellableCoroutine { cont ->
            authService.performTokenRequest(
                response.createTokenExchangeRequest()
            ) { tokenResponse, tokenEx ->
                when {
                    tokenResponse != null -> cont.resume(Result.success(tokenResponse))
                    tokenEx != null -> cont.resume(Result.failure(tokenEx))
                    else -> cont.resume(Result.failure(Exception("Unknown Spotify token exchange error")))
                }
            }
        }

    fun dispose() {
        authService.dispose()
    }
}