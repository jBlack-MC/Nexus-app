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
import com.example.nexus.api.Habit
import com.example.nexus.api.HabitFrequency
import com.example.nexus.ui.theme.Spacing

@Composable
fun CreateHabitDialog(
    initialHabit: Habit? = null,
    onDismiss: () -> Unit,
    onCreate: (String, HabitFrequency) -> Unit,
) {
    var name by remember(initialHabit?.id) { mutableStateOf(initialHabit?.name ?: "") }
    var frequency by remember(initialHabit?.id) { mutableStateOf(initialHabit?.frequency ?: HabitFrequency.DAILY) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialHabit == null) "New habit" else "Edit habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Habit name") }, singleLine = true)
                HabitFrequency.entries.forEach { option ->
                    Row {
                        RadioButton(selected = frequency == option, onClick = { frequency = option })
                        Text(option.name.lowercase().replaceFirstChar { it.uppercase() }, Modifier.padding(top = Spacing.smd))
                    }
                }
            }
        },
        confirmButton = {
            NexusPrimaryButton(
                text = if (initialHabit == null) "Create" else "Save",
                onClick = { onCreate(name, frequency) },
                enabled = name.isNotBlank(),
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
