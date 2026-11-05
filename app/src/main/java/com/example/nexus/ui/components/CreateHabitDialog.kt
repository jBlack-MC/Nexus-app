package com.example.nexus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import com.example.nexus.api.Habit
import com.example.nexus.api.HabitFrequency
import com.example.nexus.ui.theme.Spacing

@Composable
fun CreateHabitDialog(initialHabit: Habit? = null, onDismiss: () -> Unit, onCreate: (String, HabitFrequency) -> Unit,) {
    var name by remember(initialHabit?.id) { mutableStateOf(initialHabit?.name ?: "") }
    var frequency by remember(initialHabit?.id) { mutableStateOf(initialHabit?.frequency ?: HabitFrequency.DAILY) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialHabit == null) stringResource(R.string.habit_new) else stringResource(R.string.habit_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.habit_name)) },
                    singleLine = true,
                )
                HabitFrequency.entries.forEach { option ->
                    Row {
                        RadioButton(selected = frequency == option, onClick = { frequency = option })
                        Text(option.displayName(), Modifier.padding(top = Spacing.smd))
                    }
                }
            }
        },
        confirmButton = {
            NexusPrimaryButton(
                text = if (initialHabit == null) stringResource(R.string.action_create) else stringResource(R.string.action_save),
                onClick = { onCreate(name, frequency) },
                enabled = name.isNotBlank(),
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Localised habit frequency label (en/zu/tn). */
@Composable
fun com.example.nexus.api.HabitFrequency.displayName(): String = when (this) {
    com.example.nexus.api.HabitFrequency.DAILY -> stringResource(R.string.frequency_daily)
    com.example.nexus.api.HabitFrequency.WEEKLY -> stringResource(R.string.frequency_weekly)
    com.example.nexus.api.HabitFrequency.CUSTOM -> stringResource(R.string.frequency_custom)
}
