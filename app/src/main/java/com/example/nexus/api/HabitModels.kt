package com.example.nexus.api

data class Habit(
    val id: String,
    val name: String,
    val description: String? = null,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val targetDays: List<Int> = emptyList(),
    val completedDates: List<String> = emptyList(),
    val createdAt: String? = null
)

enum class HabitFrequency { DAILY, WEEKLY, CUSTOM }

data class CreateHabitRequest(
    val name: String,
    val description: String? = null,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val targetDays: List<Int> = emptyList()
)

data class UpdateHabitRequest(
    val name: String,
    val description: String? = null,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val targetDays: List<Int> = emptyList(),
    val completedDates: List<String> = emptyList()
)

data class GamificationStats(
    val points: Int,
    val level: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val completionRate: Int,
    val badges: List<String>,
    val availableRewards: List<String>,
    val redeemedRewards: List<String> = emptyList()
)
