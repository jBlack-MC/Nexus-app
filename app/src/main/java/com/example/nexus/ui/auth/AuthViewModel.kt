package com.example.nexus.ui.auth

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

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
)

class AuthViewModel(
    private val userRepository: UserRepository? = null,
    private val loginAction: suspend (LoginRequest) -> AuthResponse = { (userRepository ?: NexusApp.userRepository).login(it) },
    private val registerAction: suspend (RegisterRequest) -> AuthResponse = { (userRepository ?: NexusApp.userRepository).register(it) },
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

    fun login(
        email: String,
        password: String,
    ) {
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
                saveToken(response.token)
                syncLanguageIfChanged()
                _uiState.value = _uiState.value.copy(isLoading = false, isAuthenticated = true)
            }.onFailure { error ->
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.toApiError().toUserMessage(),
                    )
            }
        }
    }

    fun register(
        email: String,
        password: String,
        displayName: String,
    ) {
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
                saveToken(response.token)
                syncLanguageIfChanged()
                _uiState.value = _uiState.value.copy(isLoading = false, isAuthenticated = true)
            }.onFailure { error ->
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.toApiError().toUserMessage(),
                    )
            }
        }
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

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
        return email.matches(emailRegex)
    }
}
