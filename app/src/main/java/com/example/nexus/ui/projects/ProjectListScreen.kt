package com.example.nexus.ui.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexus.api.Project
import com.example.nexus.ui.components.CreateProjectDialog
import com.example.nexus.ui.components.EmptyState
import com.example.nexus.ui.components.ErrorState
import com.example.nexus.ui.components.ListSkeleton
import com.example.nexus.ui.components.NexusDestructiveIconButton
import com.example.nexus.ui.components.NexusLogo
import com.example.nexus.ui.settings.CompactLanguageMenu
import com.example.nexus.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    onBackToDashboard: () -> Unit,
    onOpenProject: (String) -> Unit,
    viewModel: ProjectListViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<Project?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    NexusLogo(iconSize = Spacing.TopBarLogoSize, textSize = Spacing.TopBarLogoTextSize)
                },
                navigationIcon = {
                    IconButton(onClick = onBackToDashboard) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadProjects() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    Text(
                        "Projects",
                        modifier = Modifier.padding(end = Spacing.md),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    CompactLanguageMenu()
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New Project")
            }
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            if (uiState.isLoading && uiState.projects.isEmpty()) {
                Box(modifier = Modifier.testTag("project_list_skeleton")) {
                    ListSkeleton()
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().testTag("project_list_content")) {
                    if (!uiState.errorMessage.isNullOrBlank() && uiState.projects.isEmpty()) {
                        ErrorState(
                            message = uiState.errorMessage!!,
                            onRetry = { viewModel.loadProjects() },
                        )
                    } else if (uiState.projects.isEmpty()) {
                        EmptyState(
                            message = "No projects yet. Create one to get started!",
                            onAction = { showCreateDialog = true },
                            actionLabel = "Create Project",
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.smd),
                        ) {
                            items(uiState.projects, key = { it.id }) { project ->
                                ProjectItem(
                                    project = project,
                                    onClick = { onOpenProject(project.id) },
                                    onEdit = { editingProject = project },
                                    onDelete = { viewModel.deleteProject(project.id) },
                                )
                            }
                        }
                    }
                }
            }

            if (!uiState.errorMessage.isNullOrBlank() && uiState.projects.isNotEmpty()) {
                Snackbar(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(Spacing.md),
                    action = {
                        TextButton(onClick = { viewModel.loadProjects() }) {
                            Text("Retry", color = MaterialTheme.colorScheme.inversePrimary)
                        }
                    },
                ) {
                    Text(uiState.errorMessage!!)
                }
            }
        }

        if (showCreateDialog) {
            CreateProjectDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name, desc ->
                    viewModel.createProject(name, desc)
                    showCreateDialog = false
                },
            )
        }
        editingProject?.let { project ->
            CreateProjectDialog(
                initialProject = project,
                onDismiss = { editingProject = null },
                onCreate = { name, description ->
                    viewModel.updateProject(project.id, name, description)
                    editingProject = null
                },
            )
        }
    }
}

@Composable
fun ProjectItem(
    project: Project,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(Spacing.md)
                    .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                NexusDestructiveIconButton(onClick = onDelete, contentDescription = "Delete")
            }
            if (!project.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = project.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}
