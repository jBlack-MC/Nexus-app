package com.example.nexus.auth

import com.example.nexus.util.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AuthSession {
    private var tokenManager: TokenManager? = null
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    fun initialize(manager: TokenManager) {
        tokenManager = manager
        _isAuthenticated.value = manager.getToken() != null
    }

    fun getToken(): String? {
        val manager = tokenManager ?: throw IllegalStateException("AuthSession.initialize() was not called")
        return manager.getToken()
    }

    fun saveToken(token: String) {
        tokenManager?.saveToken(token)
        _isAuthenticated.value = true
    }

    fun clearToken() {
        tokenManager?.clearToken()
        _isAuthenticated.value = false
    }
}
