package com.example.nexus.fakes

import com.example.nexus.api.CreateHabitRequest
import com.example.nexus.api.Habit
import com.example.nexus.data.HabitRepository

class FakeHabitRepository(
    initialHabits: List<Habit> = emptyList(),
    var habitsError: Throwable? = null,
    var createHabitError: Throwable? = null,
    var updateHabitError: Throwable? = null,
    var deleteHabitError: Throwable? = null,
) : HabitRepository() {
    val habits = initialHabits.toMutableList()

    val createdHabits = mutableListOf<CreateHabitRequest>()
    val updatedHabits = mutableListOf<Habit>()
    val deletedHabitIds = mutableListOf<String>()

    override suspend fun getHabits(): List<Habit> {
        habitsError?.let { throw it }
        return habits
    }

    override suspend fun createHabit(request: CreateHabitRequest): Habit {
        createHabitError?.let { throw it }
        createdHabits.add(request)
        val habit =
            Habit(
                id = "fake-habit-${habits.size + 1}",
                name = request.name,
                description = request.description,
                frequency = request.frequency,
                targetDays = request.targetDays,
            )
        habits.add(habit)
        return habit
    }

    override suspend fun updateHabit(habit: Habit): Habit {
        updateHabitError?.let { throw it }
        updatedHabits.add(habit)
        val index = habits.indexOfFirst { it.id == habit.id }
        if (index >= 0) habits[index] = habit else habits.add(habit)
        return habit
    }

    override suspend fun deleteHabit(habitId: String) {
        deleteHabitError?.let { throw it }
        deletedHabitIds.add(habitId)
        habits.removeAll { it.id == habitId }
    }

    val habitsLoader: suspend () -> List<Habit> = { getHabits() }
    val habitCreator: suspend (CreateHabitRequest) -> Habit = { createHabit(it) }
    val habitUpdater: suspend (Habit) -> Habit = { updateHabit(it) }
    val habitDeleter: suspend (String) -> Unit = { deleteHabit(it) }
}
