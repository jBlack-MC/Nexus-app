package com.example.nexus.ui.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
    var name by remember(project?.id) { mutableStateOf(project?.name ?: "") }
    var description by remember(project?.id) { mutableStateOf(project?.description ?: "") }

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
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.updateProject(name, description) }) {
                        Text("Save")
                    }
                    Button(onClick = { onOpenTasks(project.id) }) {
                        Text("Tasks")
                    }
                    Button(onClick = { viewModel.deleteProject() }) {
                        Text("Delete")
                    }
                }
            }

            Button(onClick = onBackToProjects) {
                Text("Back")
            }
        }
    }
}

