package com.example.nexus.ui.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    onBackToProjects: () -> Unit,
    onOpenTasks: (String) -> Unit,
    viewModel: ProjectDetailViewModel = viewModel()
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
    var projectName by remember(project?.id) { mutableStateOf(project?.name ?: "") }
    var projectDescription by remember(project?.id) { mutableStateOf(project?.description ?: "") }

    var taskTitle by remember { mutableStateOf("") }
    var taskDescription by remember { mutableStateOf("") }
    var editingTaskId by remember { mutableStateOf<String?>(null) }
    var editingTaskCompleted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Project Detail") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                Text(text = uiState.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
            }

            if (project != null) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = projectDescription,
                    onValueChange = { projectDescription = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.updateProject(projectName, projectDescription) }) {
                        Text("Save Project")
                    }
                    Button(onClick = { onOpenTasks(project.id) }) {
                        Text("Open Tasks Screen")
                    }
                    Button(onClick = { viewModel.deleteProject() }) {
                        Text("Delete Project")
                    }
                }

                Text("Tasks", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    label = { Text("Task title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = taskDescription,
                    onValueChange = { taskDescription = it },
                    label = { Text("Task description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val editingTask = uiState.tasks.firstOrNull { it.id == editingTaskId }
                        if (editingTask != null) {
                            viewModel.updateTask(
                                task = editingTask,
                                title = taskTitle,
                                description = taskDescription,
                                isCompleted = editingTaskCompleted
                            )
                        } else {
                            viewModel.createTask(taskTitle, taskDescription)
                        }
                        taskTitle = ""
                        taskDescription = ""
                        editingTaskId = null
                        editingTaskCompleted = false
                    }) {
                        Text(if (editingTaskId == null) "Create Task" else "Save Task")
                    }
                    if (editingTaskId != null) {
                        Button(onClick = {
                            taskTitle = ""
                            taskDescription = ""
                            editingTaskId = null
                            editingTaskCompleted = false
                        }) {
                            Text("Cancel")
                        }
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.tasks, key = { it.id }) { task ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = task.title, style = MaterialTheme.typography.titleSmall)
                            Text(text = task.description ?: "No description")
                            Text(text = if (task.isCompleted) "Done" else "Open")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    editingTaskId = task.id
                                    taskTitle = task.title
                                    taskDescription = task.description ?: ""
                                    editingTaskCompleted = task.isCompleted
                                }) {
                                    Text("Edit")
                                }
                                Button(onClick = {
                                    viewModel.updateTask(
                                        task = task,
                                        title = task.title,
                                        description = task.description ?: "",
                                        isCompleted = !task.isCompleted
                                    )
                                }) {
                                    Text(if (task.isCompleted) "Mark Open" else "Complete")
                                }
                                Button(onClick = { viewModel.deleteTask(task.id) }) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }

            Button(onClick = onBackToProjects) {
                Text("Back")
            }
        }
    }
}
