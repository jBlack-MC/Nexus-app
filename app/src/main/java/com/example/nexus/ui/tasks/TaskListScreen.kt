package com.example.nexus.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexus.ui.components.NexusLogo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    projectId: String,
    onBackToProject: () -> Unit,
    viewModel: TaskListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var editingTaskId by remember { mutableStateOf<String?>(null) }
    var editingTaskCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        viewModel.loadTasks(projectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    NexusLogo(iconSize = 32.dp, textSize = 22)
                },
                actions = {
                    Text(
                        "Tasks",
                        modifier = Modifier.padding(end = 16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Task description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (editingTaskId == null) {
                        viewModel.createTask(title, description)
                    } else {
                        viewModel.updateTask(
                            taskId = editingTaskId ?: return@Button,
                            title = title,
                            description = description,
                            isCompleted = editingTaskCompleted
                        )
                    }
                    title = ""
                    description = ""
                    editingTaskId = null
                    editingTaskCompleted = false
                }) {
                    Text(if (editingTaskId == null) "Create" else "Save")
                }
                if (editingTaskId != null) {
                    Button(onClick = {
                        title = ""
                        description = ""
                        editingTaskId = null
                        editingTaskCompleted = false
                    }) {
                        Text("Cancel")
                    }
                }
                Button(onClick = onBackToProject) {
                    Text("Back")
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator()
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                Text(text = uiState.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.tasks, key = { it.id }) { task ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = task.title, style = MaterialTheme.typography.titleMedium)
                            Text(text = task.description ?: "No description")
                            Text(text = if (task.isCompleted) "Done" else "Open")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    editingTaskId = task.id
                                    title = task.title
                                    description = task.description ?: ""
                                    editingTaskCompleted = task.isCompleted
                                }) {
                                    Text("Edit")
                                }
                                Button(onClick = { viewModel.toggleCompleted(task) }) {
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
        }
    }
}
