package com.example.nexus.domain

import com.example.nexus.api.GamificationStats
import com.example.nexus.api.Habit
import com.example.nexus.api.HabitFrequency
import java.util.Calendar

object HabitScoring {
    fun calculateStats(
        habits: List<Habit>,
        today: String,
    ): GamificationStats {
        val dates = habits.flatMap { it.completedDates }.toSet()
        val sortedDates = dates.sorted()
        val currentStreak = streakEndingAt(sortedDates, today)
        val bestStreak = longestStreak(sortedDates)
        val scheduled = habits.sumOf { if (it.frequency == HabitFrequency.DAILY) 30 else 4 }
        val completionRate = if (scheduled == 0) 0 else ((dates.size * 100) / scheduled).coerceAtMost(100)
        val points = dates.size * 10 + bestStreak * 5
        val level = (points / 100) + 1
        val badges =
            buildList {
                if (bestStreak >= 3) add("3-day streak")
                if (bestStreak >= 7) add("7-day streak")
                if (completionRate >= 80) add("Reliable habit builder")
            }
        return GamificationStats(
            points = points,
            level = level,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            completionRate = completionRate,
            badges = badges,
            availableRewards = listOf("Unlock level ${level + 1} theme", "Take a guilt-free break"),
        )
    }

    private fun streakEndingAt(
        dates: List<String>,
        end: String,
    ): Int {
        var streak = 0
        var cursor = end
        while (dates.binarySearch(cursor) >= 0) {
            streak++
            cursor = previousDate(cursor)
        }
        return streak
    }

    private fun longestStreak(dates: List<String>): Int {
        if (dates.isEmpty()) return 0
        var longest = 1
        var current = 1
        dates.zipWithNext().forEach { (previous, next) ->
            if (next == nextDate(previous)) current++ else current = 1
            longest = maxOf(longest, current)
        }
        return longest
    }

    private fun previousDate(date: String): String = shiftDate(date, -1)

    private fun nextDate(date: String): String = shiftDate(date, 1)

    private fun shiftDate(
        date: String,
        amount: Int,
    ): String {
        val parts = date.split('-').map(String::toInt)
        val calendar =
            Calendar.getInstance().apply {
                set(parts[0], parts[1] - 1, parts[2], 0, 0, 0)
                add(Calendar.DAY_OF_MONTH, amount)
            }
        return "%04d-%02d-%02d".format(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
        )
    }
}
