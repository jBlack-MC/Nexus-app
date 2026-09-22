package com.example.nexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.NexusApp
import com.example.nexus.api.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RemoteConfigUiState(
    val config: AppConfig = AppConfig(),
    val isLoading: Boolean = true
)

class RemoteConfigViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RemoteConfigUiState())
    val uiState: StateFlow<RemoteConfigUiState> = _uiState

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            _uiState.value = RemoteConfigUiState(NexusApp.repository.getAppConfig(), false)
        }
    }
}
