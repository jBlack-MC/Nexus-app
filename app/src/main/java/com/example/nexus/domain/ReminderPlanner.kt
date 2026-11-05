package com.example.nexus.domain

import com.example.nexus.api.Task

/** What a due-date reminder should say. Null from [ReminderPlanner.plan] means "send nothing". */
data class ReminderSummary(
    val overdue: Int,
    val dueToday: Int,
    /** Up to three task titles, overdue first, for the notification's expanded text. */
    val highlights: List<String>,
)

/**
 * Pure due-date reminder logic, kept out of the worker so it can be unit-tested.
 * Dates are ISO `yyyy-MM-dd` strings, which compare correctly as text.
 */
object ReminderPlanner {
    fun plan(tasks: List<Task>, today: String): ReminderSummary? {
        val open = tasks.filter { !it.isCompleted && it.dueDate != null }
        val overdue = open.filter { it.dueDate!! < today }.sortedBy { it.dueDate }
        val dueToday = open.filter { it.dueDate == today }
        if (overdue.isEmpty() && dueToday.isEmpty()) return null
        return ReminderSummary(
            overdue = overdue.size,
            dueToday = dueToday.size,
            highlights = (overdue + dueToday).take(3).map { it.title },
        )
    }
}
