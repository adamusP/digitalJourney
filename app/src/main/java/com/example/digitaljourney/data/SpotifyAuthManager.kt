package com.example.digitaljourney.data

import android.content.Context
import net.openid.appauth.*
import android.net.Uri
import kotlinx.coroutines.suspendCancellableCoroutine

object SpotifyAuthManager {
    private const val CLIENT_ID = "b240fef49f7946efa918336ada7aabab"
    private const val TOKEN_URL = "https://accounts.spotify.com/api/token"
    private const val REDIRECT_URI = "digitaljourney://callback"
    private const val AUTH_URL = "https://accounts.spotify.com/authorize"

    fun buildAuthRequest(): AuthorizationRequest {
        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse(AUTH_URL),
            Uri.parse(TOKEN_URL)
        )

        return AuthorizationRequest.Builder(
            serviceConfig,
            CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(REDIRECT_URI)
        )
            .setScopes("user-read-recently-played")
            .build()
    }

    suspend fun refreshAccessToken(context: Context): String? {
        val refreshToken = TokenManager.getRefreshToken(context) ?: return null

        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse(AUTH_URL),
            Uri.parse(TOKEN_URL)
        )

        val tokenRequest = TokenRequest.Builder(
            serviceConfig,
            CLIENT_ID
        )
            .setGrantType(GrantTypeValues.REFRESH_TOKEN)
            .setRefreshToken(refreshToken)
            .build()

        val authService = AuthorizationService(context)

        return suspendCancellableCoroutine { cont ->
            authService.performTokenRequest(tokenRequest) { response, ex ->
                if (response?.accessToken != null) {
                    // Save new access token (and refresh if updated)
                    TokenManager.saveTokens(
                        context,
                        response.accessToken!!,
                        response.refreshToken ?: refreshToken
                    )
                    cont.resume(response.accessToken, null)
                } else {
                    cont.resume(null, null)
                }
            }
        }
    }
}
