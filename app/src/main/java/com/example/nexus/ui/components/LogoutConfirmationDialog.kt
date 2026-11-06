package com.example.nexus.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.nexus.R

/**
 * Standard logout confirmation dialog used across Nexus entry points (e.g. Dashboard, Settings).
 */
@Composable
fun LogoutConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit,) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.logout_title)) },
        text = { Text(stringResource(R.string.logout_body)) },
        confirmButton = {
            NexusDestructiveButton(
                text = stringResource(R.string.logout_confirm),
                onClick = onConfirm,
                isFilled = true,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
