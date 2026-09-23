package com.example.nexus.auth

import android.content.Context
import com.example.nexus.NexusApp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * An account that can sign in while the backend is unreachable.
 *
 * Passwords are never stored in any form: only a per-account random salt and a salted SHA-256
 * digest are persisted. This is a deliberate simplification for a local demo fallback; a
 * memory-hard KDF such as Argon2id or bcrypt would be preferred if offline accounts were ever
 * a production feature.
 */
data class LocalUser(
    val email: String,
    val displayName: String,
    val passwordSalt: String,
    val passwordHash: String
)

object LocalAccountManager {
    private const val PREFS_NAME = "nexus_local_accounts"
    private const val KEY_USERS = "registered_users"
    private const val SALT_BYTES = 16
    private val gson = Gson()

    /**
     * Development fixtures for offline demos. Only the salted digests are compiled in; the
     * matching passwords are documented in README.md under "Test credentials" and are for local
     * development only. These accounts must never be provisioned in a production environment.
     */
    private val preSeededUsers = listOf(
        LocalUser(
            email = "admin@nexus-app.com",
            displayName = "Nexus Admin",
            passwordSalt = "nexus-dev-salt-admin",
            passwordHash = "c87da66703560c8a3d05da1a92471860ca9bc7b20694f23afaba2c62cd08891c"
        ),
        LocalUser(
            email = "jane.doe@nexus-app.com",
            displayName = "Jane Doe",
            passwordSalt = "nexus-dev-salt-jane",
            passwordHash = "2ad6bb1ca442f3477ae75720bc8b8c5f0344039955163fb7ed0e7fdbc2dfc8b9"
        ),
        LocalUser(
            email = "test.user@nexus-app.com",
            displayName = "Test User",
            passwordSalt = "nexus-dev-salt-test",
            passwordHash = "875ede86093ee2e16031c4f99607ed98d10ea151b0a672b420eb972065daadc4"
        )
    )

    private val prefs by lazy {
        NexusApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun authenticate(email: String, password: String): LocalUser? {
        val normalizedEmail = email.trim().lowercase()
        val candidate = preSeededUsers.find { it.email.lowercase() == normalizedEmail }
            ?: getCustomUsers().find { it.email.lowercase() == normalizedEmail }
            ?: return null

        // Constant-time comparison avoids leaking how much of a digest matched.
        return candidate.takeIf { MessageDigest.isEqual(it.passwordHash.toByteArray(), hash(password, it.passwordSalt).toByteArray()) }
    }

    fun register(email: String, displayName: String, password: String): Boolean {
        val normalizedEmail = email.trim().lowercase()
        // Ensure no duplicates in pre-seeded or custom users
        if (preSeededUsers.any { it.email.lowercase() == normalizedEmail }) return false

        val currentUsers = getCustomUsers().toMutableList()
        if (currentUsers.any { it.email.lowercase() == normalizedEmail }) return false

        val salt = newSalt()
        currentUsers.add(
            LocalUser(
                email = normalizedEmail,
                displayName = displayName.trim(),
                passwordSalt = salt,
                passwordHash = hash(password, salt)
            )
        )
        prefs.edit().putString(KEY_USERS, gson.toJson(currentUsers)).apply()
        return true
    }

    fun getUserByEmail(email: String): LocalUser? {
        val normalizedEmail = email.trim().lowercase()
        return preSeededUsers.find { it.email.lowercase() == normalizedEmail }
            ?: getCustomUsers().find { it.email.lowercase() == normalizedEmail }
    }

    private fun newSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hash(password: String, salt: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest((salt + password).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun getCustomUsers(): List<LocalUser> {
        val json = prefs.getString(KEY_USERS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<LocalUser>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
