package com.example.nexus.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexus.api.Task
import com.example.nexus.api.TaskPriority
import com.example.nexus.ui.components.EmptyState
import com.example.nexus.ui.components.ErrorState
import com.example.nexus.ui.components.NexusLogo
import com.example.nexus.ui.components.ListSkeleton
import com.example.nexus.ui.CompactLanguageMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    projectId: String,
    onBackToProject: () -> Unit,
    onOpenDetail: (String) -> Unit,
    viewModel: TaskListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    var editorTask by remember { mutableStateOf<Task?>(null) }

    LaunchedEffect(projectId) {
        viewModel.loadTasks(projectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    NexusLogo(iconSize = 32.dp, textSize = 22)
                },
                navigationIcon = {
                    IconButton(onClick = onBackToProject) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Text(
                        "Tasks",
                        modifier = Modifier.padding(end = 16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                    CompactLanguageMenu()
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editorTask = null
                showEditor = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "New Task")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && uiState.tasks.isEmpty()) {
                ListSkeleton(labelWidth = 0.7f)
            } else if (!uiState.errorMessage.isNullOrBlank() && uiState.tasks.isEmpty()) {
                ErrorState(
                    message = uiState.errorMessage!!,
                    onRetry = { viewModel.loadTasks(projectId) }
                )
            } else if (uiState.tasks.isEmpty()) {
                EmptyState(
                    message = "No tasks yet. Add one to stay productive!",
                    onAction = {
                        editorTask = null
                        showEditor = true
                    },
                    actionLabel = "Add Task"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.tasks, key = { it.id }) { task ->
                        TaskItem(
                            task = task,
                            onToggle = { viewModel.toggleCompleted(task) },
                            onOpen = { onOpenDetail(task.id) },
                            onDelete = { viewModel.deleteTask(task.id) }
                        )
                    }
                }
            }

            if (!uiState.errorMessage.isNullOrBlank() && uiState.tasks.isNotEmpty()) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.loadTasks(projectId) }) {
                            Text("Retry", color = MaterialTheme.colorScheme.inversePrimary)
                        }
                    }
                ) {
                    Text(uiState.errorMessage!!)
                }
            }
        }

        if (showEditor) {
            val editing = editorTask
            TaskEditorDialog(
                task = editing,
                onDismiss = {
                    showEditor = false
                    editorTask = null
                },
                onSave = { draft ->
                    if (editing == null) {
                        viewModel.createTask(draft)
                    } else {
                        viewModel.updateTask(editing.id, draft)
                    }
                    showEditor = false
                    editorTask = null
                }
            )
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val overdue = isTaskOverdue(task.dueDate, task.isCompleted)
    val metaParts = buildList {
        if (task.priority != TaskPriority.NONE) add(task.priority.displayName())
        add(task.status.displayName())
        if (task.labels.isNotEmpty()) add(task.labels.joinToString(", "))
        if (task.checklist.isNotEmpty()) {
            add("${task.checklist.count { it.isCompleted }}/${task.checklist.size} steps")
        }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            // Tapping the row opens the planning editor, matching the audit's
            // "wire TaskItem so tapping it navigates to the edit screen".
            .clickable(onClick = onOpen),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = if (task.isCompleted) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.outline else Color.Unspecified
                )
                if (!task.description.isNullOrBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Text(
                    text = metaParts.joinToString("  ·  "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                task.dueDate?.let { due ->
                    Text(
                        text = if (overdue) "Due $due · Overdue" else "Due $due",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onOpen) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }
    }
}



