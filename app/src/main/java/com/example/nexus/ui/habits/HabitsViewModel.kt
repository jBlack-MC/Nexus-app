package com.example.nexus.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.NexusApp
import com.example.nexus.api.CreateHabitRequest
import com.example.nexus.api.GamificationStats
import com.example.nexus.api.Habit
import com.example.nexus.api.HabitFrequency
import com.example.nexus.data.HabitRepository
import com.example.nexus.domain.HabitScoring
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HabitsUiState(
    val habits: List<Habit> = emptyList(),
    val stats: GamificationStats = HabitScoring.calculateStats(emptyList(), today()),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class HabitsViewModel(
    private val habitRepository: HabitRepository? = null,
    private val habitsLoader: suspend () -> List<Habit> = { (habitRepository ?: NexusApp.habitRepository).getHabits() },
    private val habitCreator: suspend (CreateHabitRequest) -> Habit = { (habitRepository ?: NexusApp.habitRepository).createHabit(it) },
    private val habitUpdater: suspend (Habit) -> Habit = { (habitRepository ?: NexusApp.habitRepository).updateHabit(it) },
    private val habitDeleter: suspend (String) -> Unit = { (habitRepository ?: NexusApp.habitRepository).deleteHabit(it) },
) : ViewModel() {
    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState: StateFlow<HabitsUiState> = _uiState

    init {
        loadHabits()
    }

    fun loadHabits() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { habitsLoader() }
                .onSuccess { updateState(it) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Could not load habits") }
        }
    }

    fun createHabit(
        name: String,
        frequency: HabitFrequency,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching { habitCreator(CreateHabitRequest(name.trim(), frequency = frequency)) }
                .onSuccess { loadHabits() }
                .onFailure { _uiState.value = _uiState.value.copy(errorMessage = "Could not create habit") }
        }
    }

    fun toggleToday(habit: Habit) {
        val date = today()
        val completedDates = if (date in habit.completedDates) habit.completedDates - date else habit.completedDates + date
        viewModelScope.launch {
            runCatching { habitUpdater(habit.copy(completedDates = completedDates)) }
                .onSuccess { loadHabits() }
                .onFailure { _uiState.value = _uiState.value.copy(errorMessage = "Could not update habit") }
        }
    }

    fun editHabit(
        habit: Habit,
        name: String,
        frequency: HabitFrequency,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching { habitUpdater(habit.copy(name = name.trim(), frequency = frequency)) }
                .onSuccess { loadHabits() }
                .onFailure { _uiState.value = _uiState.value.copy(errorMessage = "Could not edit habit") }
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitDeleter(habit.id)
            loadHabits()
        }
    }

    private fun updateState(habits: List<Habit>) {
        _uiState.value = HabitsUiState(habits = habits, stats = HabitScoring.calculateStats(habits, today()))
    }

    companion object {
        fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}

private fun today(): String = HabitsViewModel.today()
