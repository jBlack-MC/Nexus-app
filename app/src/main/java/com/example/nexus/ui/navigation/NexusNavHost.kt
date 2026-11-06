package com.example.nexus.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.nexus.NexusApp
import com.example.nexus.R
import com.example.nexus.auth.AuthSession
import com.example.nexus.ui.auth.AuthViewModel
import com.example.nexus.ui.auth.LoginScreen
import com.example.nexus.ui.auth.RegisterScreen
import com.example.nexus.ui.components.Motion
import com.example.nexus.ui.components.localizedMessage
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
import kotlinx.coroutines.launch
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
    val syncStatus by NexusApp.syncManager.status.collectAsState()
    val scope = rememberCoroutineScope()
    var reviewConflict by remember { mutableStateOf(false) }
    val showTabs = currentDestination?.let {
        it.hasRoute<Route.Dashboard>() || it.hasRoute<Route.Projects>() || it.hasRoute<Route.Habits>() || it.hasRoute<Route.Settings>()
    } == true && isAppAuthenticated
    Column(Modifier.fillMaxSize()) {
        if (isAppAuthenticated && currentDestination?.hasRoute<Route.Splash>() == false) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (syncStatus.pending > 0) Icons.Default.CloudUpload else Icons.Default.CloudDone,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(localizedMessage(syncStatus.message), style = MaterialTheme.typography.labelMedium)
                        if (syncStatus.pending > 0) {
                            Text(
                                pluralStringResource(R.plurals.sync_pending, syncStatus.pending, syncStatus.pending),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    TextButton(enabled = !syncStatus.syncing, onClick = {
                        scope.launch { NexusApp.syncManager.sync() }
                    }) { Text(stringResource(R.string.sync_button)) }
                    if (syncStatus.conflict) TextButton(onClick = { reviewConflict = true }) { Text(stringResource(R.string.sync_review)) }
                    if (syncStatus.message.startsWith("Sign in")) {
                        TextButton(onClick = {
                            AuthSession.clearToken()
                        }) { Text(stringResource(R.string.sync_sign_in)) }
                    }
                }
            }
        }
        NavHost(
            modifier = Modifier.weight(1f),
            navController = navController,
            startDestination = Route.Splash,
            enterTransition = {
                if (reduceMotion) {
                    EnterTransition.None
                } else {
                    slideInHorizontally(tween(Motion.SCREEN_ENTER_MS, easing = FastOutSlowInEasing)) { it / 4 } +
                        fadeIn(tween(Motion.SCREEN_ENTER_MS, easing = FastOutSlowInEasing))
                }
            },
            exitTransition = {
                if (reduceMotion) {
                    ExitTransition.None
                } else {
                    slideOutHorizontally(tween(Motion.SCREEN_EXIT_MS, easing = FastOutSlowInEasing)) { -it / 4 } +
                        fadeOut(tween(Motion.SCREEN_EXIT_MS, easing = FastOutSlowInEasing))
                }
            },
            popEnterTransition = {
                if (reduceMotion) {
                    EnterTransition.None
                } else {
                    slideInHorizontally(tween(Motion.SCREEN_ENTER_MS, easing = FastOutSlowInEasing)) { -it / 4 } +
                        fadeIn(tween(Motion.SCREEN_ENTER_MS, easing = FastOutSlowInEasing))
                }
            },
            popExitTransition = {
                if (reduceMotion) {
                    ExitTransition.None
                } else {
                    slideOutHorizontally(tween(Motion.SCREEN_EXIT_MS, easing = FastOutSlowInEasing)) { it / 4 } +
                        fadeOut(tween(Motion.SCREEN_EXIT_MS, easing = FastOutSlowInEasing))
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
                    onGoogleIdToken = authViewModel::googleSignIn,
                    onGoogleError = authViewModel::googleSignInUnavailable,
                )
            }

            composable<Route.Register> {
                RegisterScreen(
                    uiState = authState,
                    onRegister = { email, password, displayName ->
                        authViewModel.register(email, password, displayName)
                    },
                    onNavigateToLogin = { navController.popBackStack() },
                    onGoogleIdToken = authViewModel::googleSignIn,
                    onGoogleError = authViewModel::googleSignInUnavailable,
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
        if (showTabs) {
            NavigationBar {
                val tabs = listOf(
                    Triple<Route, String, androidx.compose.ui.graphics.vector.ImageVector>(
                        Route.Dashboard,
                        stringResource(R.string.nav_today),
                        Icons.Default.Dashboard,
                    ),
                    Triple<Route, String, androidx.compose.ui.graphics.vector.ImageVector>(
                        Route.Projects,
                        stringResource(R.string.nav_projects),
                        Icons.Default.Folder,
                    ),
                    Triple<Route, String, androidx.compose.ui.graphics.vector.ImageVector>(
                        Route.Habits,
                        stringResource(R.string.nav_habits),
                        Icons.Default.CheckCircle,
                    ),
                    Triple<Route, String, androidx.compose.ui.graphics.vector.ImageVector>(
                        Route.Settings,
                        stringResource(R.string.action_settings),
                        Icons.Default.Settings,
                    ),
                )
                tabs.forEach { (route, label, icon) ->
                    val selected = when (route) {
                        Route.Dashboard -> currentDestination?.hasRoute<Route.Dashboard>() == true
                        Route.Projects -> currentDestination?.hasRoute<Route.Projects>() == true
                        Route.Habits -> currentDestination?.hasRoute<Route.Habits>() == true
                        else -> currentDestination?.hasRoute<Route.Settings>() == true
                    }
                    NavigationBarItem(selected = selected, icon = {
                        Icon(
                            icon,
                            contentDescription = null,
                        )
                    }, label = { Text(label, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) }, onClick = {
                        navController.navigate(route) {
                            popUpTo<Route.Dashboard> { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    })
                }
            }
        }
    }

    if (reviewConflict) {
        AlertDialog(
            onDismissRequest = { reviewConflict = false },
            title = { Text(stringResource(R.string.sync_conflict_title)) },
            text = {
                Text(stringResource(R.string.sync_conflict_body))
            },
            confirmButton = {
                TextButton(onClick = {
                    reviewConflict = false
                    scope.launch { NexusApp.syncManager.discardConflict() }
                }) { Text(stringResource(R.string.sync_discard_item)) }
            },
            dismissButton = { TextButton(onClick = { reviewConflict = false }) { Text(stringResource(R.string.sync_keep_changes)) } },
        )
    }
}
