package com.example.nexus.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AuthSession {
    private var tokenStore: TokenStore? = null
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    fun initialize(store: TokenStore) {
        tokenStore = store
        _isAuthenticated.value = store.getToken() != null
    }

    private fun getStore(): TokenStore {
        return tokenStore ?: throw IllegalStateException("AuthSession.initialize() was not called")
    }

    fun getToken(): String? = getStore().getToken()

    fun saveToken(token: String) {
        getStore().saveToken(token)
        _isAuthenticated.value = true
    }

    fun clearToken() {
        getStore().clearToken()
        _isAuthenticated.value = false
    }
}
