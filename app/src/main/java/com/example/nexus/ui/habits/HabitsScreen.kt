package com.example.nexus.ui.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexus.api.GamificationStats
import com.example.nexus.api.Habit
import com.example.nexus.ui.components.CreateHabitDialog
import com.example.nexus.ui.components.ErrorState
import com.example.nexus.ui.components.HabitCard
import com.example.nexus.ui.components.ListSkeleton
import com.example.nexus.ui.components.NexusLogo
import com.example.nexus.ui.components.NexusSecondaryButton
import com.example.nexus.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    onBack: () -> Unit,
    viewModel: HabitsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var redeemedRewards by remember { mutableStateOf(emptySet<String>()) }

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
                    IconButton(onClick = viewModel::loadHabits) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add habit")
            }
        },
    ) { paddingValues ->
        when {
            state.isLoading && state.habits.isEmpty() -> ListSkeleton()
            !state.errorMessage.isNullOrBlank() && state.habits.isEmpty() ->
                ErrorState(
                    message = state.errorMessage!!,
                    onRetry = viewModel::loadHabits,
                )
            else ->
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = Spacing.md)
                            .testTag("habits_content"),
                    verticalArrangement = Arrangement.spacedBy(Spacing.smd),
                ) {
                    item {
                        Text("Habits", style = MaterialTheme.typography.headlineSmall)
                        Text("Build consistency, one small action at a time.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(Spacing.sm))
                        StatsCard(state.stats)
                    }
                    items(state.habits, key = { it.id }) { habit ->
                        HabitCard(
                            habit,
                            onToggle = { viewModel.toggleToday(habit) },
                            onEdit = { editingHabit = habit },
                            onDelete = { viewModel.deleteHabit(habit) },
                        )
                    }
                    item {
                        RewardsCard(
                            rewards = state.stats.availableRewards,
                            redeemed = redeemedRewards,
                            onRedeem = { redeemedRewards = redeemedRewards + it },
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
            },
        )
    }
    editingHabit?.let { habit ->
        CreateHabitDialog(
            initialHabit = habit,
            onDismiss = { editingHabit = null },
            onCreate = { name, frequency ->
                viewModel.editHabit(habit, name, frequency)
                editingHabit = null
            },
        )
    }
}

@Composable
private fun StatsCard(stats: GamificationStats) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.smCompact)) {
            Text("Level ${stats.level}  •  ${stats.points} points", style = MaterialTheme.typography.titleMedium)
            Text("Current streak: ${stats.currentStreak} days  •  Best: ${stats.bestStreak} days")
            Text("Completion rate: ${stats.completionRate}%")
            if (stats.badges.isNotEmpty()) Text("Badges: ${stats.badges.joinToString()}")
        }
    }
}

@Composable
private fun RewardsCard(
    rewards: List<String>,
    redeemed: Set<String>,
    onRedeem: (String) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text("Rewards", style = MaterialTheme.typography.titleMedium)
            rewards.forEach { reward ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(reward, modifier = Modifier.weight(1f))
                    NexusSecondaryButton(
                        text = if (reward in redeemed) "Redeemed" else "Redeem",
                        onClick = { onRedeem(reward) },
                        enabled = reward !in redeemed,
                    )
                }
            }
        }
    }
}
