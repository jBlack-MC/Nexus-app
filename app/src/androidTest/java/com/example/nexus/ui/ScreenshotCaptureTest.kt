package com.example.nexus.ui

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nexus.api.DashboardData
import com.example.nexus.api.Habit
import com.example.nexus.api.HabitFrequency
import com.example.nexus.api.Project
import com.example.nexus.api.UserProfile
import com.example.nexus.data.DashboardRepository
import com.example.nexus.data.HabitRepository
import com.example.nexus.data.ProjectRepository
import com.example.nexus.data.UserRepository
import com.example.nexus.ui.auth.AuthUiState
import com.example.nexus.ui.auth.LoginScreen
import com.example.nexus.ui.auth.RegisterScreen
import com.example.nexus.ui.dashboard.DashboardScreen
import com.example.nexus.ui.dashboard.DashboardViewModel
import com.example.nexus.ui.habits.HabitsScreen
import com.example.nexus.ui.habits.HabitsViewModel
import com.example.nexus.ui.profile.ProfileScreen
import com.example.nexus.ui.projects.ProjectListScreen
import com.example.nexus.ui.projects.ProjectListViewModel
import com.example.nexus.ui.settings.SettingsScreen
import com.example.nexus.ui.settings.SettingsViewModel
import com.example.nexus.ui.splash.SplashIntroScreen
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Instrumented test suite verifying content settling and screenshot capture wait conditions
 * across all Nexus screens.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotCaptureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Explicit wait condition per screen: waits for the target semantics testTag
     * present in the target settled state (Success, or Empty/Error/Skeleton where that's
     * what is captured) with a 5000ms timeout, rather than relying on fixed sleeps.
     */
    private fun waitForContentTag(targetTag: String, timeoutMillis: Long = 5000L) {
        composeTestRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeTestRule
                .onAllNodes(hasTestTag(targetTag))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun captureSplash() {
        composeTestRule.setContent {
            SplashIntroScreen(onFinished = {})
        }
        waitForContentTag("splash_content")
    }

    @Test
    fun captureLogin() {
        composeTestRule.setContent {
            LoginScreen(
                uiState = AuthUiState(),
                onLogin = { _, _ -> },
                onNavigateToRegister = {},
            )
        }
        waitForContentTag("login_content")
    }

    @Test
    fun captureRegister() {
        composeTestRule.setContent {
            RegisterScreen(
                uiState = AuthUiState(),
                onRegister = { _, _, _ -> },
                onNavigateToLogin = {},
            )
        }
        waitForContentTag("register_content")
    }

    @Test
    fun captureDashboardContent() {
        val fakeDashboardRepo = object : DashboardRepository() {
            override suspend fun getDashboard(): DashboardData =
                DashboardData(projects = 4, tasks = 12, activity = 8)
        }
        val viewModel = DashboardViewModel(dashboardRepository = fakeDashboardRepo)
        composeTestRule.setContent {
            DashboardScreen(
                onOpenProjects = {},
                onOpenSettings = {},
                onOpenProfile = {},
                onOpenHabits = {},
                onLogout = {},
                viewModel = viewModel,
            )
        }
        waitForContentTag("dashboard_content")
    }

    @Test
    fun captureDashboardOffline() {
        val fakeDashboardRepo = object : DashboardRepository() {
            override suspend fun getDashboard(): DashboardData =
                throw IOException("Unable to connect to Nexus server.")
        }
        val viewModel = DashboardViewModel(dashboardRepository = fakeDashboardRepo)
        composeTestRule.setContent {
            DashboardScreen(
                onOpenProjects = {},
                onOpenSettings = {},
                onOpenProfile = {},
                onOpenHabits = {},
                onLogout = {},
                viewModel = viewModel,
            )
        }
        waitForContentTag("dashboard_error_content")
    }

    @Test
    fun captureProjectListSkeleton() {
        val fakeProjectRepo = object : ProjectRepository() {
            override suspend fun getProjects(): List<Project> {
                delay(10000)
                return emptyList()
            }
        }
        val viewModel = ProjectListViewModel(projectRepository = fakeProjectRepo)
        composeTestRule.setContent {
            ProjectListScreen(
                onBackToDashboard = {},
                onOpenProject = {},
                viewModel = viewModel,
            )
        }
        waitForContentTag("project_list_skeleton")
    }

    @Test
    fun captureProjectListContent() {
        val fakeProjectRepo = object : ProjectRepository() {
            override suspend fun getProjects(): List<Project> = listOf(
                Project("1", "Mobile App Redesign", "Overhaul UI/UX for Nexus v2.0"),
                Project("2", "Backend API Migration", "Migrate REST endpoints to Node.js"),
            )
        }
        val viewModel = ProjectListViewModel(projectRepository = fakeProjectRepo)
        composeTestRule.setContent {
            ProjectListScreen(
                onBackToDashboard = {},
                onOpenProject = {},
                viewModel = viewModel,
            )
        }
        waitForContentTag("project_list_content")
    }

    @Test
    fun captureHabitsContent() {
        val fakeHabitRepo = object : HabitRepository() {
            override suspend fun getHabits(): List<Habit> = listOf(
                Habit("1", "Morning Meditation", description = "10 mins daily", frequency = HabitFrequency.DAILY),
                Habit("2", "Exercise 30 mins", description = "Cardio & strength", frequency = HabitFrequency.DAILY),
            )
        }
        val viewModel = HabitsViewModel(habitRepository = fakeHabitRepo)
        composeTestRule.setContent {
            HabitsScreen(
                onBack = {},
                viewModel = viewModel,
            )
        }
        waitForContentTag("habits_content")
    }

    @Test
    fun captureProfileContent() {
        val fakeUserRepo = object : UserRepository() {
            override suspend fun getProfile(): UserProfile =
                UserProfile("alex.dev@example.com", "Alex Developer")
        }
        val viewModel = SettingsViewModel(userRepository = fakeUserRepo)
        composeTestRule.setContent {
            ProfileScreen(
                onBack = {},
                onOpenSettings = {},
                viewModel = viewModel,
            )
        }
        waitForContentTag("profile_content")
    }

    @Test
    fun captureSettingsContent() {
        val fakeUserRepo = object : UserRepository() {
            override suspend fun getProfile(): UserProfile =
                UserProfile("alex.dev@example.com", "Alex Developer")
        }
        val viewModel = SettingsViewModel(userRepository = fakeUserRepo)
        composeTestRule.setContent {
            SettingsScreen(
                onBack = {},
                onLoggedOut = {},
                viewModel = viewModel,
            )
        }
        waitForContentTag("settings_content")
    }
}
