package com.example.nexus.ui

import android.os.Looper
import com.example.nexus.api.ApiService
import com.example.nexus.api.AuthResponse
import com.example.nexus.api.LoginRequest
import com.example.nexus.api.RegisterRequest
import com.example.nexus.api.RetrofitClient
import com.example.nexus.auth.AuthSession
import io.mockk.*
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var scheduler: TestCoroutineScheduler
    private lateinit var testDispatcher: TestDispatcher
    private val apiService = mockk<ApiService>()
    private val isAuthenticatedFlow = MutableStateFlow(false)

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        mockkStatic(Looper::class)
        val mockLooper = mockk<Looper>(relaxed = true)
        every { Looper.getMainLooper() } returns mockLooper

        scheduler = TestCoroutineScheduler()
        testDispatcher = StandardTestDispatcher(scheduler)
        Dispatchers.setMain(testDispatcher)
        
        mockkObject(RetrofitClient)
        every { RetrofitClient.instance } returns apiService

        mockkObject(AuthSession)
        every { AuthSession.isAuthenticated } returns isAuthenticatedFlow.asStateFlow()
        every { AuthSession.saveToken(any()) } answers {
            isAuthenticatedFlow.value = true
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        isAuthenticatedFlow.value = false
    }

    @Test
    fun login_success_updatesStateToAuthenticatedAndSavesToken() = runTest(scheduler) {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        val token = "mock-jwt-token"
        
        coEvery { apiService.login(LoginRequest(email, password)) } returns AuthResponse(token)

        // Act
        viewModel = AuthViewModel()
        viewModel.login(email, password)
        
        // Execute pending coroutines
        advanceUntilIdle()

        // Assert final state
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.isAuthenticated)
        
        verify(exactly = 1) { AuthSession.saveToken(token) }
    }

    @Test
    fun login_invalidEmail_setsValidationError() = runTest(scheduler) {
        // Act
        viewModel = AuthViewModel()
        viewModel.login("invalid-email", "password123")

        // Assert
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Please enter a valid email address", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun login_shortPassword_setsValidationError() = runTest(scheduler) {
        // Act
        viewModel = AuthViewModel()
        viewModel.login("test@example.com", "12345")

        // Assert
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Password must be at least 6 characters", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun login_apiFailure_surfacesErrorMessage() = runTest(scheduler) {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        
        coEvery { apiService.login(LoginRequest(email, password)) } throws IOException("Network Error")

        // Act
        viewModel = AuthViewModel()
        viewModel.login(email, password)
        
        advanceUntilIdle()

        // Assert
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("No internet connection. Check your network and try again.", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun register_success_updatesStateToAuthenticatedAndSavesToken() = runTest(scheduler) {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        val displayName = "Test User"
        val token = "mock-jwt-token"
        
        coEvery { apiService.register(RegisterRequest(email, password, displayName)) } returns AuthResponse(token)

        // Act
        viewModel = AuthViewModel()
        viewModel.register(email, password, displayName)
        
        // Execute pending coroutines
        advanceUntilIdle()

        // Assert final state
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.isAuthenticated)
        
        verify(exactly = 1) { AuthSession.saveToken(token) }
    }

    @Test
    fun register_blankDisplayName_setsValidationError() = runTest(scheduler) {
        // Act
        viewModel = AuthViewModel()
        viewModel.register("test@example.com", "password123", "   ")

        // Assert
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Please enter a display name", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun register_invalidEmail_setsValidationError() = runTest(scheduler) {
        // Act
        viewModel = AuthViewModel()
        viewModel.register("invalid-email", "password123", "Test User")

        // Assert
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Please enter a valid email address", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun register_shortPassword_setsValidationError() = runTest(scheduler) {
        // Act
        viewModel = AuthViewModel()
        viewModel.register("test@example.com", "12345", "Test User")

        // Assert
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Password must be at least 6 characters", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun register_apiFailure_surfacesErrorMessage() = runTest(scheduler) {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        val displayName = "Test User"
        
        coEvery { apiService.register(RegisterRequest(email, password, displayName)) } throws IOException("Network Error")

        // Act
        viewModel = AuthViewModel()
        viewModel.register(email, password, displayName)
        
        advanceUntilIdle()

        // Assert
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("No internet connection. Check your network and try again.", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isAuthenticated)
    }
}
