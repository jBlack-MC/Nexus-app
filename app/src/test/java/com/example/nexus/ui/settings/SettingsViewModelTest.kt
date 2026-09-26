package com.example.nexus.ui.settings

import android.os.Looper
import com.example.nexus.auth.AuthSession
import com.example.nexus.fakes.FakeAuthSession
import com.example.nexus.fakes.FakeSettingsSession
import com.example.nexus.settings.SettingsSession
import com.example.nexus.settings.ThemeMode
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private lateinit var scheduler: TestCoroutineScheduler
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var fakeSettingsSession: FakeSettingsSession
    private lateinit var fakeAuthSession: FakeAuthSession

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        mockkStatic(Looper::class)
        val mockLooper = mockk<Looper>(relaxed = true)
        every { Looper.getMainLooper() } returns mockLooper

        scheduler = TestCoroutineScheduler()
        testDispatcher = StandardTestDispatcher(scheduler)
        Dispatchers.setMain(testDispatcher)

        fakeSettingsSession =
            FakeSettingsSession(
                initialThemeMode = ThemeMode.SYSTEM,
                initialNotificationsEnabled = true,
            )
        fakeSettingsSession.applyToMock()

        fakeAuthSession = FakeAuthSession()
        fakeAuthSession.applyToMock()

        viewModel = SettingsViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun initialState_reflectsSettingsSession() =
        runTest(scheduler) {
            advanceUntilIdle()

            assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)
            assertEquals(true, viewModel.uiState.value.notificationsEnabled)
        }

    @Test
    fun setThemeMode_updatesUiStateAndPersistsChoice() =
        runTest(scheduler) {
            viewModel.setThemeMode(ThemeMode.DARK)
            advanceUntilIdle()

            assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
            verify(exactly = 1) { SettingsSession.setThemeMode(ThemeMode.DARK) }
        }

    @Test
    fun setNotificationsEnabled_updatesUiState() =
        runTest(scheduler) {
            viewModel.setNotificationsEnabled(false)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.notificationsEnabled)
            verify(exactly = 1) { SettingsSession.setNotificationsEnabled(false) }
        }

    @Test
    fun logout_clearsAuthSessionToken() =
        runTest(scheduler) {
            viewModel.logout()
            advanceUntilIdle()

            verify(exactly = 1) { AuthSession.clearToken() }
        }
}
