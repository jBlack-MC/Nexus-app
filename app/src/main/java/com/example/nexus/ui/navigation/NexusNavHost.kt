package com.example.nexus.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.nexus.auth.AuthSession
import com.example.nexus.ui.auth.AuthViewModel
import com.example.nexus.ui.auth.LoginScreen
import com.example.nexus.ui.auth.RegisterScreen
import com.example.nexus.ui.components.Motion
import com.example.nexus.ui.components.rememberReduceMotion
import com.example.nexus.ui.dashboard.DashboardScreen
import com.example.nexus.ui.habits.HabitsScreen
import com.example.nexus.ui.profile.ProfileScreen
import com.example.nexus.ui.projects.ProjectDetailScreen
import com.example.nexus.ui.projects.ProjectListScreen
import com.example.nexus.ui.settings.SettingsScreen
import com.example.nexus.ui.splash.SplashIntroScreen
import com.example.nexus.ui.tasks.TaskDetailScreen
import com.example.nexus.ui.tasks.TaskListScreen
import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object Splash : Route

    @Serializable data object Login : Route

    @Serializable data object Register : Route

    @Serializable data object Dashboard : Route

    @Serializable data object Settings : Route

    @Serializable data object Profile : Route

    @Serializable data object Habits : Route

    @Serializable data object Projects : Route

    @Serializable data class ProjectDetail(val projectId: String) : Route

    @Serializable data class Tasks(val projectId: String) : Route

    @Serializable data class TaskDetail(val projectId: String, val taskId: String) : Route
}

@Composable
fun NexusNavHost(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    val authState by authViewModel.uiState.collectAsState()
    val isAppAuthenticated by AuthSession.isAuthenticated.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination

    LaunchedEffect(isAppAuthenticated, currentDestination) {
        if (currentDestination == null) return@LaunchedEffect
        val isUnauthAllowed =
            currentDestination.hasRoute<Route.Login>() ||
                currentDestination.hasRoute<Route.Register>() ||
                currentDestination.hasRoute<Route.Splash>()

        if (!isAppAuthenticated && !isUnauthAllowed) {
            navController.navigate(Route.Login) {
                popUpTo(0) { inclusive = true }
            }
        } else if (isAppAuthenticated && (currentDestination.hasRoute<Route.Login>() || currentDestination.hasRoute<Route.Register>())) {
            navController.navigate(Route.Dashboard) {
                popUpTo<Route.Login> { inclusive = true }
            }
        }
    }

    val reduceMotion = rememberReduceMotion()
    NavHost(
        navController = navController,
        startDestination = Route.Splash,
        enterTransition = {
            if (reduceMotion) {
                EnterTransition.None
            } else {
                slideInHorizontally(tween(Motion.screenEnterMs, easing = FastOutSlowInEasing)) { it / 4 } +
                    fadeIn(tween(Motion.screenEnterMs, easing = FastOutSlowInEasing))
            }
        },
        exitTransition = {
            if (reduceMotion) {
                ExitTransition.None
            } else {
                slideOutHorizontally(tween(Motion.screenExitMs, easing = FastOutSlowInEasing)) { -it / 4 } +
                    fadeOut(tween(Motion.screenExitMs, easing = FastOutSlowInEasing))
            }
        },
        popEnterTransition = {
            if (reduceMotion) {
                EnterTransition.None
            } else {
                slideInHorizontally(tween(Motion.screenEnterMs, easing = FastOutSlowInEasing)) { -it / 4 } +
                    fadeIn(tween(Motion.screenEnterMs, easing = FastOutSlowInEasing))
            }
        },
        popExitTransition = {
            if (reduceMotion) {
                ExitTransition.None
            } else {
                slideOutHorizontally(tween(Motion.screenExitMs, easing = FastOutSlowInEasing)) { it / 4 } +
                    fadeOut(tween(Motion.screenExitMs, easing = FastOutSlowInEasing))
            }
        },
    ) {
        composable<Route.Splash> {
            SplashIntroScreen(
                onFinished = {
                    val nextRoute: Route = if (isAppAuthenticated) Route.Dashboard else Route.Login
                    navController.navigate(nextRoute) {
                        popUpTo<Route.Splash> { inclusive = true }
                    }
                },
            )
        }

        composable<Route.Login> {
            LoginScreen(
                uiState = authState,
                onLogin = { email, password -> authViewModel.login(email, password) },
                onNavigateToRegister = { navController.navigate(Route.Register) },
            )
        }

        composable<Route.Register> {
            RegisterScreen(
                uiState = authState,
                onRegister = { email, password, displayName ->
                    authViewModel.register(email, password, displayName)
                },
                onNavigateToLogin = { navController.popBackStack() },
            )
        }

        composable<Route.Dashboard> {
            DashboardScreen(
                onOpenProjects = { navController.navigate(Route.Projects) },
                onOpenSettings = { navController.navigate(Route.Settings) },
                onOpenProfile = { navController.navigate(Route.Profile) },
                onOpenHabits = { navController.navigate(Route.Habits) },
                onLogout = {
                    AuthSession.clearToken()
                    navController.navigate(Route.Login) {
                        popUpTo(0)
                    }
                },
            )
        }

        composable<Route.Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Route.Login) {
                        popUpTo(0)
                    }
                },
            )
        }

        composable<Route.Profile> {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(Route.Settings) },
            )
        }

        composable<Route.Habits> {
            HabitsScreen(onBack = { navController.popBackStack() })
        }

        composable<Route.Projects> {
            ProjectListScreen(
                onBackToDashboard = { navController.popBackStack() },
                onOpenProject = { projectId -> navController.navigate(Route.ProjectDetail(projectId)) },
            )
        }

        composable<Route.ProjectDetail> { backStackEntry ->
            val args = backStackEntry.toRoute<Route.ProjectDetail>()
            ProjectDetailScreen(
                projectId = args.projectId,
                onBackToProjects = { navController.popBackStack() },
                onOpenTasks = { id -> navController.navigate(Route.Tasks(id)) },
            )
        }

        composable<Route.Tasks> { backStackEntry ->
            val args = backStackEntry.toRoute<Route.Tasks>()
            TaskListScreen(
                projectId = args.projectId,
                onBackToProject = { navController.popBackStack() },
                onOpenDetail = { taskId ->
                    navController.navigate(Route.TaskDetail(args.projectId, taskId))
                },
            )
        }

        composable<Route.TaskDetail> { backStackEntry ->
            val args = backStackEntry.toRoute<Route.TaskDetail>()
            TaskDetailScreen(
                projectId = args.projectId,
                taskId = args.taskId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
