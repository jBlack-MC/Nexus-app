package com.example.nexus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexus.api.Habit
import com.example.nexus.api.HabitFrequency
import com.example.nexus.ui.components.ErrorState
import com.example.nexus.ui.components.NexusLogo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    onBack: () -> Unit,
    viewModel: HabitsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var redeemedRewards by remember { mutableStateOf(emptySet<String>()) }

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
                    IconButton(onClick = viewModel::loadHabits) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add habit")
            }
        }
    ) { paddingValues ->
        when {
            state.isLoading && state.habits.isEmpty() -> CircularProgressIndicator(modifier = Modifier.padding(paddingValues))
            !state.errorMessage.isNullOrBlank() && state.habits.isEmpty() -> ErrorState(
                message = state.errorMessage!!,
                onRetry = viewModel::loadHabits
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Habits", style = MaterialTheme.typography.headlineSmall)
                    Text("Build consistency, one small action at a time.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    StatsCard(state.stats)
                }
                items(state.habits, key = { it.id }) { habit ->
                    HabitCard(
                        habit,
                        onToggle = { viewModel.toggleToday(habit) },
                        onEdit = { editingHabit = habit },
                        onDelete = { viewModel.deleteHabit(habit) }
                    )
                }
                item {
                    RewardsCard(
                        rewards = state.stats.availableRewards,
                        redeemed = redeemedRewards,
                        onRedeem = { redeemedRewards = redeemedRewards + it }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateHabitDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, frequency ->
                viewModel.createHabit(name, frequency)
                showCreateDialog = false
            }
        )
    }
    editingHabit?.let { habit ->
        CreateHabitDialog(
            initialHabit = habit,
            onDismiss = { editingHabit = null },
            onCreate = { name, frequency ->
                viewModel.editHabit(habit, name, frequency)
                editingHabit = null
            }
        )
    }
}

@Composable
private fun StatsCard(stats: com.example.nexus.api.GamificationStats) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Level ${stats.level}  •  ${stats.points} points", style = MaterialTheme.typography.titleMedium)
            Text("Current streak: ${stats.currentStreak} days  •  Best: ${stats.bestStreak} days")
            Text("Completion rate: ${stats.completionRate}%")
            if (stats.badges.isNotEmpty()) Text("Badges: ${stats.badges.joinToString()}")
        }
    }
}

@Composable
private fun HabitCard(habit: Habit, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val doneToday = HabitsViewModel.today() in habit.completedDates
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(habit.name, style = MaterialTheme.typography.titleMedium)
                    Text(habit.frequency.name.lowercase().replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit habit") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete habit") }
            }
            Button(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
                Text(if (doneToday) "Completed today" else "Complete today")
            }
        }
    }
}

@Composable
private fun RewardsCard(rewards: List<String>, redeemed: Set<String>, onRedeem: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Rewards", style = MaterialTheme.typography.titleMedium)
            rewards.forEach { reward ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(reward, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { onRedeem(reward) }, enabled = reward !in redeemed) {
                        Text(if (reward in redeemed) "Redeemed" else "Redeem")
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateHabitDialog(
    initialHabit: Habit? = null,
    onDismiss: () -> Unit,
    onCreate: (String, HabitFrequency) -> Unit
) {
    var name by remember(initialHabit?.id) { mutableStateOf(initialHabit?.name ?: "") }
    var frequency by remember(initialHabit?.id) { mutableStateOf(initialHabit?.frequency ?: HabitFrequency.DAILY) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialHabit == null) "New habit" else "Edit habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Habit name") }, singleLine = true)
                HabitFrequency.entries.forEach { option ->
                    Row {
                        RadioButton(selected = frequency == option, onClick = { frequency = option })
                        Text(option.name.lowercase().replaceFirstChar { it.uppercase() }, Modifier.padding(top = 12.dp))
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onCreate(name, frequency) }, enabled = name.isNotBlank()) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
