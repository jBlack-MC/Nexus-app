package com.example.nexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.auth.AuthSession
import com.example.nexus.settings.SettingsSession
import com.example.nexus.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true
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
    }

    fun setThemeMode(mode: ThemeMode) {
        SettingsSession.setThemeMode(mode)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        SettingsSession.setNotificationsEnabled(enabled)
    }

    fun logout() {
        AuthSession.clearToken()
    }
}
