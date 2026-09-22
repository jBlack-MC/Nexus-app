package com.example.nexus.auth

import android.content.Context
import com.example.nexus.NexusApp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class LocalUser(
    val email: String,
    val password: String,
    val displayName: String
)

object LocalAccountManager {
    private const val PREFS_NAME = "nexus_local_accounts"
    private const val KEY_USERS = "registered_users"
    private val gson = Gson()

    private val preSeededUsers = listOf(
        LocalUser("admin@nexus-app.com", "nexusAdmin123", "Nexus Admin"),
        LocalUser("jane.doe@nexus-app.com", "janeDoe789", "Jane Doe"),
        LocalUser("test.user@nexus-app.com", "testUser456", "Test User")
    )

    private val prefs by lazy {
        NexusApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun authenticate(email: String, password: String): LocalUser? {
        val normalizedEmail = email.trim().lowercase()
        // Check pre-seeded first
        val preSeeded = preSeededUsers.find { it.email.lowercase() == normalizedEmail && it.password == password }
        if (preSeeded != null) return preSeeded

        // Check dynamically registered local users
        return getCustomUsers().find { it.email.lowercase() == normalizedEmail && it.password == password }
    }

    fun register(user: LocalUser): Boolean {
        val normalizedEmail = user.email.trim().lowercase()
        // Ensure no duplicates in pre-seeded or custom users
        if (preSeededUsers.any { it.email.lowercase() == normalizedEmail }) return false
        
        val currentUsers = getCustomUsers().toMutableList()
        if (currentUsers.any { it.email.lowercase() == normalizedEmail }) return false

        currentUsers.add(user)
        prefs.edit().putString(KEY_USERS, gson.toJson(currentUsers)).apply()
        return true
    }

    fun getUserByEmail(email: String): LocalUser? {
        val normalizedEmail = email.trim().lowercase()
        return preSeededUsers.find { it.email.lowercase() == normalizedEmail }
            ?: getCustomUsers().find { it.email.lowercase() == normalizedEmail }
    }

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
