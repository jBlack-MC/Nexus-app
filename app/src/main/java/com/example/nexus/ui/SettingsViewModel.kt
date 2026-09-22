package com.example.nexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.NexusApp
import com.example.nexus.api.ApiError
import com.example.nexus.api.UpdateProfileRequest
import com.example.nexus.api.UserProfile
import com.example.nexus.api.toApiError
import com.example.nexus.api.toUserMessage
import com.example.nexus.auth.AuthSession
import com.example.nexus.settings.SettingsSession
import com.example.nexus.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val selectedLanguage: String = "en",
    val isLoading: Boolean = false,
    val profile: UserProfile? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            SettingsSession.themeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
        viewModelScope.launch {
            SettingsSession.notificationsEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
            }
        }
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                NexusApp.repository.getProfile()
            }.onSuccess { profile ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profile = profile,
                    selectedLanguage = profile.language,
                    notificationsEnabled = profile.notificationsEnabled
                )
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    fun updateProfile(displayName: String) {
        if (displayName.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            runCatching {
                NexusApp.repository.updateProfile(UpdateProfileRequest(displayName = displayName.trim()))
            }.onSuccess { profile ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profile = profile,
                    successMessage = "Profile updated successfully!"
                )
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        SettingsSession.setThemeMode(mode)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        SettingsSession.setNotificationsEnabled(enabled)
        updatePreferences(notificationsEnabled = enabled)
    }

    fun setLanguage(language: String) {
        if (language == _uiState.value.selectedLanguage) return
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
        updatePreferences(language = language)
    }

    fun logout() {
        AuthSession.clearToken()
    }

    private fun handleError(error: Throwable) {
        val apiError = error.toApiError()
        if (apiError is ApiError.SessionExpired) {
            AuthSession.clearToken()
        }
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = apiError.toUserMessage()
        )
    }

    private fun updatePreferences(language: String? = null, notificationsEnabled: Boolean? = null) {
        viewModelScope.launch {
            runCatching {
                NexusApp.repository.updateProfile(
                    UpdateProfileRequest(language = language, notificationsEnabled = notificationsEnabled)
                )
            }.onSuccess { profile ->
                _uiState.value = _uiState.value.copy(
                    profile = profile,
                    selectedLanguage = profile.language,
                    notificationsEnabled = profile.notificationsEnabled
                )
            }.onFailure(::handleError)
        }
    }
}
