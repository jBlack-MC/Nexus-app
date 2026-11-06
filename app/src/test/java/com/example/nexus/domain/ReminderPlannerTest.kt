package com.example.nexus.domain

import com.example.nexus.api.Task
import com.example.nexus.api.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderPlannerTest {
    private fun task(title: String, due: String?, done: Boolean = false) = Task(
        id = title,
        projectId = "p",
        title = title,
        dueDate = due,
        isCompleted = done,
        status = if (done) TaskStatus.DONE else TaskStatus.TODO,
    )

    @Test
    fun noReminder_whenNothingIsDueOrOverdue() {
        val tasks = listOf(task("Later", "2026-11-20"), task("Undated", null), task("Finished", "2026-11-01", done = true))
        assertNull(ReminderPlanner.plan(tasks, "2026-11-05"))
    }

    @Test
    fun countsOverdueAndDueToday_overdueFirstInHighlights() {
        val tasks =
            listOf(
                task("Today A", "2026-11-05"),
                task("Old", "2026-11-01"),
                task("Older", "2026-10-28"),
                task("Today B", "2026-11-05"),
                task("Done today", "2026-11-05", done = true),
            )
        val summary = ReminderPlanner.plan(tasks, "2026-11-05")!!
        assertEquals(2, summary.overdue)
        assertEquals(2, summary.dueToday)
        assertEquals(listOf("Older", "Old", "Today A"), summary.highlights)
    }
}
