package com.example.nexus.ui
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.api.LoginRequest
import com.example.nexus.api.RegisterRequest
import com.example.nexus.api.RetrofitClient
import com.example.nexus.api.toApiError
import com.example.nexus.api.toUserMessage
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
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        viewModelScope.launch {
            AuthSession.isAuthenticated.collect { authenticated ->
                _uiState.value = _uiState.value.copy(isAuthenticated = authenticated)
            }
        }
    }

    fun login(email: String, password: String) {
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
                RetrofitClient.instance.login(LoginRequest(email.trim(), password))
            }.onSuccess { response ->
                AuthSession.saveToken(response.token)
                _uiState.value = _uiState.value.copy(isLoading = false, isAuthenticated = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.toApiError().toUserMessage()
                )
            }
        }
    }

    fun register(email: String, password: String, displayName: String) {
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
                RetrofitClient.instance.register(RegisterRequest(email.trim(), password, displayName.trim()))
            }.onSuccess { response ->
                AuthSession.saveToken(response.token)
                _uiState.value = _uiState.value.copy(isLoading = false, isAuthenticated = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.toApiError().toUserMessage()
                )
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
        return email.matches(emailRegex)
    }
}
