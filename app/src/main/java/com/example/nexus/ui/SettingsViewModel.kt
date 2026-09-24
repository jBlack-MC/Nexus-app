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
    val successMessage: String? = null,
    val csvExport: String? = null
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
        viewModelScope.launch {
            SettingsSession.language.collect { language ->
                _uiState.value = _uiState.value.copy(selectedLanguage = language)
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
                // A pre-auth language pick whose server push failed is still pending; applying
                // profile.language now would silently revert the user's choice, so keep local.
                val languageSyncPending = SettingsSession.languageSyncPending.value
                if (!languageSyncPending) {
                    SettingsSession.setLanguage(profile.language)
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profile = profile,
                    selectedLanguage = if (languageSyncPending) SettingsSession.language.value else profile.language,
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
                // Same guard as loadProfile: don't let the response's language revert a
                // pending pre-auth pick.
                if (!SettingsSession.languageSyncPending.value) {
                    SettingsSession.setLanguage(profile.language)
                }
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
        SettingsSession.setLanguage(language)
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
        updatePreferences(language = language)
    }

    fun logout() {
        AuthSession.clearToken()
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        if (currentPassword.isBlank() || newPassword.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Use a current password and a new password of at least 6 characters")
            return
        }
        viewModelScope.launch {
            runCatching { NexusApp.repository.changePassword(com.example.nexus.api.ChangePasswordRequest(currentPassword, newPassword)) }
                .onSuccess { _uiState.value = _uiState.value.copy(successMessage = "Password changed successfully") }
                .onFailure(::handleError)
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            runCatching { NexusApp.repository.deleteAccount() }
                .onSuccess { AuthSession.clearToken() }
                .onFailure(::handleError)
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            runCatching { NexusApp.repository.getHabits() }
                .onSuccess { habits ->
                    val rows = habits.flatMap { habit ->
                        habit.completedDates.map { date -> "${habit.name},${habit.frequency},$date" }
                    }
                    _uiState.value = _uiState.value.copy(
                        csvExport = "habit,frequency,completed_date\n${rows.joinToString("\n")}",
                        successMessage = "CSV export ready"
                    )
                }
                .onFailure(::handleError)
        }
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
                if (language != null) {
                    // Server accepted the local pick — nothing pending anymore.
                    SettingsSession.clearLanguageSyncPending()
                }
                val languageSyncPending = SettingsSession.languageSyncPending.value
                _uiState.value = _uiState.value.copy(
                    profile = profile,
                    selectedLanguage = if (languageSyncPending) SettingsSession.language.value else profile.language,
                    notificationsEnabled = profile.notificationsEnabled
                )
            }.onFailure { error ->
                if (language != null) {
                    // Push failed: flag it so loadProfile/updateProfile won't revert the pick.
                    SettingsSession.markLanguageSyncPending()
                }
                handleError(error)
            }
        }
    }
}
