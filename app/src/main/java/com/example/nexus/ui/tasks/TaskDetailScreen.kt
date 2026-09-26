package com.example.nexus.ui.tasks

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexus.api.Task
import com.example.nexus.ui.components.DetailSkeleton
import com.example.nexus.ui.components.EmptyState
import com.example.nexus.ui.components.ErrorState
import com.example.nexus.ui.components.NexusPrimaryButton
import com.example.nexus.ui.settings.CompactLanguageMenu
import com.example.nexus.ui.theme.Spacing

/**
 * Task detail destination for the `task/{projectId}/{taskId}` route.
 *
 * Shows the planning fields (due date, priority, status, labels, checklist) and
 * delegates full editing to [TaskEditorDialog], reusing [TaskListViewModel]'s
 * existing `loadTasks`/`updateTask` methods - no new backend calls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    projectId: String,
    taskId: String,
    onBack: () -> Unit,
    viewModel: TaskListViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditor by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        viewModel.loadTasks(projectId)
    }

    val task = uiState.tasks.firstOrNull { it.id == taskId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { CompactLanguageMenu() },
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            when {
                task == null && uiState.isLoading -> DetailSkeleton()
                task == null && !uiState.errorMessage.isNullOrBlank() ->
                    ErrorState(
                        message = uiState.errorMessage!!,
                        onRetry = { viewModel.loadTasks(projectId) },
                    )
                task == null ->
                    EmptyState(
                        message = "This task no longer exists.",
                        onAction = onBack,
                        actionLabel = "Go back",
                    )
                else -> TaskDetailContent(task = task, onEdit = { showEditor = true })
            }
        }
    }

    if (showEditor && task != null) {
        TaskEditorDialog(
            task = task,
            onDismiss = { showEditor = false },
            onSave = { draft ->
                viewModel.updateTask(task.id, draft)
                showEditor = false
            },
        )
    }
}

@Composable
private fun TaskDetailContent(
    task: Task,
    onEdit: () -> Unit,
) {
    val overdue = isTaskOverdue(task.dueDate, task.isCompleted)
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .testTag("task_detail_content")
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(text = task.title, style = MaterialTheme.typography.headlineSmall)

        val description = task.description
        if (!description.isNullOrEmpty()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            MetaChip(
                text =
                    when {
                        task.dueDate == null -> "No due date"
                        overdue -> "Overdue \u00b7 due ${task.dueDate}"
                        else -> "Due ${task.dueDate}"
                    },
                emphasized = overdue,
            )
            MetaChip(text = "Priority \u00b7 ${task.priority.displayName()}")
            MetaChip(text = "Status \u00b7 ${task.status.displayName()}")
        }

        if (task.labels.isNotEmpty()) {
            Text("Labels", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                task.labels.forEach { label -> MetaChip(text = label) }
            }
        }

        if (task.checklist.isNotEmpty()) {
            Text("Checklist", style = MaterialTheme.typography.titleMedium)
            task.checklist.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = item.isCompleted, onCheckedChange = null, enabled = false)
                    Text(item.title, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        NexusPrimaryButton(
            text = "Edit task",
            icon = Icons.Default.Edit,
            onClick = onEdit,
        )
    }
}

@Composable
private fun MetaChip(
    text: String,
    emphasized: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color =
            if (emphasized) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = Spacing.mdCompact, vertical = Spacing.smCompact),
            style = MaterialTheme.typography.labelMedium,
            color =
                if (emphasized) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
