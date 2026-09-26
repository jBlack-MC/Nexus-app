package com.example.nexus.ui.auth

import android.os.Looper
import com.example.nexus.api.AuthResponse
import com.example.nexus.auth.AuthSession
import com.example.nexus.fakes.FakeAuthSession
import com.example.nexus.fakes.FakeUserRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private lateinit var scheduler: TestCoroutineScheduler
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var fakeAuthSession: FakeAuthSession
    private lateinit var fakeUserRepository: FakeUserRepository

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        mockkStatic(Looper::class)
        val mockLooper = mockk<Looper>(relaxed = true)
        every { Looper.getMainLooper() } returns mockLooper

        scheduler = TestCoroutineScheduler()
        testDispatcher = StandardTestDispatcher(scheduler)
        Dispatchers.setMain(testDispatcher)

        fakeAuthSession = FakeAuthSession()
        fakeAuthSession.applyToMock()

        fakeUserRepository = FakeUserRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun login_success_updatesStateToAuthenticatedAndSavesToken() =
        runTest(scheduler) {
            val email = "test@example.com"
            val password = "password123"
            val token = "mock-jwt-token"

            fakeUserRepository.loginAction = { request ->
                assertEquals(email, request.email)
                assertEquals(password, request.password)
                AuthResponse(token)
            }

            viewModel =
                AuthViewModel(
                    loginAction = fakeUserRepository.loginActionLambda,
                )
            viewModel.login(email, password)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.errorMessage)
            assertTrue(viewModel.uiState.value.isAuthenticated)
            verify(exactly = 1) { AuthSession.saveToken(token) }
        }

    @Test
    fun login_invalidEmail_setsValidationError() =
        runTest(scheduler) {
            viewModel = AuthViewModel()
            viewModel.login("invalid-email", "password123")

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals("Please enter a valid email address", viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isAuthenticated)
        }

    @Test
    fun login_shortPassword_setsValidationError() =
        runTest(scheduler) {
            viewModel = AuthViewModel()
            viewModel.login("test@example.com", "12345")

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals("Password must be at least 6 characters", viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isAuthenticated)
        }

    @Test
    fun login_apiFailure_surfacesErrorMessage() =
        runTest(scheduler) {
            val email = "test@example.com"
            val password = "password123"

            fakeUserRepository.loginResult = Result.failure(IOException("Network Error"))

            viewModel =
                AuthViewModel(
                    loginAction = fakeUserRepository.loginActionLambda,
                )
            viewModel.login(email, password)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals("No internet connection. Check your network and try again.", viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isAuthenticated)
        }

    @Test
    fun register_success_updatesStateToAuthenticatedAndSavesToken() =
        runTest(scheduler) {
            val email = "test@example.com"
            val password = "password123"
            val displayName = "Test User"
            val token = "mock-jwt-token"

            fakeUserRepository.registerAction = { request ->
                assertEquals(email, request.email)
                assertEquals(password, request.password)
                assertEquals(displayName, request.displayName)
                AuthResponse(token)
            }

            viewModel =
                AuthViewModel(
                    registerAction = fakeUserRepository.registerActionLambda,
                )
            viewModel.register(email, password, displayName)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.errorMessage)
            assertTrue(viewModel.uiState.value.isAuthenticated)
            verify(exactly = 1) { AuthSession.saveToken(token) }
        }

    @Test
    fun register_blankDisplayName_setsValidationError() =
        runTest(scheduler) {
            viewModel = AuthViewModel()
            viewModel.register("test@example.com", "password123", "   ")

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals("Please enter a display name", viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isAuthenticated)
        }

    @Test
    fun register_invalidEmail_setsValidationError() =
        runTest(scheduler) {
            viewModel = AuthViewModel()
            viewModel.register("invalid-email", "password123", "Test User")

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals("Please enter a valid email address", viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isAuthenticated)
        }

    @Test
    fun register_shortPassword_setsValidationError() =
        runTest(scheduler) {
            viewModel = AuthViewModel()
            viewModel.register("test@example.com", "12345", "Test User")

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals("Password must be at least 6 characters", viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isAuthenticated)
        }

    @Test
    fun register_apiFailure_surfacesErrorMessage() =
        runTest(scheduler) {
            val email = "test@example.com"
            val password = "password123"
            val displayName = "Test User"

            fakeUserRepository.registerResult = Result.failure(IOException("Network Error"))

            viewModel =
                AuthViewModel(
                    registerAction = fakeUserRepository.registerActionLambda,
                )
            viewModel.register(email, password, displayName)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals("No internet connection. Check your network and try again.", viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isAuthenticated)
        }
}
