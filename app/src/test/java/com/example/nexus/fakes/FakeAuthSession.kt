package com.example.nexus.fakes

import com.example.nexus.auth.AuthSession
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAuthSession(
    initialIsAuthenticated: Boolean = false,
    initialToken: String? = if (initialIsAuthenticated) "fake-token" else null,
) {
    private val _isAuthenticated = MutableStateFlow(initialIsAuthenticated)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    var savedToken: String? = initialToken
        private set

    var isCleared: Boolean = false
        private set

    fun saveToken(token: String) {
        savedToken = token
        _isAuthenticated.value = true
        isCleared = false
    }

    fun clearToken() {
        savedToken = null
        _isAuthenticated.value = false
        isCleared = true
    }

    fun applyToMock() {
        mockkObject(AuthSession)
        every { AuthSession.isAuthenticated } returns isAuthenticated
        every { AuthSession.getToken() } answers { savedToken }
        every { AuthSession.saveToken(any()) } answers { saveToken(firstArg()) }
        every { AuthSession.clearToken() } answers { clearToken() }
    }
}
