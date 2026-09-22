package com.example.nexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.NexusApp
import com.example.nexus.api.ApiError
import com.example.nexus.api.DashboardData
import com.example.nexus.api.toApiError
import com.example.nexus.api.toUserMessage
import com.example.nexus.auth.AuthSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class DashboardState {
    data object Loading : DashboardState()
    data class Success(val data: DashboardData) : DashboardState()
    data class Error(val message: String) : DashboardState()
}

class DashboardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val uiState: StateFlow<DashboardState> = _uiState

    init {
        fetchDashboard()
    }

    fun fetchDashboard() {
        viewModelScope.launch {
            _uiState.value = DashboardState.Loading
            runCatching {
                NexusApp.repository.getDashboard()
            }.onSuccess { data ->
                _uiState.value = DashboardState.Success(data)
            }.onFailure { error ->
                val apiError = error.toApiError()
                if (apiError is ApiError.SessionExpired) {
                    AuthSession.clearToken()
                }
                _uiState.value = DashboardState.Error(apiError.toUserMessage())
            }
        }
    }
}
