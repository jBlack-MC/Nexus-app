package com.example.nexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.NexusApp
import com.example.nexus.api.AppConfig
import com.example.nexus.api.toApiError
import com.example.nexus.api.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RemoteConfigUiState(
    val config: AppConfig = AppConfig(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class RemoteConfigViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RemoteConfigUiState())
    val uiState: StateFlow<RemoteConfigUiState> = _uiState

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { NexusApp.repository.getAppConfig() }
                .onSuccess { config ->
                    _uiState.value = RemoteConfigUiState(config = config, isLoading = false)
                }
                .onFailure { error ->
                    // Keep the last successfully loaded config; surface why the refresh failed.
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.toApiError().toUserMessage()
                    )
                }
        }
    }
}
