package com.example.nexus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexus.ui.components.ErrorState
import com.example.nexus.ui.components.NexusLogo
import com.example.nexus.ui.components.SkeletonBlock
import com.example.nexus.ui.components.rememberReduceMotion
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
    val reduceMotion = rememberReduceMotion()

    // Re-entering this destination (back from Projects/Tasks/Habits) should show fresh counts and
    // schedule cards, because the ViewModel survives in the back stack and its init fetch does not
    // re-run. The first composition is already covered by that init fetch, so only later entries
    // refresh — softly, so the Loading skeleton never flashes on the way back.
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
                    IconButton(onClick = onOpenProfile) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = stringResource(R.string.action_profile)
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.action_settings)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
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
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
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

                when (val state = uiState) {
                    is DashboardState.Loading -> Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 72.dp)
                        SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 72.dp)
                        SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 72.dp)
                        SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 48.dp)
                    }
                    is DashboardState.Error -> ErrorState(
                        message = state.message,
                        onRetry = { viewModel.fetchDashboard() },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                    is DashboardState.Success -> Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
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
                            if (!reduceMotion) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(3.dp))
                            }
                        }
                        if (state.data.projects == 0 && state.data.tasks == 0 && state.data.activity == 0) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.RocketLaunch,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Text(
                                    text = "Start your first project",
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Projects keep your tasks and habits organized.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Filled.Folder,
                                    value = state.data.projects.toString(),
                                    label = "Projects",
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                StatCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Filled.Checklist,
                                    value = state.data.tasks.toString(),
                                    label = "Tasks",
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                StatCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Filled.Timeline,
                                    value = state.data.activity.toString(),
                                    label = "Activity",
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }

                        TaskAttentionSections(state.cachedTasks)

                        Button(onClick = onOpenProjects, modifier = Modifier.fillMaxWidth()) {
                            Text("Open Projects")
                        }
                        if (remoteConfigState.config.featureFlags["habits"] != false) {
                            OutlinedButton(onClick = onOpenHabits, modifier = Modifier.fillMaxWidth()) {
                                Text("Open Habits")
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                        TextButton(
                            onClick = onLogout,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Log Out")
                        }
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
            if (tasks.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text("Nothing here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            else tasks.forEach { Text("• ${it.title}${it.dueDate?.let { date -> " · $date" } ?: ""}", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
