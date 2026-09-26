package com.example.nexus.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexus.R
import com.example.nexus.api.Task
import com.example.nexus.ui.components.DashboardSkeleton
import com.example.nexus.ui.components.EmptyState
import com.example.nexus.ui.components.ErrorState
import com.example.nexus.ui.components.InlineErrorState
import com.example.nexus.ui.components.LogoutConfirmationDialog
import com.example.nexus.ui.components.NexusDestructiveButton
import com.example.nexus.ui.components.NexusLogo
import com.example.nexus.ui.components.NexusPrimaryButton
import com.example.nexus.ui.components.NexusSecondaryButton
import com.example.nexus.ui.components.StatCard
import com.example.nexus.ui.settings.LanguagePill
import com.example.nexus.ui.settings.RemoteConfigViewModel
import com.example.nexus.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenProjects: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenHabits: () -> Unit,
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val remoteConfigViewModel: RemoteConfigViewModel = viewModel()
    val remoteConfigState by remoteConfigViewModel.uiState.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var hasEnteredOnce by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (hasEnteredOnce) {
            viewModel.fetchDashboard(softRefresh = true)
        } else {
            hasEnteredOnce = true
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { NexusLogo(iconSize = 36.dp, textSize = 26) },
                actions = {
                    LanguagePill(modifier = Modifier.padding(end = Spacing.xs))
                    IconButton(onClick = onOpenProfile) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = stringResource(R.string.action_profile),
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.action_settings),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier =
                    Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxSize(),
            ) {
                val config = remoteConfigState.config
                val notice =
                    when {
                        config.maintenanceMode -> config.maintenanceMessage.ifBlank { "Nexus is temporarily under maintenance." }
                        config.announcements.isNotEmpty() -> "${config.announcements.first().title}: ${config.announcements.first().message}"
                        else -> null
                    }
                val configError = remoteConfigState.errorMessage
                if (notice != null || (configError != null && !remoteConfigState.isLoading)) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        if (notice != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = if (config.maintenanceMode) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (config.maintenanceMode) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Text(notice, modifier = Modifier.padding(Spacing.smd))
                            }
                        }
                        if (configError != null && !remoteConfigState.isLoading) {
                            InlineErrorState(
                                message = configError,
                                onRetry = { remoteConfigViewModel.refresh() },
                            )
                        }
                    }
                }

                when (val state = uiState) {
                    is DashboardState.Loading ->
                        Column(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(Spacing.smd),
                        ) {
                            DashboardSkeleton()
                            NexusPrimaryButton(
                                text = "Open Projects",
                                onClick = onOpenProjects,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                            )
                        }
                    is DashboardState.Error ->
                        ErrorState(
                            message = state.message,
                            onRetry = { viewModel.fetchDashboard() },
                            modifier = Modifier.fillMaxSize().testTag("dashboard_error_content"),
                        )
                    is DashboardState.Success ->
                        Column(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .testTag("dashboard_content")
                                    .verticalScroll(rememberScrollState())
                                    .padding(Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.smd),
                        ) {
                            if (state.data.projects == 0 && state.data.tasks == 0 && state.data.activity == 0) {
                                EmptyState(
                                    title = "Start your first project",
                                    message = "Projects keep your tasks and habits organized.",
                                    icon = Icons.Filled.RocketLaunch,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
                                ) {
                                    StatCard(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Filled.Folder,
                                        value = state.data.projects.toString(),
                                        label = "Projects",
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    StatCard(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Filled.Checklist,
                                        value = state.data.tasks.toString(),
                                        label = "Tasks",
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                    StatCard(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Filled.Timeline,
                                        value = state.data.activity.toString(),
                                        label = "Activity",
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                }
                            }

                            TaskAttentionSections(state.cachedTasks)

                            NexusPrimaryButton(
                                text = "Open Projects",
                                onClick = onOpenProjects,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = Spacing.xs),
                            )
                            if (remoteConfigState.config.featureFlags["habits"] != false) {
                                NexusSecondaryButton(
                                    text = "Open Habits",
                                    onClick = onOpenHabits,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(top = Spacing.smd))
                            NexusDestructiveButton(
                                text = "Log Out",
                                onClick = { showLogoutDialog = true },
                                modifier =
                                    Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(top = Spacing.xs),
                            )
                        }
                }
            }

            if (uiState is DashboardState.Success && (uiState as DashboardState.Success).isStale) {
                Snackbar(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(Spacing.md),
                    action = {
                        TextButton(onClick = { viewModel.fetchDashboard(softRefresh = true) }) {
                            Text("Retry", color = MaterialTheme.colorScheme.inversePrimary)
                        }
                    },
                ) {
                    Text(stringResource(R.string.dashboard_stale_banner))
                }
            }
        }
    }

    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onDismiss = { showLogoutDialog = false },
        )
    }
}

@Composable
private fun TaskAttentionSections(tasks: List<Task>) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val overdue = tasks.filter { !it.isCompleted && it.dueDate != null && it.dueDate < today }
    val dueToday = tasks.filter { !it.isCompleted && it.dueDate == today }
    val upcoming = tasks.filter { !it.isCompleted && it.dueDate != null && it.dueDate > today }.sortedBy { it.dueDate }.take(3)
    if (tasks.isNotEmpty()) {
        Text("Your schedule", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Spacing.smd))
        AttentionCard("Overdue", overdue)
        AttentionCard("Due today", dueToday)
        AttentionCard("Upcoming", upcoming)
    }
}

@Composable
private fun AttentionCard(
    title: String,
    tasks: List<Task>,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.smd)) {
            Text("$title (${tasks.size})", style = MaterialTheme.typography.titleSmall)
            if (tasks.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.smCompact),
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                    Text("Nothing here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                tasks.forEach {
                    Text(
                        "• ${it.title}${it.dueDate?.let {
                                date ->
                            " · $date"
                        } ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
