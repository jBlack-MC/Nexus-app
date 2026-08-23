package com.example.nexus.api

data class Project(
    val id: String,
    val name: String,
    val description: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class CreateProjectRequest(
    val name: String,
    val description: String? = null
)

data class UpdateProjectRequest(
    val name: String,
    val description: String? = null
)

data class Task(
    val id: String,
    val projectId: String,
    val title: String,
    val description: String? = null,
    val isCompleted: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class CreateTaskRequest(
    val title: String,
    val description: String? = null
)

data class UpdateTaskRequest(
    val title: String,
    val description: String? = null,
    val isCompleted: Boolean? = null
)

