package com.example.nexus.ui.projects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexus.R
import com.example.nexus.api.Task
import com.example.nexus.api.TaskStatus
import com.example.nexus.ui.components.DeleteConfirmationDialog
import com.example.nexus.ui.components.DeleteKind
import com.example.nexus.ui.components.DetailSkeleton
import com.example.nexus.ui.components.ErrorState
import com.example.nexus.ui.components.NexusDestructiveButton
import com.example.nexus.ui.components.NexusLogo
import com.example.nexus.ui.components.NexusPrimaryButton
import com.example.nexus.ui.components.NexusSecondaryButton
import com.example.nexus.ui.components.rememberDeleteConfirmation
import com.example.nexus.ui.settings.CompactLanguageMenu
import com.example.nexus.ui.tasks.TaskDraft
import com.example.nexus.ui.tasks.TaskEditorDialog
import com.example.nexus.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    onBackToProjects: () -> Unit,
    onOpenTasks: (String) -> Unit,
    viewModel: ProjectDetailViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    if (uiState.isDeleted) {
        LaunchedEffect(uiState.isDeleted) {
            onBackToProjects()
        }
    }

    val project = uiState.project
    val confirmDelete = rememberDeleteConfirmation()
    DeleteConfirmationDialog(confirmDelete)
    var projectName by remember(project?.id) { mutableStateOf(project?.name ?: "") }
    var projectDescription by remember(project?.id) { mutableStateOf(project?.description ?: "") }

    var showTaskEditor by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    NexusLogo(iconSize = Spacing.TopBarLogoSize, textSize = Spacing.TopBarLogoTextSize)
                },
                actions = {
                    Text(
                        stringResource(R.string.project_details),
                        modifier = Modifier.padding(end = Spacing.md),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    CompactLanguageMenu()
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (uiState.isLoading && project == null) {
                DetailSkeleton()
            } else if (!uiState.errorMessage.isNullOrBlank() && project == null) {
                ErrorState(
                    message = uiState.errorMessage!!,
                    onRetry = { viewModel.loadProject(projectId) },
                )
            } else {
                Column(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .testTag("project_detail_content")
                        .padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    if (project != null) {
                        OutlinedTextField(
                            value = projectName,
                            onValueChange = { projectName = it },
                            label = { Text(stringResource(R.string.project_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = projectDescription,
                            onValueChange = { projectDescription = it },
                            label = { Text(stringResource(R.string.field_description)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            NexusPrimaryButton(
                                text = stringResource(R.string.project_save),
                                onClick = { viewModel.updateProject(projectName, projectDescription) },
                            )
                            NexusSecondaryButton(
                                text = stringResource(R.string.project_open_tasks),
                                onClick = { onOpenTasks(project.id) },
                            )
                            NexusDestructiveButton(
                                text = stringResource(R.string.project_delete),
                                onClick = { confirmDelete.ask(DeleteKind.PROJECT, project?.name.orEmpty()) { viewModel.deleteProject() } },
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(R.string.nav_tasks), style = MaterialTheme.typography.titleMedium)
                            NexusPrimaryButton(
                                text = stringResource(R.string.task_add),
                                onClick = {
                                    editingTask = null
                                    showTaskEditor = true
                                },
                            )
                        }

                        if (uiState.tasks.isEmpty()) {
                            Text(
                                text = stringResource(R.string.project_no_tasks),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = Spacing.sm),
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                                modifier = Modifier.weight(1f, fill = false),
                            ) {
                                items(uiState.tasks, key = { it.id }) { task ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    ) {
                                        Column(
                                            modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(Spacing.sm),
                                            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                                        ) {
                                            Text(text = task.title, style = MaterialTheme.typography.titleSmall)
                                            Text(text = task.description ?: stringResource(R.string.project_no_description))
                                            Text(
                                                text = if (task.isCompleted) {
                                                    stringResource(R.string.status_done)
                                                } else {
                                                    stringResource(R.string.task_state_open)
                                                },
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                                NexusSecondaryButton(
                                                    text = stringResource(R.string.action_edit),
                                                    onClick = {
                                                        editingTask = task
                                                        showTaskEditor = true
                                                    },
                                                )
                                                NexusPrimaryButton(
                                                    text = if (task.isCompleted) {
                                                        stringResource(R.string.task_mark_open)
                                                    } else {
                                                        stringResource(R.string.task_mark_complete)
                                                    },
                                                    onClick = {
                                                        viewModel.updateTask(
                                                            task.id,
                                                            TaskDraft(
                                                                title = task.title,
                                                                description = task.description,
                                                                dueDate = task.dueDate,
                                                                priority = task.priority,
                                                                status = if (task.isCompleted) TaskStatus.TODO else TaskStatus.DONE,
                                                                labels = task.labels,
                                                                checklist = task.checklist,
                                                            ),
                                                        )
                                                    },
                                                )
                                                NexusDestructiveButton(
                                                    text = stringResource(R.string.action_delete),
                                                    onClick = {
                                                        confirmDelete.ask(
                                                            DeleteKind.TASK,
                                                            task.title,
                                                        ) { viewModel.deleteTask(task.id) }
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    NexusSecondaryButton(
                        text = stringResource(R.string.action_back),
                        onClick = onBackToProjects,
                    )
                }
            }

            if (!uiState.errorMessage.isNullOrBlank() && project != null) {
                Snackbar(
                    modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(Spacing.md),
                    action = {
                        TextButton(onClick = { viewModel.loadProject(projectId) }) {
                            Text(stringResource(R.string.action_retry), color = MaterialTheme.colorScheme.inversePrimary)
                        }
                    },
                ) {
                    Text(uiState.errorMessage!!)
                }
            }
        }

        if (showTaskEditor) {
            val editing = editingTask
            TaskEditorDialog(
                task = editing,
                onDismiss = {
                    showTaskEditor = false
                    editingTask = null
                },
                onSave = { draft ->
                    if (editing == null) {
                        viewModel.createTask(draft)
                    } else {
                        viewModel.updateTask(editing.id, draft)
                    }
                    showTaskEditor = false
                    editingTask = null
                },
            )
        }
    }
}
