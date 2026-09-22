package com.example.nexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.NexusApp
import com.example.nexus.api.CreateHabitRequest
import com.example.nexus.api.GamificationStats
import com.example.nexus.api.Habit
import com.example.nexus.api.HabitFrequency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HabitsUiState(
    val habits: List<Habit> = emptyList(),
    val stats: GamificationStats = GamificationStats.from(emptyList(), today()),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class HabitsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState: StateFlow<HabitsUiState> = _uiState

    init { loadHabits() }

    fun loadHabits() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { NexusApp.repository.getHabits() }
                .onSuccess { updateState(it) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Could not load habits") }
        }
    }

    fun createHabit(name: String, frequency: HabitFrequency) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching { NexusApp.repository.createHabit(CreateHabitRequest(name.trim(), frequency = frequency)) }
                .onSuccess { loadHabits() }
                .onFailure { _uiState.value = _uiState.value.copy(errorMessage = "Could not create habit") }
        }
    }

    fun toggleToday(habit: Habit) {
        val date = today()
        val completedDates = if (date in habit.completedDates) habit.completedDates - date else habit.completedDates + date
        viewModelScope.launch {
            runCatching { NexusApp.repository.updateHabit(habit.copy(completedDates = completedDates)) }
                .onSuccess { loadHabits() }
                .onFailure { _uiState.value = _uiState.value.copy(errorMessage = "Could not update habit") }
        }
    }

    fun editHabit(habit: Habit, name: String, frequency: HabitFrequency) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching { NexusApp.repository.updateHabit(habit.copy(name = name.trim(), frequency = frequency)) }
                .onSuccess { loadHabits() }
                .onFailure { _uiState.value = _uiState.value.copy(errorMessage = "Could not edit habit") }
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            NexusApp.repository.deleteHabit(habit.id)
            loadHabits()
        }
    }

    private fun updateState(habits: List<Habit>) {
        _uiState.value = HabitsUiState(habits = habits, stats = GamificationStats.from(habits, today()))
    }

    companion object {
        fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}

private fun today(): String = HabitsViewModel.today()
