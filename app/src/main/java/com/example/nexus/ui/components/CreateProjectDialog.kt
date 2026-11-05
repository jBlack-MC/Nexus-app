package com.example.nexus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.nexus.R
import com.example.nexus.api.Project
import com.example.nexus.ui.theme.Spacing

@Composable
fun CreateProjectDialog(initialProject: Project? = null, onDismiss: () -> Unit, onCreate: (String, String) -> Unit,) {
    var name by remember(initialProject?.id) { mutableStateOf(initialProject?.name ?: "") }
    var desc by remember(initialProject?.id) { mutableStateOf(initialProject?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialProject == null) stringResource(R.string.project_new) else stringResource(R.string.project_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.field_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text(stringResource(R.string.field_description_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            NexusPrimaryButton(
                text = if (initialProject == null) stringResource(R.string.action_create) else stringResource(R.string.action_save),
                onClick = { onCreate(name, desc) },
                enabled = name.isNotBlank(),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
