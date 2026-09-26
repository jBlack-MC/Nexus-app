package com.example.nexus.ui.tasks

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexus.api.Task
import com.example.nexus.api.TaskPriority
import com.example.nexus.ui.components.EmptyState
import com.example.nexus.ui.components.ErrorState
import com.example.nexus.ui.components.ListSkeleton
import com.example.nexus.ui.components.Motion
import com.example.nexus.ui.components.NexusDestructiveIconButton
import com.example.nexus.ui.components.NexusLogo
import com.example.nexus.ui.components.rememberReduceMotion
import com.example.nexus.ui.settings.CompactLanguageMenu
import com.example.nexus.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    projectId: String,
    onBackToProject: () -> Unit,
    onOpenDetail: (String) -> Unit,
    viewModel: TaskListViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    var editorTask by remember { mutableStateOf<Task?>(null) }
    val reduceMotion = rememberReduceMotion()

    LaunchedEffect(projectId) {
        viewModel.loadTasks(projectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    NexusLogo(iconSize = Spacing.TopBarLogoSize, textSize = Spacing.TopBarLogoTextSize)
                },
                navigationIcon = {
                    IconButton(onClick = onBackToProject) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Text(
                        "Tasks",
                        modifier = Modifier.padding(end = Spacing.md),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    CompactLanguageMenu()
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editorTask = null
                showEditor = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "New Task")
            }
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            if (uiState.isLoading && uiState.tasks.isEmpty()) {
                ListSkeleton(labelWidth = 0.7f)
            } else {
                Box(modifier = Modifier.fillMaxSize().testTag("task_list_content")) {
                    if (!uiState.errorMessage.isNullOrBlank() && uiState.tasks.isEmpty()) {
                        ErrorState(
                            message = uiState.errorMessage!!,
                            onRetry = { viewModel.loadTasks(projectId) },
                        )
                    } else if (uiState.tasks.isEmpty()) {
                        EmptyState(
                            message = "No tasks yet. Add one to stay productive!",
                            onAction = {
                                editorTask = null
                                showEditor = true
                            },
                            actionLabel = "Add Task",
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            items(uiState.tasks, key = { it.id }) { task ->
                                TaskItem(
                                    task = task,
                                    onToggle = { viewModel.toggleCompleted(task) },
                                    onOpen = { onOpenDetail(task.id) },
                                    onDelete = { viewModel.deleteTask(task.id) },
                                    modifier =
                                        if (reduceMotion) {
                                            Modifier
                                        } else {
                                            Modifier.animateItem(
                                                fadeInSpec = tween(Motion.fadeMs),
                                                placementSpec = tween(Motion.itemMs, easing = FastOutSlowInEasing),
                                                fadeOutSpec = tween(Motion.fadeMs),
                                            )
                                        },
                                )
                            }
                        }
                    }
                }
            }

            if (!uiState.errorMessage.isNullOrBlank() && uiState.tasks.isNotEmpty()) {
                Snackbar(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(Spacing.md),
                    action = {
                        TextButton(onClick = { viewModel.loadTasks(projectId) }) {
                            Text("Retry", color = MaterialTheme.colorScheme.inversePrimary)
                        }
                    },
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
                },
            )
        }
    }
}

@Composable
fun TaskItem(
    modifier: Modifier = Modifier,
    task: Task,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val reduceMotion = rememberReduceMotion()
    val haptics = LocalHapticFeedback.current
    val checkScale = remember { Animatable(1f) }
    var firstComposition by remember(task.id) { mutableStateOf(true) }
    LaunchedEffect(task.isCompleted) {
        if (firstComposition) {
            firstComposition = false
        } else if (!reduceMotion) {
            // Completion feedback: brief checkbox scale bounce + haptic, skipped under reduced motion.
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            checkScale.animateTo(1.18f, tween(90, easing = FastOutLinearInEasing))
            checkScale.animateTo(1f, tween(140, easing = LinearOutSlowInEasing))
        }
    }
    val overdue = isTaskOverdue(task.dueDate, task.isCompleted)
    val metaParts =
        buildList {
            if (task.priority != TaskPriority.NONE) add(task.priority.displayName())
            add(task.status.displayName())
            if (task.labels.isNotEmpty()) add(task.labels.joinToString(", "))
            if (task.checklist.isNotEmpty()) {
                add("${task.checklist.count { it.isCompleted }}/${task.checklist.size} steps")
            }
        }
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                // Tapping the row opens the planning editor, matching the audit's
                // "wire TaskItem so tapping it navigates to the edit screen".
                .clickable(onClick = onOpen),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors =
            if (task.isCompleted) {
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            } else {
                CardDefaults.cardColors()
            },
    ) {
        Row(
            modifier =
                Modifier
                    .padding(Spacing.smd)
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                modifier =
                    Modifier.graphicsLayer {
                        scaleX = checkScale.value
                        scaleY = checkScale.value
                    },
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.outline else Color.Unspecified,
                )
                if (!task.description.isNullOrBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Text(
                    text = metaParts.joinToString("  ·  "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                task.dueDate?.let { due ->
                    Text(
                        text = if (overdue) "Due $due · Overdue" else "Due $due",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onOpen) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            NexusDestructiveIconButton(onClick = onDelete, contentDescription = "Delete")
        }
    }
}
