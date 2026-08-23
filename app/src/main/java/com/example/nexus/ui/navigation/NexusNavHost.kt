package com.example.nexus.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nexus.auth.AuthSession
import com.example.nexus.ui.AuthViewModel
import com.example.nexus.ui.DashboardScreen
import com.example.nexus.ui.LoginScreen
import com.example.nexus.ui.RegisterScreen
import com.example.nexus.ui.projects.ProjectDetailScreen
import com.example.nexus.ui.projects.ProjectListScreen
import com.example.nexus.ui.tasks.TaskListScreen

private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val DASHBOARD = "dashboard"
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

    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated) {
            navController.navigate(Routes.DASHBOARD) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (authState.isAuthenticated) Routes.DASHBOARD else Routes.LOGIN
    ) {
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
                onLogout = {
                    AuthSession.clearToken()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0)
                    }
                }
            )
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

