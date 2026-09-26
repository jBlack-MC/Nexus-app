package com.example.nexus.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexus.settings.ThemeMode
import com.example.nexus.ui.components.DetailSkeleton
import com.example.nexus.ui.components.ErrorState
import com.example.nexus.ui.components.LogoutConfirmationDialog
import com.example.nexus.ui.components.NexusDestructiveButton
import com.example.nexus.ui.components.NexusLogo
import com.example.nexus.ui.components.NexusPrimaryButton
import com.example.nexus.ui.components.NexusSecondaryButton
import com.example.nexus.ui.components.PasswordDialog
import com.example.nexus.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var displayName by remember(uiState.profile?.displayName) {
        mutableStateOf(uiState.profile?.displayName ?: "")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { NexusLogo(iconSize = Spacing.TopBarLogoSize, textSize = Spacing.TopBarLogoTextSize) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Text(
                        "Settings",
                        modifier = Modifier.padding(end = Spacing.md),
                        style = MaterialTheme.typography.titleMedium,
                    )
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
            if (uiState.isLoading && uiState.profile == null) {
                DetailSkeleton()
            } else if (!uiState.errorMessage.isNullOrBlank() && uiState.profile == null) {
                ErrorState(
                    message = uiState.errorMessage!!,
                    onRetry = { viewModel.loadProfile() },
                )
            } else {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(Spacing.md)
                            .testTag("settings_content")
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    Text("User Profile", style = MaterialTheme.typography.titleMedium)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.smd)) {
                            OutlinedTextField(
                                value = uiState.profile?.email ?: "",
                                onValueChange = {},
                                label = { Text("Email") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                readOnly = true,
                            )

                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Display Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            )

                            NexusPrimaryButton(
                                text = if (uiState.isLoading) "Saving..." else "Save Changes",
                                onClick = { viewModel.updateProfile(displayName) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isLoading && displayName.isNotBlank() && displayName != uiState.profile?.displayName,
                                isLoading = uiState.isLoading,
                            )

                            if (!uiState.successMessage.isNullOrBlank()) {
                                Text(
                                    text = uiState.successMessage!!,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }

                    Text("Appearance", style = MaterialTheme.typography.titleMedium)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(Spacing.sm)) {
                            ThemeMode.entries.forEach { mode ->
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = Spacing.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = uiState.themeMode == mode,
                                        onClick = { viewModel.setThemeMode(mode) },
                                    )
                                    Spacer(Modifier.width(Spacing.sm))
                                    Text(mode.displayLabel())
                                }
                            }
                        }
                    }

                    Text("Language", style = MaterialTheme.typography.titleMedium)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        LanguageSelector(
                            selectedLanguage = uiState.selectedLanguage,
                            onLanguageSelected = viewModel::setLanguage,
                        )
                    }

                    Text("Notifications", style = MaterialTheme.typography.titleMedium)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.smd),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Task due-date reminders")
                            Switch(
                                checked = uiState.notificationsEnabled,
                                onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                            )
                        }
                    }

                    Text("Account", style = MaterialTheme.typography.titleMedium)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(Spacing.smd), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            NexusPrimaryButton(text = "Sync account", onClick = viewModel::loadProfile, modifier = Modifier.fillMaxWidth())
                            NexusSecondaryButton(text = "Export habits CSV", onClick = viewModel::exportCsv, modifier = Modifier.fillMaxWidth())
                            NexusSecondaryButton(
                                text = "Change password",
                                onClick = { showPasswordDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            NexusDestructiveButton(
                                text = "Delete account and data",
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (uiState.csvExport != null) {
                                NexusSecondaryButton(
                                    text = "Share CSV",
                                    onClick = {
                                        context.startActivity(
                                            Intent.createChooser(
                                                Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/csv"
                                                    putExtra(Intent.EXTRA_TEXT, uiState.csvExport)
                                                },
                                                "Share habits CSV",
                                            ),
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }

                    if (!uiState.errorMessage.isNullOrBlank() && uiState.profile != null) {
                        Text(
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    Spacer(Modifier.height(Spacing.lg))

                    NexusDestructiveButton(
                        text = "Log Out",
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(
                        text = "Version 1.0.0 (Prototype)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = Spacing.md),
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
            },
        )
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete account?") },
            text = { Text("This permanently deletes your account and synced data.") },
            confirmButton = {
                NexusDestructiveButton(
                    text = "Delete",
                    onClick = {
                        viewModel.deleteAccount()
                        showDeleteDialog = false
                        onLoggedOut()
                    },
                    isFilled = true,
                )
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
        )
    }
    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout()
                onLoggedOut()
            },
            onDismiss = { showLogoutDialog = false },
        )
    }
}

private fun ThemeMode.displayLabel(): String =
    when (this) {
        ThemeMode.SYSTEM -> "System default"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }
