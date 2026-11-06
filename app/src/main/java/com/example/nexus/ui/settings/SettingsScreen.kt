package com.example.nexus.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexus.R
import com.example.nexus.notifications.NexusNotifications
import com.example.nexus.notifications.Reminders
import com.example.nexus.settings.ThemeMode
import com.example.nexus.ui.components.DetailSkeleton
import com.example.nexus.ui.components.ErrorState
import com.example.nexus.ui.components.LogoutConfirmationDialog
import com.example.nexus.ui.components.NexusDestructiveButton
import com.example.nexus.ui.components.NexusLogo
import com.example.nexus.ui.components.NexusPrimaryButton
import com.example.nexus.ui.components.NexusSecondaryButton
import com.example.nexus.ui.components.PasswordDialog
import com.example.nexus.ui.components.localizedMessage
import com.example.nexus.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onLoggedOut: () -> Unit, viewModel: SettingsViewModel = viewModel(),) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.settings_share_csv_title)
    val appName = stringResource(R.string.app_name)
    val testBody = stringResource(R.string.notification_test_body)
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            permissionDenied = !granted
            if (granted) Reminders.runNow(context)
        }
    val requestPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissionDenied = true // notifications switched off for the app in system settings
        }
    }
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    Text(
                        stringResource(R.string.action_settings),
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
                    Text(stringResource(R.string.settings_user_profile), style = MaterialTheme.typography.titleMedium)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.smd)) {
                            OutlinedTextField(
                                value = uiState.profile?.email ?: "",
                                onValueChange = {},
                                label = { Text(stringResource(R.string.auth_email)) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                readOnly = true,
                            )

                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text(stringResource(R.string.auth_display_name)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            )

                            NexusPrimaryButton(
                                text = if (uiState.isLoading) {
                                    stringResource(R.string.settings_saving)
                                } else {
                                    stringResource(R.string.settings_save_changes)
                                },
                                onClick = { viewModel.updateProfile(displayName) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isLoading && displayName.isNotBlank() && displayName != uiState.profile?.displayName,
                                isLoading = uiState.isLoading,
                            )

                            if (!uiState.successMessage.isNullOrBlank()) {
                                Text(
                                    text = localizedMessage(uiState.successMessage!!),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }

                    Text(stringResource(R.string.settings_appearance), style = MaterialTheme.typography.titleMedium)
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

                    Text(stringResource(R.string.language_picker_label), style = MaterialTheme.typography.titleMedium)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        LanguageSelector(
                            selectedLanguage = uiState.selectedLanguage,
                            onLanguageSelected = viewModel::setLanguage,
                        )
                    }

                    Text(stringResource(R.string.settings_notifications), style = MaterialTheme.typography.titleMedium)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(Spacing.smd), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(stringResource(R.string.settings_reminders), modifier = Modifier.weight(1f))
                                Switch(
                                    checked = uiState.notificationsEnabled,
                                    onCheckedChange = { enabled ->
                                        viewModel.setNotificationsEnabled(enabled)
                                        if (enabled) {
                                            // Android 13+ asks for permission; reminders run as soon as it is granted.
                                            if (NexusNotifications.canPost(context)) Reminders.runNow(context) else requestPermission()
                                        }
                                    },
                                )
                            }
                            Text(
                                stringResource(R.string.settings_reminders_help),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            NexusSecondaryButton(
                                text = stringResource(R.string.settings_test_notification),
                                onClick = {
                                    if (!NexusNotifications.canPost(context)) {
                                        requestPermission()
                                    } else {
                                        // Local notification proves the channel works; the server push proves FCM end to end.
                                        NexusNotifications.show(
                                            context, NexusNotifications.ID_TEST, NexusNotifications.CHANNEL_UPDATES,
                                            appName, testBody,
                                        )
                                        viewModel.sendTestNotification()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            val push = uiState.pushTest
                            val pushMessage = when {
                                permissionDenied -> stringResource(R.string.settings_permission_denied)
                                uiState.pushTestFailed -> stringResource(R.string.settings_push_failed)
                                push == null -> null
                                !push.configured -> stringResource(R.string.settings_push_not_configured)
                                push.sent == 0 -> stringResource(R.string.settings_push_no_device)
                                else -> stringResource(R.string.settings_push_sent, push.sent)
                            }
                            if (pushMessage != null) {
                                Text(pushMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Text(stringResource(R.string.settings_account), style = MaterialTheme.typography.titleMedium)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(Spacing.smd), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            NexusPrimaryButton(
                                text = stringResource(R.string.settings_sync_account),
                                onClick = viewModel::loadProfile,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            NexusSecondaryButton(
                                text = stringResource(R.string.settings_export_csv),
                                onClick = viewModel::exportCsv,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            NexusSecondaryButton(
                                text = stringResource(R.string.password_change),
                                onClick = { showPasswordDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            NexusDestructiveButton(
                                text = stringResource(R.string.settings_delete_account),
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (uiState.csvExport != null) {
                                NexusSecondaryButton(
                                    text = stringResource(R.string.settings_share_csv),
                                    onClick = {
                                        context.startActivity(
                                            Intent.createChooser(
                                                Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/csv"
                                                    putExtra(Intent.EXTRA_TEXT, uiState.csvExport)
                                                },
                                                shareTitle,
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
                            text = localizedMessage(uiState.errorMessage!!),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    Spacer(Modifier.height(Spacing.lg))

                    NexusDestructiveButton(
                        text = stringResource(R.string.action_log_out),
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(
                        text = stringResource(R.string.settings_version, com.example.nexus.BuildConfig.VERSION_NAME),
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
            title = { Text(stringResource(R.string.settings_delete_title)) },
            text = { Text(stringResource(R.string.settings_delete_body)) },
            confirmButton = {
                NexusDestructiveButton(
                    text = stringResource(R.string.action_delete),
                    onClick = {
                        viewModel.deleteAccount()
                        showDeleteDialog = false
                        onLoggedOut()
                    },
                    isFilled = true,
                )
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.action_cancel)) } },
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

@Composable
private fun ThemeMode.displayLabel(): String = when (this) {
    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
}
