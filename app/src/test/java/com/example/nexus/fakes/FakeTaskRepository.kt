package com.example.nexus.fakes

import com.example.nexus.api.CreateTaskRequest
import com.example.nexus.api.Task
import com.example.nexus.api.UpdateTaskRequest
import com.example.nexus.data.TaskRepository

class FakeTaskRepository(
    initialTasks: List<Task> = emptyList(),
    initialCachedTasks: List<Task> = emptyList(),
    var getTasksError: Throwable? = null,
    var getCachedTasksError: Throwable? = null,
    var createTaskError: Throwable? = null,
    var updateTaskError: Throwable? = null,
    var deleteTaskError: Throwable? = null,
) : TaskRepository() {
    val tasks = initialTasks.toMutableList()
    val cachedTasks = initialCachedTasks.toMutableList()

    val createdTasks = mutableListOf<Pair<String, CreateTaskRequest>>()
    val updatedTasks = mutableListOf<Pair<String, UpdateTaskRequest>>()
    val deletedTaskIds = mutableListOf<String>()

    override suspend fun getTasks(projectId: String): List<Task> {
        getTasksError?.let { throw it }
        return tasks.filter { it.projectId == projectId }
    }

    override suspend fun getCachedTasks(): List<Task> {
        getCachedTasksError?.let { throw it }
        return cachedTasks
    }

    override suspend fun createTask(
        projectId: String,
        request: CreateTaskRequest,
    ): Task {
        createTaskError?.let { throw it }
        createdTasks.add(projectId to request)
        val newTask =
            Task(
                id = "fake-task-${tasks.size + 1}",
                projectId = projectId,
                title = request.title,
                description = request.description,
                isCompleted = false,
                dueDate = request.dueDate,
                priority = request.priority,
                status = request.status,
                labels = request.labels,
                checklist = request.checklist,
            )
        tasks.add(newTask)
        return newTask
    }

    override suspend fun updateTask(
        taskId: String,
        request: UpdateTaskRequest,
    ): Task {
        updateTaskError?.let { throw it }
        updatedTasks.add(taskId to request)
        val index = tasks.indexOfFirst { it.id == taskId }
        val updated =
            if (index >= 0) {
                val existing = tasks[index]
                existing.copy(
                    title = request.title,
                    description = request.description,
                    isCompleted = request.isCompleted ?: existing.isCompleted,
                    dueDate = request.dueDate,
                    priority = request.priority,
                    status = request.status,
                    labels = request.labels,
                    checklist = request.checklist,
                ).also { tasks[index] = it }
            } else {
                Task(
                    id = taskId,
                    projectId = "project-1",
                    title = request.title,
                    description = request.description,
                    isCompleted = request.isCompleted ?: false,
                )
            }
        return updated
    }

    override suspend fun deleteTask(taskId: String) {
        deleteTaskError?.let { throw it }
        deletedTaskIds.add(taskId)
        tasks.removeAll { it.id == taskId }
    }

    val tasksLoader: suspend (String) -> List<Task> = { getTasks(it) }
    val cachedTasksLoader: suspend () -> List<Task> = { getCachedTasks() }
    val taskCreator: suspend (String, CreateTaskRequest) -> Task = { id, req -> createTask(id, req) }
    val taskUpdater: suspend (String, UpdateTaskRequest) -> Task = { id, req -> updateTask(id, req) }
    val taskDeleter: suspend (String) -> Unit = { deleteTask(it) }
}
