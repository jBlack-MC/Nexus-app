package com.example.nexus.ui

import android.animation.ValueAnimator
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nexus.auth.AuthSession
import com.example.nexus.ui.navigation.NexusNavHost
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test suite verifying the SplashIntroScreen flow and its deterministic navigation.
 */
@RunWith(AndroidJUnit4::class)
class SplashIntroScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        // Reset/clear authentication status before each test to ensure a deterministic starting state
        AuthSession.clearToken()
    }

    @Test
    fun testSplashVisibleAtLaunchAndNavigatesToLoginWhenUnauthenticated() {
        // 1. Control the Compose clock to verify the initial state before animations/LaunchedEffects run
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            NexusNavHost()
        }

        // Constraint 1: Verify that the Splash screen is visible at initial app launch.
        // The text "Nexus" from SplashIntroScreen should be visible, while LoginScreen elements should not exist yet.
        composeTestRule.onNodeWithText("Nexus").assertIsDisplayed()
        composeTestRule.onNodeWithText("Welcome Back").assertDoesNotExist()

        // 2. Advance the clock past the splash animation duration (3600ms) to trigger the onFinished callback
        composeTestRule.mainClock.advanceTimeBy(4000L)

        // Resume automatic advancement so idling resources can wait for composure to settle
        composeTestRule.mainClock.autoAdvance = true

        // Constraint 2: Verify deterministic navigation to the LoginScreen when the user is unauthenticated.
        composeTestRule.onNodeWithText("Welcome Back").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign In").assertIsDisplayed()
    }

    @Test
    fun testSplashVisibleAtLaunchAndNavigatesToDashboardWhenAuthenticated() {
        // Constraint 3: Mock/set up the AuthSession/TokenManager appropriately for an authenticated user.
        AuthSession.saveToken("mocked_authenticated_jwt_token")

        // 1. Control the Compose clock to verify the initial state before animations run
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            NexusNavHost()
        }

        // Constraint 1: Verify that the Splash screen is visible at initial app launch.
        composeTestRule.onNodeWithText("Nexus").assertIsDisplayed()
        composeTestRule.onNodeWithText("Log Out").assertDoesNotExist()

        // 2. Advance the clock past the splash animation duration (3600ms) to trigger the onFinished callback
        composeTestRule.mainClock.advanceTimeBy(4000L)

        // Resume automatic advancement
        composeTestRule.mainClock.autoAdvance = true

        // Constraint 3: Verify deterministic navigation to the DashboardScreen when the user is authenticated.
        // Depending on network/repository response, DashboardScreen shows either the success state or error fallback.
        // Both states uniquely exist on DashboardScreen. We check for success or fallback components.
        try {
            composeTestRule.onNodeWithText("Log Out").assertIsDisplayed()
            composeTestRule.onNodeWithText("Open Projects").assertIsDisplayed()
        } catch (_: AssertionError) {
            // In case of any unhandled network repository error triggering DashboardState.Error state,
            // the Retry button uniquely identifies that the DashboardScreen was successfully navigated to.
            composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
        }
    }

    @Test
    fun testLogoutRequiresConfirmationOnDashboard() {
        AuthSession.saveToken("mocked_authenticated_jwt_token")

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            NexusNavHost()
        }

        composeTestRule.mainClock.advanceTimeBy(4000L)
        composeTestRule.mainClock.autoAdvance = true

        // Tap Log Out button on Dashboard
        composeTestRule.onNodeWithText("Log Out").performClick()

        // Verify confirmation dialog appears
        composeTestRule.onNodeWithText("Log out?").assertIsDisplayed()
        composeTestRule.onNodeWithText("You'll need to sign in again to continue.").assertIsDisplayed()

        // Click Cancel inside dialog
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Dialog should be dismissed, user remains on Dashboard
        composeTestRule.onNodeWithText("Log out?").assertDoesNotExist()
        composeTestRule.onNodeWithText("Log Out").assertIsDisplayed()

        // Tap Log Out button again
        composeTestRule.onNodeWithText("Log Out").performClick()

        // Confirm logout inside dialog
        composeTestRule.onNodeWithText("Log out").performClick()

        // User should now be logged out and navigated to LoginScreen
        composeTestRule.onNodeWithText("Welcome Back").assertIsDisplayed()
    }

    @Test
    fun testSplashReducedMotionPath() {
        var originalScale = 1f
        try {
            val field = ValueAnimator::class.java.getDeclaredField("sDurationScale")
            field.isAccessible = true
            originalScale = field.get(null) as Float
            field.set(null, 0f)
        } catch (_: Exception) {
            try {
                val method = ValueAnimator::class.java.getDeclaredMethod("getDurationScale")
                method.isAccessible = true
                originalScale = method.invoke(null) as Float
                val setMethod = ValueAnimator::class.java.getDeclaredMethod("setDurationScale", Float::class.java)
                setMethod.isAccessible = true
                setMethod.invoke(null, 0f)
            } catch (_: Exception) {
                InstrumentationRegistry.getInstrumentation()
                    .uiAutomation.executeShellCommand("settings put global animator_duration_scale 0")
            }
        }

        try {
            composeTestRule.mainClock.autoAdvance = false

            composeTestRule.setContent {
                NexusNavHost()
            }

            // Verify initial state: Splash screen is visible
            composeTestRule.onNodeWithText("Nexus").assertIsDisplayed()
            composeTestRule.onNodeWithText("Welcome Back").assertDoesNotExist()

            // Advance clock by 500ms (which is more than the 450ms reduced-motion delay but way less than 3600ms)
            composeTestRule.mainClock.advanceTimeBy(500L)

            // Resume auto advance to let it settle
            composeTestRule.mainClock.autoAdvance = true

            // The app should have advanced to Login screen due to reduced motion logic (450ms delay)
            composeTestRule.onNodeWithText("Welcome Back").assertIsDisplayed()
        } finally {
            // Restore original animation scale
            try {
                val field = ValueAnimator::class.java.getDeclaredField("sDurationScale")
                field.isAccessible = true
                field.set(null, originalScale)
            } catch (_: Exception) {
                try {
                    val setMethod = ValueAnimator::class.java.getDeclaredMethod("setDurationScale", Float::class.java)
                    setMethod.isAccessible = true
                    setMethod.invoke(null, originalScale)
                } catch (_: Exception) {
                    InstrumentationRegistry.getInstrumentation()
                        .uiAutomation.executeShellCommand("settings put global animator_duration_scale $originalScale")
                }
            }
        }
    }
}
