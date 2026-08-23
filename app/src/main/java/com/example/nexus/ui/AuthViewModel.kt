package com.example.nexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.api.LoginRequest
import com.example.nexus.api.RegisterRequest
import com.example.nexus.api.RetrofitClient
import com.example.nexus.auth.AuthSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false
)

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        AuthUiState(isAuthenticated = !AuthSession.getToken().isNullOrBlank())
    )
    val uiState: StateFlow<AuthUiState> = _uiState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            runCatching {
                RetrofitClient.instance.login(LoginRequest(email, password))
            }.onSuccess { response ->
                AuthSession.saveToken(response.token)
                _uiState.value = AuthUiState(isAuthenticated = true)
            }.onFailure { error ->
                _uiState.value = AuthUiState(errorMessage = error.message ?: "Login failed")
            }
        }
    }

    fun register(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            runCatching {
                RetrofitClient.instance.register(RegisterRequest(email, password, displayName))
            }.onSuccess { response ->
                AuthSession.saveToken(response.token)
                _uiState.value = AuthUiState(isAuthenticated = true)
            }.onFailure { error ->
                _uiState.value = AuthUiState(errorMessage = error.message ?: "Registration failed")
            }
        }
    }

}

