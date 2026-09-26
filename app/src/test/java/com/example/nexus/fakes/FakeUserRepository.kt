package com.example.nexus.fakes

import com.example.nexus.api.AuthResponse
import com.example.nexus.api.ChangePasswordRequest
import com.example.nexus.api.LoginRequest
import com.example.nexus.api.RegisterRequest
import com.example.nexus.api.UpdateProfileRequest
import com.example.nexus.api.UserProfile
import com.example.nexus.data.UserRepository

class FakeUserRepository(
    var loginResult: Result<AuthResponse> = Result.success(AuthResponse("fake-jwt-token")),
    var loginAction: (suspend (LoginRequest) -> AuthResponse)? = null,
    var registerResult: Result<AuthResponse> = Result.success(AuthResponse("fake-jwt-token")),
    var registerAction: (suspend (RegisterRequest) -> AuthResponse)? = null,
    var userProfile: UserProfile = UserProfile(email = "test@example.com", displayName = "Test User"),
    var getProfileError: Throwable? = null,
    var updateProfileError: Throwable? = null,
    var updateProfileResult: UserProfile? = null,
    var profileUpdater: (suspend (UpdateProfileRequest) -> UserProfile)? = null,
    var changePasswordError: Throwable? = null,
    var deleteAccountError: Throwable? = null,
) : UserRepository() {
    val loginRequests = mutableListOf<LoginRequest>()
    val registerRequests = mutableListOf<RegisterRequest>()
    val updateProfileRequests = mutableListOf<UpdateProfileRequest>()
    val changePasswordRequests = mutableListOf<ChangePasswordRequest>()
    var deleteAccountCalled = false

    override suspend fun login(request: LoginRequest): AuthResponse {
        loginRequests.add(request)
        loginAction?.let { return it(request) }
        return loginResult.getOrThrow()
    }

    override suspend fun register(request: RegisterRequest): AuthResponse {
        registerRequests.add(request)
        registerAction?.let { return it(request) }
        return registerResult.getOrThrow()
    }

    override suspend fun getProfile(): UserProfile {
        getProfileError?.let { throw it }
        return userProfile
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): UserProfile {
        updateProfileRequests.add(request)
        profileUpdater?.let { return it(request) }
        updateProfileError?.let { throw it }
        val updated =
            updateProfileResult ?: userProfile.copy(
                displayName = request.displayName ?: userProfile.displayName,
                language = request.language ?: userProfile.language,
                notificationsEnabled = request.notificationsEnabled ?: userProfile.notificationsEnabled,
            )
        userProfile = updated
        return updated
    }

    override suspend fun changePassword(request: ChangePasswordRequest) {
        changePasswordRequests.add(request)
        changePasswordError?.let { throw it }
    }

    override suspend fun deleteAccount() {
        deleteAccountCalled = true
        deleteAccountError?.let { throw it }
    }

    val loginActionLambda: suspend (LoginRequest) -> AuthResponse = { login(it) }
    val registerActionLambda: suspend (RegisterRequest) -> AuthResponse = { register(it) }
    val profileLoader: suspend () -> UserProfile = { getProfile() }
    val profileUpdaterLambda: suspend (UpdateProfileRequest) -> UserProfile = { updateProfile(it) }
    val passwordChanger: suspend (ChangePasswordRequest) -> Unit = { changePassword(it) }
    val accountDeleter: suspend () -> Unit = { deleteAccount() }
}
