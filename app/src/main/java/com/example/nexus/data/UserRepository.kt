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
) {
    open suspend fun login(request: LoginRequest): AuthResponse = apiService?.login(request) ?: throw IllegalStateException("No service")

    open suspend fun register(request: RegisterRequest): AuthResponse =
        apiService?.register(request) ?: throw IllegalStateException("No service")

    open suspend fun getProfile(): UserProfile = apiService?.getProfile() ?: throw IllegalStateException("No service")

    open suspend fun updateProfile(request: UpdateProfileRequest): UserProfile =
        apiService?.updateProfile(request) ?: throw IllegalStateException("No service")

    open suspend fun changePassword(request: ChangePasswordRequest) {
        apiService?.changePassword(request)
    }

    open suspend fun deleteAccount() {
        apiService?.deleteAccount()
    }
}
