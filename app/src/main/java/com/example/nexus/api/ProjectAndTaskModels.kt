package com.example.nexus.api

data class Project(
    val id: String,
    val name: String,
    val description: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class CreateProjectRequest(
    val name: String,
    val description: String? = null,
)

data class UpdateProjectRequest(
    val name: String,
    val description: String? = null,
)

data class Task(
    val id: String,
    val projectId: String,
    val title: String,
    val description: String? = null,
    val isCompleted: Boolean = false,
    /** ISO-8601 date (yyyy-MM-dd), kept nullable for an undated task. */
    val dueDate: String? = null,
    val priority: TaskPriority = TaskPriority.NONE,
    val status: TaskStatus = if (isCompleted) TaskStatus.DONE else TaskStatus.TODO,
    val labels: List<String> = emptyList(),
    val checklist: List<ChecklistItem> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

enum class TaskPriority { NONE, LOW, MEDIUM, HIGH }

enum class TaskStatus { TODO, IN_PROGRESS, DONE }

data class ChecklistItem(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false,
)

data class CreateTaskRequest(
    val title: String,
    val description: String? = null,
    val dueDate: String? = null,
    val priority: TaskPriority = TaskPriority.NONE,
    val status: TaskStatus = TaskStatus.TODO,
    val labels: List<String> = emptyList(),
    val checklist: List<ChecklistItem> = emptyList(),
)

data class UpdateTaskRequest(
    val title: String,
    val description: String? = null,
    val isCompleted: Boolean? = null,
    val dueDate: String? = null,
    val priority: TaskPriority = TaskPriority.NONE,
    val status: TaskStatus = TaskStatus.TODO,
    val labels: List<String> = emptyList(),
    val checklist: List<ChecklistItem> = emptyList(),
)
