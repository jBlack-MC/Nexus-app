package com.example.nexus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.nexus.R
import com.example.nexus.api.Habit
import com.example.nexus.ui.habits.HabitsViewModel
import com.example.nexus.ui.theme.Spacing

@Composable
fun HabitCard(habit: Habit, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier,) {
    val doneToday = HabitsViewModel.today() in habit.completedDates
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(habit.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        habit.frequency.displayName(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.habit_edit)) }
                NexusDestructiveIconButton(onClick = onDelete, contentDescription = stringResource(R.string.habit_delete))
            }
            NexusPrimaryButton(
                text = if (doneToday) stringResource(R.string.habit_completed_today) else stringResource(R.string.habit_complete_today),
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
