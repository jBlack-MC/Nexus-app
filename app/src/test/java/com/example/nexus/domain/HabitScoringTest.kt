package com.example.nexus.domain

import com.example.nexus.api.Habit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitScoringTest {
    @Test
    fun calculateStats_calculatesCurrentBestAndCompletionRate() {
        val habit =
            Habit(
                id = "habit-1",
                name = "Read",
                completedDates = listOf("2026-09-21", "2026-09-22", "2026-09-23"),
            )

        val stats = HabitScoring.calculateStats(listOf(habit), "2026-09-23")

        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.bestStreak)
        assertEquals(10, stats.completionRate)
        assertEquals(45, stats.points)
        assertEquals(1, stats.level)
        assertTrue("3-day streak" in stats.badges)
    }

    @Test
    fun calculateStats_breaksCurrentStreakWhenTodayIsIncomplete() {
        val habit =
            Habit(
                id = "habit-1",
                name = "Read",
                completedDates = listOf("2026-09-20", "2026-09-21"),
            )

        val stats = HabitScoring.calculateStats(listOf(habit), "2026-09-23")

        assertEquals(0, stats.currentStreak)
        assertEquals(2, stats.bestStreak)
    }

    @Test
    fun calculateStats_handlesGapInDatesCorrectly() {
        val habit =
            Habit(
                id = "habit-1",
                name = "Exercise",
                completedDates = listOf("2026-09-15", "2026-09-16", "2026-09-17", "2026-09-22", "2026-09-23"),
            )

        val stats = HabitScoring.calculateStats(listOf(habit), "2026-09-23")

        assertEquals(2, stats.currentStreak)
        assertEquals(3, stats.bestStreak)
    }

    @Test
    fun calculateStats_aggregatesMultipleHabits() {
        val habit1 =
            Habit(
                id = "habit-1",
                name = "Read",
                completedDates = listOf("2026-09-22", "2026-09-23"),
            )
        val habit2 =
            Habit(
                id = "habit-2",
                name = "Meditate",
                completedDates = listOf("2026-09-21", "2026-09-22"),
            )

        val stats = HabitScoring.calculateStats(listOf(habit1, habit2), "2026-09-23")

        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.bestStreak)
    }
}
