package com.example.digitaljourney.data.managers

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.tasks.await


object GoogleCalendarAuth {

    private const val WEB_CLIENT_ID = "413902878148-5porqpng826ic0rr0k4damrac4cb6oec.apps.googleusercontent.com"
    private const val CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar.readonly"

    // sign in using credential manager
    suspend fun signInAndAuthorize(activity: Activity): AuthResult {
        val credentialManager = CredentialManager.create(activity)

        val googleOption = GetSignInWithGoogleOption.Builder(
            serverClientId = WEB_CLIENT_ID
        ).build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        val credentialResponse = try {
            credentialManager.getCredential(
                request = request,
                context = activity
            )
        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            Log.e("GoogleAuth", "Cancellation: ${e.type} / ${e.message}", e)
            return AuthResult.Error("Google sign-in was canceled or account reauth failed")
        } catch (e: androidx.credentials.exceptions.GetCredentialException) {
            return AuthResult.Error("Credential Manager error: ${e.message}")
        }

        val credential = credentialResponse.credential
        if (
            credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return AuthResult.Error("Unexpected credential type")
        }

        return try {
            GoogleIdTokenCredential.createFrom(credential.data)
            requestCalendarAccess(activity)
        } catch (e: GoogleIdTokenParsingException) {
            AuthResult.Error("Failed to parse Google credential: ${e.message}")
        }
    }

    suspend fun tryRefreshAccessTokenSilently(context: Context): String? {
        return try {
            when (val result = requestCalendarAccess(context)) {
                is AuthResult.Token -> result.accessToken
                is AuthResult.NeedsResolution -> null
                is AuthResult.Error -> null
            }
        } catch (e: Exception) {
            Log.e("GoogleCalendarAuth", "Silent auth failed", e)
            null
        }
    }

    suspend fun requestCalendarAccess(context: Context): AuthResult {
        val requestedScopes = listOf(
            Scope(CALENDAR_SCOPE)
        )

        val request = AuthorizationRequest.builder()
            .setRequestedScopes(requestedScopes)
            .build()

        return try {
            val authorizationResult: AuthorizationResult =
                Identity.getAuthorizationClient(context)
                    .authorize(request)
                    .await()

            val accessToken = authorizationResult.accessToken

            when {
                !accessToken.isNullOrBlank() -> AuthResult.Token(accessToken)
                authorizationResult.hasResolution() -> {
                    AuthResult.NeedsResolution(authorizationResult.pendingIntent)
                }
                else -> AuthResult.Error("No token and no resolution returned")
            }
        } catch (e: ApiException) {
            AuthResult.Error("Authorization failed: ${e.statusCode}")
        }
    }

    sealed class AuthResult {
        data class Token(val accessToken: String) : AuthResult()
        data class NeedsResolution(val pendingIntent: android.app.PendingIntent?) : AuthResult()
        data class Error(val message: String) : AuthResult()
    }
}