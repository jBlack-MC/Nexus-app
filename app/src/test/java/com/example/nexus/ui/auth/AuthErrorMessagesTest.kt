package com.example.nexus.ui.auth

import android.os.Looper
import com.example.nexus.api.AuthResponse
import com.example.nexus.auth.AuthSession
import com.example.nexus.fakes.FakeAuthSession
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** AUD-18: sign-in and registration failures explain the real cause; Google SSO hands its token to the API. */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthErrorMessagesTest {
    private val scheduler = TestCoroutineScheduler()

    @Before
    fun setUp() {
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mockk(relaxed = true)
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        FakeAuthSession().applyToMock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun http(code: Int) = HttpException(Response.error<Any>(code, "{}".toResponseBody("application/json".toMediaType())))

    private fun loginFailingWith(code: Int): String? {
        val viewModel = AuthViewModel(loginAction = { throw http(code) }, onSignedIn = {})
        viewModel.login("person@example.com", "secret123")
        scheduler.advanceUntilIdle()
        return viewModel.uiState.value.errorMessage
    }

    @Test
    fun wrongPassword_saysCredentialsAreIncorrect_notSessionExpired() =
        runTest(scheduler) { assertEquals(AuthViewModel.MSG_WRONG_CREDENTIALS, loginFailingWith(401)) }

    @Test
    fun rateLimited_saysToWait() = runTest(scheduler) { assertEquals(AuthViewModel.MSG_TOO_MANY_ATTEMPTS, loginFailingWith(429)) }

    @Test
    fun duplicateEmail_onRegister_saysAccountExists() = runTest(scheduler) {
        val viewModel = AuthViewModel(registerAction = { throw http(409) }, onSignedIn = {})
        viewModel.register("person@example.com", "secret123", "Person")
        advanceUntilIdle()
        assertEquals(AuthViewModel.MSG_EMAIL_TAKEN, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun googleSignIn_sendsTokenToApi_andSignsIn() = runTest(scheduler) {
        var sentToken: String? = null
        var signedInHook = false
        val viewModel =
            AuthViewModel(
                googleSignInAction = { token ->
                    sentToken = token
                    AuthResponse("nexus-jwt")
                },
                onSignedIn = { signedInHook = true },
            )
        viewModel.googleSignIn("google-id-token")
        advanceUntilIdle()
        assertEquals("google-id-token", sentToken)
        assertTrue(viewModel.uiState.value.isAuthenticated)
        assertTrue(signedInHook)
        verify(exactly = 1) { AuthSession.saveToken("nexus-jwt") }
    }

    @Test
    fun googleSignIn_serverWithoutSso_explainsIt() = runTest(scheduler) {
        val viewModel = AuthViewModel(googleSignInAction = { throw http(503) }, onSignedIn = {})
        viewModel.googleSignIn("google-id-token")
        advanceUntilIdle()
        assertEquals(AuthViewModel.MSG_GOOGLE_NOT_CONFIGURED, viewModel.uiState.value.errorMessage)
    }
}
