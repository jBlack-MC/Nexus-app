package com.example.nexus.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nexus.auth.AuthSession
import com.example.nexus.ui.AuthViewModel
import com.example.nexus.ui.DashboardScreen
import com.example.nexus.ui.HabitsScreen
import com.example.nexus.ui.LoginScreen
import com.example.nexus.ui.ProfileScreen
import com.example.nexus.ui.RegisterScreen
import com.example.nexus.ui.SettingsScreen
import com.example.nexus.ui.SplashIntroScreen
import com.example.nexus.ui.projects.ProjectDetailScreen
import com.example.nexus.ui.projects.ProjectListScreen
import com.example.nexus.ui.tasks.TaskListScreen

private object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val HABITS = "habits"
    const val PROJECTS = "projects"
    const val PROJECT_DETAIL = "project/{projectId}"
    const val TASKS = "tasks/{projectId}"

    fun projectDetail(projectId: String) = "project/$projectId"
    fun tasks(projectId: String) = "tasks/$projectId"
}

@Composable
fun NexusNavHost(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    val authState by authViewModel.uiState.collectAsState()
    val isAppAuthenticated by AuthSession.isAuthenticated.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    LaunchedEffect(isAppAuthenticated, currentRoute) {
        if (
            currentRoute != null &&
            !isAppAuthenticated &&
            currentRoute != Routes.LOGIN &&
            currentRoute != Routes.REGISTER &&
            currentRoute != Routes.SPLASH
        ) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        } else if (
            isAppAuthenticated &&
                (currentRoute == Routes.LOGIN || currentRoute == Routes.REGISTER)
        ) {
            navController.navigate(Routes.DASHBOARD) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashIntroScreen(
                onFinished = {
                    val nextRoute = if (isAppAuthenticated) Routes.DASHBOARD else Routes.LOGIN
                    navController.navigate(nextRoute) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                uiState = authState,
                onLogin = { email, password -> authViewModel.login(email, password) },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                uiState = authState,
                onRegister = { email, password, displayName ->
                    authViewModel.register(email, password, displayName)
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onOpenProjects = { navController.navigate(Routes.PROJECTS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onOpenHabits = { navController.navigate(Routes.HABITS) },
                onLogout = {
                    AuthSession.clearToken()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0)
                    }
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0)
                    }
                }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.HABITS) {
            HabitsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.PROJECTS) {
            ProjectListScreen(
                onBackToDashboard = { navController.popBackStack() },
                onOpenProject = { projectId -> navController.navigate(Routes.projectDetail(projectId)) }
            )
        }

        composable(
            route = Routes.PROJECT_DETAIL,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            ProjectDetailScreen(
                projectId = projectId,
                onBackToProjects = { navController.popBackStack() },
                onOpenTasks = { id -> navController.navigate(Routes.tasks(id)) }
            )
        }

        composable(
            route = Routes.TASKS,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            TaskListScreen(
                projectId = projectId,
                onBackToProject = { navController.popBackStack() }
            )
        }
    }
}

