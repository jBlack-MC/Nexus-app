package com.example.nexus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexus.ui.components.ErrorState
import com.example.nexus.ui.components.NexusLogo
import com.example.nexus.ui.components.SkeletonBlock
import com.example.nexus.api.Task
import androidx.compose.ui.res.stringResource
import com.example.nexus.R
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
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val remoteConfigViewModel: RemoteConfigViewModel = viewModel()
    val remoteConfigState by remoteConfigViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { NexusLogo(iconSize = 36.dp, textSize = 26) },
                actions = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Filled.Person, contentDescription = "Profile")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                    CompactLanguageMenu()
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is DashboardState.Loading -> Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 72.dp)
                    SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 72.dp)
                    SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 72.dp)
                    SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 48.dp)
                }
                is DashboardState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { viewModel.fetchDashboard() }
                )
                is DashboardState.Success -> Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.isStale) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.dashboard_stale_banner),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    DashboardCard("Projects", state.data.projects.toString())
                    DashboardCard("Tasks", state.data.tasks.toString())
                    DashboardCard("Activity", state.data.activity.toString())
                    TaskAttentionSections(state.cachedTasks)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onOpenProjects, modifier = Modifier.fillMaxWidth()) {
                        Text("Open Projects")
                    }
                    if (remoteConfigState.config.featureFlags["habits"] != false) {
                        Button(onClick = onOpenHabits, modifier = Modifier.fillMaxWidth()) {
                            Text("Open Habits")
                        }
                    }
                    Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                        Text("Log Out")
                    }
                }
            }

            if (uiState !is DashboardState.Success) {
                Button(
                    onClick = onOpenProjects,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text("Open Projects")
                }
            }

            val config = remoteConfigState.config
            val notice = when {
                config.maintenanceMode -> config.maintenanceMessage.ifBlank { "Nexus is temporarily under maintenance." }
                config.announcements.isNotEmpty() -> "${config.announcements.first().title}: ${config.announcements.first().message}"
                else -> null
            }
            val configError = remoteConfigState.errorMessage
            if (notice != null || (configError != null && !remoteConfigState.isLoading)) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (notice != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (config.maintenanceMode) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (config.maintenanceMode) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(notice, modifier = Modifier.padding(12.dp))
                        }
                    }
                    if (configError != null && !remoteConfigState.isLoading) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = configError,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { remoteConfigViewModel.refresh() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskAttentionSections(tasks: List<Task>) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val overdue = tasks.filter { !it.isCompleted && it.dueDate != null && it.dueDate < today }
    val dueToday = tasks.filter { !it.isCompleted && it.dueDate == today }
    val upcoming = tasks.filter { !it.isCompleted && it.dueDate != null && it.dueDate > today }.sortedBy { it.dueDate }.take(3)
    if (tasks.isNotEmpty()) {
        Text("Your schedule", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        AttentionCard("Overdue", overdue)
        AttentionCard("Due today", dueToday)
        AttentionCard("Upcoming", upcoming)
    }
}

@Composable
private fun AttentionCard(title: String, tasks: List<Task>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("$title (${tasks.size})", style = MaterialTheme.typography.titleSmall)
            if (tasks.isEmpty()) Text("Nothing here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            else tasks.forEach { Text("• ${it.title}${it.dueDate?.let { date -> " · $date" } ?: ""}", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun DashboardCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
