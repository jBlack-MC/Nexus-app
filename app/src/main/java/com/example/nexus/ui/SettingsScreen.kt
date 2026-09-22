package com.example.nexus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexus.settings.ThemeMode
import com.example.nexus.ui.components.ErrorState
import com.example.nexus.ui.components.NexusLogo
import com.example.nexus.ui.components.DetailSkeleton
import android.content.Intent
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
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
                DetailSkeleton()
            } else if (!uiState.errorMessage.isNullOrBlank() && uiState.profile == null) {
                ErrorState(
                    message = uiState.errorMessage!!,
                    onRetry = { viewModel.loadProfile() }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("User Profile", style = MaterialTheme.typography.titleMedium)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                    Text("Saving...", style = MaterialTheme.typography.labelLarge)
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
                        }
                    }

                    Text("Appearance", style = MaterialTheme.typography.titleMedium)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(8.dp)) {
                            ThemeMode.entries.forEach { mode ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = uiState.themeMode == mode,
                                        onClick = { viewModel.setThemeMode(mode) }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(mode.displayLabel())
                                }
                            }
                        }
                    }

                    Text("Language", style = MaterialTheme.typography.titleMedium)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        LanguageSelector(
                            selectedLanguage = uiState.selectedLanguage,
                            onLanguageSelected = viewModel::setLanguage
                        )
                    }

                    Text("Notifications", style = MaterialTheme.typography.titleMedium)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Task due-date reminders")
                            Switch(
                                checked = uiState.notificationsEnabled,
                                onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                            )
                        }
                    }

                    Text("Account", style = MaterialTheme.typography.titleMedium)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::loadProfile, modifier = Modifier.fillMaxWidth()) { Text("Sync account") }
                            OutlinedButton(onClick = viewModel::exportCsv, modifier = Modifier.fillMaxWidth()) { Text("Export habits CSV") }
                            OutlinedButton(onClick = { showPasswordDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Change password") }
                            OutlinedButton(onClick = { showDeleteDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Delete account and data") }
                            if (uiState.csvExport != null) {
                                Button(
                                    onClick = {
                                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                            type = "text/csv"
                                            putExtra(Intent.EXTRA_TEXT, uiState.csvExport)
                                        }, "Share habits CSV"))
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Share CSV") }
                            }
                        }
                    }

                    if (!uiState.errorMessage.isNullOrBlank() && uiState.profile != null) {
                        Text(
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.logout()
                            onLoggedOut()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                    ) {
                        Text("Log Out")
                    }

                    Text(
                        text = "Version 1.0.0 (Prototype)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
                    )
                }
            }
        }
    }

    if (showPasswordDialog) {
        PasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onChange = { current, new ->
                viewModel.changePassword(current, new)
                showPasswordDialog = false
            }
        )
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete account?") },
            text = { Text("This permanently deletes your account and synced data.") },
            confirmButton = {
                Button(onClick = { viewModel.deleteAccount(); showDeleteDialog = false; onLoggedOut() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PasswordDialog(onDismiss: () -> Unit, onChange: (String, String) -> Unit) {
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(current, { current = it }, label = { Text("Current password") })
                OutlinedTextField(newPassword, { newPassword = it }, label = { Text("New password") })
            }
        },
        confirmButton = { Button(onClick = { onChange(current, newPassword) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelector(selectedLanguage: String, onLanguageSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val languages = supportedLanguages
    val selectedLabel = languages.firstOrNull { it.first == selectedLanguage }?.second ?: selectedLanguage

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("App language") },
            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth().padding(12.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            languages.forEach { (code, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onLanguageSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun ThemeMode.displayLabel(): String = when (this) {
    ThemeMode.SYSTEM -> "System default"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}
