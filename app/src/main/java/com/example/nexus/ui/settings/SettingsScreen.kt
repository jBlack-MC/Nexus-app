package com.example.nexus.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexus.ui.components.ErrorState
import com.example.nexus.ui.components.NexusLogo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var displayName by remember(uiState.profile?.displayName) { 
        mutableStateOf(uiState.profile?.displayName ?: "") 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { NexusLogo(iconSize = 32.dp, textSize = 22) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Text(
                        "Settings",
                        modifier = Modifier.padding(end = 16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && uiState.profile == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (!uiState.errorMessage.isNullOrBlank() && uiState.profile == null) {
                ErrorState(
                    message = uiState.errorMessage!!,
                    onRetry = { viewModel.loadProfile() }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "User Profile",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    OutlinedTextField(
                        value = uiState.profile?.email ?: "",
                        onValueChange = {},
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        readOnly = true
                    )

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )

                    Button(
                        onClick = { viewModel.updateProfile(displayName) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading && displayName.isNotBlank() && displayName != uiState.profile?.displayName
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Save Changes")
                        }
                    }

                    if (!uiState.successMessage.isNullOrBlank()) {
                        Text(
                            text = uiState.successMessage!!,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (!uiState.errorMessage.isNullOrBlank() && uiState.profile != null) {
                        Text(
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "App Settings",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    // Simple informational text as placeholder for more settings
                    Text(
                        text = "Version 1.0.0 (Prototype)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
