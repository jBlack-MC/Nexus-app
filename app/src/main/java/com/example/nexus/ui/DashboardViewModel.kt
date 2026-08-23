package com.example.nexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.api.DashboardData
import com.example.nexus.api.RetrofitClient
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
            try {
                val data = RetrofitClient.instance.getDashboard()
                _uiState.value = DashboardState.Success(data)
            } catch (e: Exception) {
                _uiState.value = DashboardState.Error(e.message ?: "Unknown Error")
            }
        }
    }
}