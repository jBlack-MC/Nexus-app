package com.example.nexus.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitModelsTest {
    @Test
    fun gamificationCalculatesCurrentBestAndCompletionRate() {
        val habit = Habit(
            id = "habit-1",
            name = "Read",
            completedDates = listOf("2026-09-21", "2026-09-22", "2026-09-23")
        )

        val stats = GamificationStats.from(listOf(habit), "2026-09-23")

        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.bestStreak)
        assertEquals(10, stats.completionRate)
        assertEquals(45, stats.points)
        assertEquals(1, stats.level)
        assertTrue("3-day streak" in stats.badges)
    }

    @Test
    fun gamificationBreaksCurrentStreakWhenTodayIsIncomplete() {
        val habit = Habit(
            id = "habit-1",
            name = "Read",
            completedDates = listOf("2026-09-20", "2026-09-21")
        )

        val stats = GamificationStats.from(listOf(habit), "2026-09-23")

        assertEquals(0, stats.currentStreak)
        assertEquals(2, stats.bestStreak)
    }
}
