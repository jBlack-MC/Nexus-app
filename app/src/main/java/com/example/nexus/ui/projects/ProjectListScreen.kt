package com.example.nexus.ui.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
fun ProjectListScreen(
    onBackToDashboard: () -> Unit,
    onOpenProject: (String) -> Unit,
    viewModel: ProjectListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var newProjectName by remember { mutableStateOf("") }
    var newProjectDescription by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Projects") })
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
                value = newProjectName,
                onValueChange = { newProjectName = it },
                label = { Text("Project name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = newProjectDescription,
                onValueChange = { newProjectDescription = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    viewModel.createProject(newProjectName, newProjectDescription)
                    newProjectName = ""
                    newProjectDescription = ""
                }) {
                    Text("Create")
                }
                Button(onClick = onBackToDashboard) {
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
                items(uiState.projects, key = { it.id }) { project ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenProject(project.id) }
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = project.name, style = MaterialTheme.typography.titleMedium)
                            Text(text = project.description ?: "No description")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onOpenProject(project.id) }) {
                                    Text("Open")
                                }
                                Button(onClick = { viewModel.deleteProject(project.id) }) {
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

