package com.example.nexus.ui
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.api.LoginRequest
import com.example.nexus.api.RegisterRequest
import com.example.nexus.api.RetrofitClient
import com.example.nexus.api.UpdateProfileRequest
import com.example.nexus.api.toApiError
import com.example.nexus.api.toUserMessage
import com.example.nexus.auth.AuthSession
import com.example.nexus.settings.SettingsSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false
)

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    /**
     * Language as it stood when this auth flow started. The Login/Register picker can only
     * change the device-local preference (no token exists yet), so a language the user
     * picked before authenticating is pushed to the server once a token is available.
     * Comparing against the start value keeps a fresh "en" default from overwriting a
     * language the user previously saved from Settings.
     */
    private val languageAtStart = SettingsSession.language.value

    init {
        viewModelScope.launch {
            AuthSession.isAuthenticated.collect { authenticated ->
                _uiState.value = _uiState.value.copy(isAuthenticated = authenticated)
            }
        }
    }

    fun login(email: String, password: String) {
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
                RetrofitClient.instance.login(LoginRequest(email.trim(), password))
            }.onSuccess { response ->
                AuthSession.saveToken(response.token)
                syncLanguageIfChanged()
                _uiState.value = _uiState.value.copy(isLoading = false, isAuthenticated = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.toApiError().toUserMessage()
                )
            }
        }
    }

    fun register(email: String, password: String, displayName: String) {
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
                RetrofitClient.instance.register(RegisterRequest(email.trim(), password, displayName.trim()))
            }.onSuccess { response ->
                AuthSession.saveToken(response.token)
                syncLanguageIfChanged()
                _uiState.value = _uiState.value.copy(isLoading = false, isAuthenticated = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.toApiError().toUserMessage()
                )
            }
        }
    }

    /**
     * Best-effort push of a language chosen on the Login/Register screen to the server
     * via a partial `PATCH /users/me`. Only runs after a token is stored, and only when
     * the language actually changed during this auth flow. A failure is non-fatal: the pick
     * stays flagged in SettingsSession.languageSyncPending so a later server→local sync
     * (e.g. opening Settings) cannot silently revert it; the next successful push clears it.
     */
    private fun syncLanguageIfChanged() {
        val selected = SettingsSession.language.value
        if (selected == languageAtStart) return
        SettingsSession.markLanguageSyncPending()
        viewModelScope.launch {
            runCatching {
                RetrofitClient.instance.updateProfile(UpdateProfileRequest(language = selected))
            }.onSuccess {
                SettingsSession.clearLanguageSyncPending()
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
        return email.matches(emailRegex)
    }
}
