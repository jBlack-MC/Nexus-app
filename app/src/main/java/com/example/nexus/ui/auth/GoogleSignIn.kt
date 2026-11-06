package com.example.nexus.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.nexus.BuildConfig
import com.example.nexus.R
import com.example.nexus.ui.components.NexusSecondaryButton
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

/**
 * Google single sign-on with Android Credential Manager.
 *
 * Flow: the user taps the button → Credential Manager shows the Google account sheet → we receive a
 * Google ID token → [onIdToken] sends it to the Nexus API (POST /auth/google), which verifies it
 * with Google and returns a normal Nexus JWT. The client never trusts the token itself.
 *
 * Requires BuildConfig.GOOGLE_WEB_CLIENT_ID (the Web OAuth client of the same Google Cloud project
 * that has an Android OAuth client for this package name and signing-certificate SHA-1).
 * Reference: https://developer.android.com/identity/sign-in/credential-manager-siwg (accessed 2026-11-05).
 */
object GoogleSignIn {
    private const val TAG = "GoogleSignIn"

    val isConfigured: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    sealed interface Result {
        data class Token(val idToken: String) : Result

        data object Cancelled : Result

        data class Failed(val message: String) : Result
    }

    suspend fun requestIdToken(activityContext: Context): Result {
        val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                Result.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                Log.w(TAG, "Unexpected credential type ${credential.type}")
                Result.Failed(MSG_FAILED)
            }
        } catch (error: GetCredentialCancellationException) {
            Result.Cancelled
        } catch (error: NoCredentialException) {
            Log.w(TAG, "No Google account available", error)
            Result.Failed(MSG_NO_ACCOUNT)
        } catch (error: GetCredentialException) {
            Log.w(TAG, "Credential Manager failed", error)
            Result.Failed(MSG_FAILED)
        }
    }

    const val MSG_NO_ACCOUNT = "Add a Google account to this device to continue with Google."
    const val MSG_FAILED = "Google sign-in failed. Please try again."
}

/**
 * The hosting Activity. LocalContext is a locale-specific configuration context (see MainActivity),
 * but Credential Manager must be given an Activity to show its account sheet.
 */
val LocalHostActivity = staticCompositionLocalOf<Activity?> { null }

/** "Continue with Google" button. Hidden entirely when SSO is not configured for this build. */
@Composable
fun GoogleSignInButton(enabled: Boolean, onIdToken: (String) -> Unit, onError: (String?) -> Unit, modifier: Modifier = Modifier,) {
    if (!GoogleSignIn.isConfigured) return
    val activity = LocalHostActivity.current ?: return
    val scope = rememberCoroutineScope()
    NexusSecondaryButton(
        text = stringResource(R.string.auth_continue_google),
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        onClick = {
            scope.launch {
                when (val result = GoogleSignIn.requestIdToken(activity)) {
                    is GoogleSignIn.Result.Token -> onIdToken(result.idToken)
                    GoogleSignIn.Result.Cancelled -> onError(null)
                    is GoogleSignIn.Result.Failed -> onError(result.message)
                }
            }
        },
    )
}

@Composable
fun OrDivider() {
    if (GoogleSignIn.isConfigured) Text(stringResource(R.string.auth_or))
}
