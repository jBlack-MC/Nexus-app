package com.example.nexus.data

import androidx.room.withTransaction
import com.example.nexus.api.ApiService
import com.example.nexus.api.CreateHabitRequest
import com.example.nexus.api.Habit
import com.example.nexus.api.UpdateHabitRequest
import java.io.IOException

open class HabitRepository(
    private val database: NexusDatabase? = null,
    private val apiService: ApiService? = null,
    private val cacheOwner: () -> String? = { null },
    private val sync: SyncManager? = null,
) {
    private fun owner(): String? = cacheOwner()?.takeIf { it.isNotBlank() }

    private fun HabitEntity.toModel() = Habit(id, name, description, frequency, targetDays, completedDates, createdAt)

    private fun Habit.toEntity(userId: String) = HabitEntity(
        id,
        userId,
        name,
        description,
        frequency,
        targetDays,
        completedDates,
        createdAt,
    )

    open suspend fun getHabits(): List<Habit> {
        val db = database ?: return apiService?.getHabits() ?: emptyList()
        val api = apiService ?: return db.habitDao().getHabits(owner() ?: "").map { it.toModel() }
        val userId = owner() ?: return api.getHabits()
        if (sync != null) {
            return sync.read(
                remote = {
                    api.getHabits().also { rows ->
                        db.withTransaction {
                            db.habitDao().clearForUser(userId)
                            db.habitDao().insertHabits(rows.map { it.toEntity(userId) })
                        }
                    }
                },
                local = { db.habitDao().getHabits(userId).map { it.toModel() } },
            )
        }
        return try {
            api.getHabits().also { habits ->
                db.habitDao().insertHabits(habits.map { it.toEntity(userId) })
            }
        } catch (_: IOException) {
            db.habitDao().getHabits(userId).map { it.toModel() }
        }
    }

    open suspend fun createHabit(request: CreateHabitRequest): Habit {
        val db = database ?: return apiService?.createHabit(request) ?: throw IllegalStateException("No service")
        val api = apiService ?: throw IllegalStateException("No service")
        val userId = owner() ?: return api.createHabit(request)
        if (sync != null) {
            val habit =
                Habit(java.util.UUID.randomUUID().toString(), request.name, request.description, request.frequency, request.targetDays)
            sync.save("habits", habit.id, "create", habit) { db.habitDao().insertHabit(habit.toEntity(it)) }
            return habit
        }
        val habit = api.createHabit(request)
        db.habitDao().insertHabit(habit.toEntity(userId))
        return habit
    }

    open suspend fun updateHabit(habit: Habit): Habit {
        val request = UpdateHabitRequest(habit.name, habit.description, habit.frequency, habit.targetDays, habit.completedDates)
        val db = database ?: return apiService?.updateHabit(habit.id, request) ?: habit
        val api = apiService ?: throw IllegalStateException("No service")
        val userId = owner() ?: return api.updateHabit(habit.id, request)
        if (sync != null) {
            sync.save("habits", habit.id, "update", habit) { db.habitDao().insertHabit(habit.toEntity(it)) }
            return habit
        }
        val updated = api.updateHabit(habit.id, request)
        db.habitDao().insertHabit(updated.toEntity(userId))
        return updated
    }

    open suspend fun deleteHabit(habitId: String) {
        val userId = owner()
        if (sync != null && database != null) {
            sync.save("habits", habitId, "delete", emptyMap<String, String>()) { database.habitDao().deleteHabit(it, habitId) }
            return
        }
        apiService?.deleteHabit(habitId)
        if (userId != null && database != null) database.habitDao().deleteHabit(userId, habitId)
    }
}
