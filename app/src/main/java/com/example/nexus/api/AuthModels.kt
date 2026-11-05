package com.example.nexus.api

data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
)

data class LoginRequest(
    val email: String,
    val password: String,
)

data class AuthResponse(
    val token: String,
)

data class UserProfile(
    val email: String,
    val displayName: String,
    val language: String = "en",
    val notificationsEnabled: Boolean = true,
)

data class UpdateProfileRequest(
    val displayName: String? = null,
    val language: String? = null,
    val notificationsEnabled: Boolean? = null,
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)

/** Google ID token obtained from Credential Manager, exchanged for a Nexus JWT by the API. */
data class GoogleSignInRequest(
    val idToken: String,
)

/** Firebase Cloud Messaging registration token for this device. */
data class DeviceTokenRequest(
    val token: String,
)

data class TestNotificationResult(
    val configured: Boolean = false,
    val sent: Int = 0,
    val devices: Int = 0,
)
