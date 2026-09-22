package com.example.nexus.auth

interface TokenStore {
    fun saveToken(token: String)
    fun getToken(): String?
    fun clearToken()
}
