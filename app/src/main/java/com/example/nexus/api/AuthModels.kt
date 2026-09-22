package com.example.nexus.api

data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String
)

data class UserProfile(
    val email: String,
    val displayName: String
)

data class UpdateProfileRequest(
    val displayName: String
)

