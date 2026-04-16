package com.example.digitaljourney.data.repositories

import android.content.Context
import com.example.digitaljourney.data.managers.GoogleCalendarAuth
import com.example.digitaljourney.data.managers.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.TokenResponse
import kotlin.coroutines.resume

class AuthRepository(private val context: Context) {

    suspend fun exchangeSpotifyToken(
        authService: AuthorizationService,
        response: AuthorizationResponse
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            suspendCancellableTokenExchange(authService, response).fold(
                onSuccess = { tokenResponse ->
                    val accessToken = tokenResponse.accessToken
                    if (accessToken != null) {
                        TokenManager.saveSpotifyTokens(
                            context,
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
            GoogleCalendarAuth.requestCalendarAccess(context)
        }
    }

    suspend fun onGoogleAccessTokenReceived(accessToken: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                TokenManager.saveGoogleTokens(context, accessToken, null)
                CalendarRepository.syncCalendarLogs(context)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun suspendCancellableTokenExchange(
        authService: AuthorizationService,
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
}