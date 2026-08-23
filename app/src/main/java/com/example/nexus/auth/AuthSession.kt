package com.example.nexus.auth

object AuthSession {
    private var tokenStore: SecureTokenStore? = null

    fun initialize(store: SecureTokenStore) {
        tokenStore = store
    }

    fun saveToken(token: String) {
        tokenStore?.saveToken(token)
    }

    fun getToken(): String? = tokenStore?.getToken()

    fun clearToken() {
        tokenStore?.clearToken()
    }
}

