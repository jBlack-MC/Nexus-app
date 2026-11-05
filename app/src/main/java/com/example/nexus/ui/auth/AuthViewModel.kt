package com.example.nexus.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.NexusApp
import com.example.nexus.api.AuthResponse
import com.example.nexus.api.LoginRequest
import com.example.nexus.api.RegisterRequest
import com.example.nexus.api.UpdateProfileRequest
import com.example.nexus.api.UserProfile
import com.example.nexus.api.toApiError
import com.example.nexus.api.toUserMessage
import com.example.nexus.auth.AuthSession
import com.example.nexus.data.UserRepository
import com.example.nexus.settings.SettingsSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
)

class AuthViewModel(
    private val userRepository: UserRepository? = null,
    private val loginAction: suspend (LoginRequest) -> AuthResponse = { (userRepository ?: NexusApp.userRepository).login(it) },
    private val registerAction: suspend (RegisterRequest) -> AuthResponse = { (userRepository ?: NexusApp.userRepository).register(it) },
    private val googleSignInAction: suspend (String) -> AuthResponse = { (userRepository ?: NexusApp.userRepository).googleSignIn(it) },
    private val onSignedIn: () -> Unit = { NexusApp.onSignedIn() },
    private val profileUpdater: suspend (
        UpdateProfileRequest,
    ) -> UserProfile = { (userRepository ?: NexusApp.userRepository).updateProfile(it) },
    private val authSessionState: StateFlow<Boolean> = AuthSession.isAuthenticated,
    private val saveToken: (String) -> Unit = { AuthSession.saveToken(it) },
    private val getLanguage: () -> String = { SettingsSession.language.value },
    private val markLanguageSyncPending: () -> Unit = { SettingsSession.markLanguageSyncPending() },
    private val clearLanguageSyncPending: () -> Unit = { SettingsSession.clearLanguageSyncPending() },
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    private val languageAtStart = getLanguage()

    init {
        viewModelScope.launch {
            authSessionState.collect { authenticated ->
                _uiState.value = _uiState.value.copy(isAuthenticated = authenticated)
            }
        }
    }

    fun login(email: String, password: String,) {
        if (!isValidEmail(email)) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid email address")
            return
        }
        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                loginAction(LoginRequest(email.trim(), password))
            }.onSuccess { response ->
                completeSignIn(response)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = authErrorMessage(error))
            }
        }
    }

    fun register(email: String, password: String, displayName: String,) {
        if (displayName.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a display name")
            return
        }
        if (!isValidEmail(email)) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid email address")
            return
        }
        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                registerAction(RegisterRequest(email.trim(), password, displayName.trim()))
            }.onSuccess { response ->
                completeSignIn(response)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = authErrorMessage(error))
            }
        }
    }

    /** Google SSO: [idToken] comes from Credential Manager; the API verifies it with Google. */
    fun googleSignIn(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { googleSignInAction(idToken) }
                .onSuccess { response -> completeSignIn(response) }
                .onFailure { error ->
                    Log.w(TAG, "Google sign-in rejected by the API", error)
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = authErrorMessage(error, google = true))
                }
        }
    }

    /** Called when Credential Manager itself fails or the user cancels the Google account picker. */
    fun googleSignInUnavailable(message: String?) {
        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = message)
    }

    private fun completeSignIn(response: AuthResponse) {
        saveToken(response.token)
        syncLanguageIfChanged()
        onSignedIn()
        Log.i(TAG, "Signed in")
        _uiState.value = _uiState.value.copy(isLoading = false, isAuthenticated = true)
    }

    private fun syncLanguageIfChanged() {
        val selected = getLanguage()
        if (selected == languageAtStart) return
        markLanguageSyncPending()
        viewModelScope.launch {
            runCatching {
                profileUpdater(UpdateProfileRequest(language = selected))
            }.onSuccess {
                clearLanguageSyncPending()
            }
        }
    }

    /**
     * Auth endpoints need their own messages: a 401 here means wrong credentials, not an expired
     * session, and 409/429 have specific causes the user can act on (AUD-18).
     */
    private fun authErrorMessage(error: Throwable, google: Boolean = false): String = when ((error as? HttpException)?.code()) {
        401 -> if (google) MSG_GOOGLE_REJECTED else MSG_WRONG_CREDENTIALS
        409 -> MSG_EMAIL_TAKEN
        429 -> MSG_TOO_MANY_ATTEMPTS
        503 -> if (google) MSG_GOOGLE_NOT_CONFIGURED else error.toApiError().toUserMessage()
        else -> error.toApiError().toUserMessage()
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
        return email.matches(emailRegex)
    }

    companion object {
        private const val TAG = "AuthViewModel"
        const val MSG_WRONG_CREDENTIALS = "Email or password is incorrect."
        const val MSG_EMAIL_TAKEN = "An account with this email already exists. Sign in instead."
        const val MSG_TOO_MANY_ATTEMPTS = "Too many attempts. Please wait 15 minutes and try again."
        const val MSG_GOOGLE_REJECTED = "Google sign-in could not be verified. Please try again."
        const val MSG_GOOGLE_NOT_CONFIGURED = "Google sign-in is not available on this server yet."
    }
}
