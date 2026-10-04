package com.example.nexus.data

import com.example.nexus.api.ApiService
import com.example.nexus.api.AuthResponse
import com.example.nexus.api.ChangePasswordRequest
import com.example.nexus.api.LoginRequest
import com.example.nexus.api.RegisterRequest
import com.example.nexus.api.UpdateProfileRequest
import com.example.nexus.api.UserProfile

open class UserRepository(
    private val apiService: ApiService? = null,
    private val preferences: android.content.SharedPreferences? = null,
    private val cacheOwner: () -> String? = { null },
    private val onAccountDeleted: suspend (String) -> Unit = {},
) {
    open suspend fun login(request: LoginRequest): AuthResponse = apiService?.login(request) ?: throw IllegalStateException("No service")

    open suspend fun register(request: RegisterRequest): AuthResponse =
        apiService?.register(request) ?: throw IllegalStateException("No service")

    open suspend fun getProfile(): UserProfile {
        val key = cacheOwner()?.let { "profile:$it" }
        return try {
            val profile = apiService?.getProfile() ?: throw IllegalStateException("No service")
            if (key != null) preferences?.edit()?.putString(key, com.google.gson.Gson().toJson(profile))?.apply()
            profile
        } catch (error: java.io.IOException) {
            val saved = key?.let { preferences?.getString(it, null) } ?: throw error
            com.google.gson.Gson().fromJson(saved, UserProfile::class.java)
        }
    }

    open suspend fun updateProfile(request: UpdateProfileRequest): UserProfile =
        apiService?.updateProfile(request) ?: throw IllegalStateException("No service")

    open suspend fun changePassword(request: ChangePasswordRequest) {
        apiService?.changePassword(request)
    }

    open suspend fun deleteAccount() {
        val owner = cacheOwner()
        (apiService ?: error("No service")).deleteAccount()
        if (owner != null) {
            preferences?.edit()?.remove("profile:$owner")?.apply()
            onAccountDeleted(owner)
        }
    }
}
