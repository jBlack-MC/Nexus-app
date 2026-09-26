package com.example.nexus.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.NexusApp
import com.example.nexus.api.ApiError
import com.example.nexus.api.ChangePasswordRequest
import com.example.nexus.api.Habit
import com.example.nexus.api.UpdateProfileRequest
import com.example.nexus.api.UserProfile
import com.example.nexus.api.toApiError
import com.example.nexus.api.toUserMessage
import com.example.nexus.auth.AuthSession
import com.example.nexus.data.HabitRepository
import com.example.nexus.data.UserRepository
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
    val csvExport: String? = null,
)

class SettingsViewModel(
    private val userRepository: UserRepository? = null,
    private val habitRepository: HabitRepository? = null,
    private val profileLoader: suspend () -> UserProfile = { (userRepository ?: NexusApp.userRepository).getProfile() },
    private val profileUpdater: suspend (
        UpdateProfileRequest,
    ) -> UserProfile = { (userRepository ?: NexusApp.userRepository).updateProfile(it) },
    private val passwordChanger: suspend (
        ChangePasswordRequest,
    ) -> Unit = { (userRepository ?: NexusApp.userRepository).changePassword(it) },
    private val accountDeleter: suspend () -> Unit = { (userRepository ?: NexusApp.userRepository).deleteAccount() },
    private val habitsLoader: suspend () -> List<Habit> = { (habitRepository ?: NexusApp.habitRepository).getHabits() },
    private val themeModeFlow: StateFlow<ThemeMode> = SettingsSession.themeMode,
    private val notificationsEnabledFlow: StateFlow<Boolean> = SettingsSession.notificationsEnabled,
    private val languageFlow: StateFlow<String> = SettingsSession.language,
    private val languageSyncPendingFlow: StateFlow<Boolean> = SettingsSession.languageSyncPending,
    private val setLanguageSession: (String) -> Unit = { SettingsSession.setLanguage(it) },
    private val setThemeModeSession: (ThemeMode) -> Unit = { SettingsSession.setThemeMode(it) },
    private val setNotificationsEnabledSession: (Boolean) -> Unit = { SettingsSession.setNotificationsEnabled(it) },
    private val markLanguageSyncPending: () -> Unit = { SettingsSession.markLanguageSyncPending() },
    private val clearLanguageSyncPending: () -> Unit = { SettingsSession.clearLanguageSyncPending() },
    private val clearSession: () -> Unit = { AuthSession.clearToken() },
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            themeModeFlow.collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
        viewModelScope.launch {
            notificationsEnabledFlow.collect { enabled ->
                _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
            }
        }
        viewModelScope.launch {
            languageFlow.collect { language ->
                _uiState.value = _uiState.value.copy(selectedLanguage = language)
            }
        }
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                profileLoader()
            }.onSuccess { profile ->
                val syncPending = languageSyncPendingFlow.value
                if (!syncPending) {
                    setLanguageSession(profile.language)
                }
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        profile = profile,
                        selectedLanguage = if (syncPending) languageFlow.value else profile.language,
                        notificationsEnabled = profile.notificationsEnabled,
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
                profileUpdater(UpdateProfileRequest(displayName = displayName.trim()))
            }.onSuccess { profile ->
                if (!languageSyncPendingFlow.value) {
                    setLanguageSession(profile.language)
                }
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        profile = profile,
                        successMessage = "Profile updated successfully!",
                    )
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        setThemeModeSession(mode)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        setNotificationsEnabledSession(enabled)
        updatePreferences(notificationsEnabled = enabled)
    }

    fun setLanguage(language: String) {
        if (language == _uiState.value.selectedLanguage) return
        setLanguageSession(language)
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
        updatePreferences(language = language)
    }

    fun logout() {
        clearSession()
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
    ) {
        if (currentPassword.isBlank() || newPassword.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Use a current password and a new password of at least 6 characters")
            return
        }
        viewModelScope.launch {
            runCatching { passwordChanger(ChangePasswordRequest(currentPassword, newPassword)) }
                .onSuccess { _uiState.value = _uiState.value.copy(successMessage = "Password changed successfully") }
                .onFailure(::handleError)
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            runCatching { accountDeleter() }
                .onSuccess { clearSession() }
                .onFailure(::handleError)
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            runCatching { habitsLoader() }
                .onSuccess { habits ->
                    val rows =
                        habits.flatMap { habit ->
                            habit.completedDates.map { date -> "${habit.name},${habit.frequency},$date" }
                        }
                    _uiState.value =
                        _uiState.value.copy(
                            csvExport = "habit,frequency,completed_date\n${rows.joinToString("\n")}",
                            successMessage = "CSV export ready",
                        )
                }
                .onFailure(::handleError)
        }
    }

    private fun handleError(error: Throwable) {
        val apiError = error.toApiError()
        if (apiError is ApiError.SessionExpired) {
            clearSession()
        }
        _uiState.value =
            _uiState.value.copy(
                isLoading = false,
                errorMessage = apiError.toUserMessage(),
            )
    }

    private fun updatePreferences(
        language: String? = null,
        notificationsEnabled: Boolean? = null,
    ) {
        viewModelScope.launch {
            runCatching {
                profileUpdater(
                    UpdateProfileRequest(language = language, notificationsEnabled = notificationsEnabled),
                )
            }.onSuccess { profile ->
                if (language != null) {
                    clearLanguageSyncPending()
                }
                val syncPending = languageSyncPendingFlow.value
                _uiState.value =
                    _uiState.value.copy(
                        profile = profile,
                        selectedLanguage = if (syncPending) languageFlow.value else profile.language,
                        notificationsEnabled = profile.notificationsEnabled,
                    )
            }.onFailure { error ->
                if (language != null) {
                    markLanguageSyncPending()
                }
                handleError(error)
            }
        }
    }
}
