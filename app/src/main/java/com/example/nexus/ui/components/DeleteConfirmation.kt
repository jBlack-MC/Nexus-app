package com.example.nexus.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.nexus.R

/** What is about to be deleted; the dialog picks localised wording from the kind. */
enum class DeleteKind { PROJECT, TASK, HABIT }

class DeleteRequest(val kind: DeleteKind, val name: String, val onConfirm: () -> Unit)

/**
 * Deletes are permanent (and a project takes its tasks with it), so every list and detail screen
 * asks first. Usage: `val confirm = rememberDeleteConfirmation()`, call `confirm.ask(...)` from the
 * delete button, and place `DeleteConfirmationDialog(confirm)` once in the screen.
 */
@Stable
class DeleteConfirmation {
    var request by mutableStateOf<DeleteRequest?>(null)
        private set

    fun ask(kind: DeleteKind, name: String, onConfirm: () -> Unit) {
        request = DeleteRequest(kind, name, onConfirm)
    }

    fun dismiss() {
        request = null
    }
}

@Composable
fun rememberDeleteConfirmation(): DeleteConfirmation = remember { DeleteConfirmation() }

@Composable
fun DeleteConfirmationDialog(state: DeleteConfirmation) {
    val request = state.request ?: return
    val (title, body) =
        when (request.kind) {
            DeleteKind.PROJECT -> R.string.confirm_delete_project_title to R.string.confirm_delete_project_body
            DeleteKind.TASK -> R.string.confirm_delete_task_title to R.string.confirm_delete_task_body
            DeleteKind.HABIT -> R.string.confirm_delete_habit_title to R.string.confirm_delete_habit_body
        }
    AlertDialog(
        onDismissRequest = state::dismiss,
        title = { Text(stringResource(title)) },
        text = { Text(stringResource(body, request.name)) },
        confirmButton = {
            NexusDestructiveButton(
                text = stringResource(R.string.action_delete),
                onClick = {
                    request.onConfirm()
                    state.dismiss()
                },
                isFilled = true,
            )
        },
        dismissButton = { TextButton(onClick = state::dismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
