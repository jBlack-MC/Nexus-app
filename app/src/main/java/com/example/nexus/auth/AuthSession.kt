package com.example.nexus.auth

import android.util.Base64
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AuthSession {
    /** Tokens minted by the offline fallback embed the account email. */
    const val OFFLINE_TOKEN_PREFIX = "offline_token_"

    private var tokenStore: TokenStore? = null
    private var onSessionCleared: (() -> Unit)? = null
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    fun initialize(store: TokenStore) {
        tokenStore = store
        _isAuthenticated.value = store.getToken() != null
    }

    /**
     * Registers a callback invoked whenever the local session ends or changes, so cached data
     * belonging to the previous account can be purged.
     */
    fun setOnSessionCleared(block: () -> Unit) {
        onSessionCleared = block
    }

    private fun getStore(): TokenStore {
        return tokenStore ?: throw IllegalStateException("AuthSession.initialize() was not called")
    }

    fun getToken(): String? = getStore().getToken()

    /**
     * Stable identifier for the signed-in account. Every cached row is stamped with this value so
     * one account can never read another account's cached data on a shared device.
     */
    fun cacheOwnerId(): String? = getToken()?.let(::cacheOwnerIdOf)

    fun saveToken(token: String) {
        val previousOwner = cacheOwnerId()
        getStore().saveToken(token)
        _isAuthenticated.value = true
        // A different account signing in must not inherit the previous account's cache.
        if (previousOwner != cacheOwnerId()) {
            onSessionCleared?.invoke()
        }
    }

    fun clearToken() {
        getStore().clearToken()
        _isAuthenticated.value = false
        onSessionCleared?.invoke()
    }

    private fun cacheOwnerIdOf(token: String): String? {
        if (token.startsWith(OFFLINE_TOKEN_PREFIX)) {
            return token.removePrefix(OFFLINE_TOKEN_PREFIX).trim().lowercase().ifBlank { null }
        }
        // Server JWTs carry a stable account id in the "sub" claim. The signature is intentionally
        // not verified here: this value only scopes a local cache and is never trusted as auth.
        val subject = runCatching {
            val payload = token.split('.').getOrNull(1) ?: return@runCatching null
            val json = String(
                Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            )
            JsonParser.parseString(json).asJsonObject.get("sub")?.asString
        }.getOrNull()
        return subject?.takeIf { it.isNotBlank() } ?: token.hashCode().toString()
    }
}
