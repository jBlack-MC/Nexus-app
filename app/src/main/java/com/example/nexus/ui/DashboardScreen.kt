package com.example.nexus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.nexus.api.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenProjects: () -> Unit,
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { NexusLogo(iconSize = 36.dp, textSize = 26) }
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
                is DashboardState.Loading -> CircularProgressIndicator()
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
                    DashboardCard("Projects", state.data.projects.toString())
                    DashboardCard("Tasks", state.data.tasks.toString())
                    DashboardCard("Activity", state.data.activity.toString())
                    TaskAttentionSections(state.cachedTasks)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onOpenProjects, modifier = Modifier.fillMaxWidth()) {
                        Text("Open Projects")
                    }
                    Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                        Text("Log Out")
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
