package com.example.nexus.data

import androidx.room.withTransaction
import com.example.nexus.api.ApiService
import com.example.nexus.api.CreateTaskRequest
import com.example.nexus.api.Task
import com.example.nexus.api.UpdateTaskRequest
import java.io.IOException

open class TaskRepository(
    private val database: NexusDatabase? = null,
    private val apiService: ApiService? = null,
    private val cacheOwner: () -> String? = { null },
    private val sync: SyncManager? = null,
) {
    private fun owner(): String? = cacheOwner()?.takeIf { it.isNotBlank() }

    private fun TaskEntity.toModel() =
        Task(id, projectId, title, description, isCompleted, dueDate, priority, status, labels, checklist, createdAt, updatedAt)

    private fun Task.toEntity(userId: String) =
        TaskEntity(id, userId, projectId, title, description, isCompleted, dueDate, priority, status, labels, checklist, createdAt, updatedAt)

    open suspend fun getTasks(projectId: String): List<Task> {
        val db = database ?: return apiService?.getTasks(projectId) ?: emptyList()
        val api = apiService ?: return db.taskDao().getTasksByProjectId(owner() ?: "", projectId).map { it.toModel() }
        val userId = owner() ?: return api.getTasks(projectId)
        if (sync != null) {
            return sync.read(
                remote = {
                    api.getTasks(projectId).also { rows ->
                        db.withTransaction {
                            db.taskDao().deleteTasksByProjectId(userId, projectId)
                            db.taskDao().insertTasks(rows.map { it.toEntity(userId) })
                        }
                    }
                },
                local = { db.taskDao().getTasksByProjectId(userId, projectId).map { it.toModel() } },
            )
        }
        return try {
            val networkTasks = api.getTasks(projectId)
            db.taskDao().insertTasks(networkTasks.map { it.toEntity(userId) })
            networkTasks
        } catch (e: IOException) {
            db.taskDao().getTasksByProjectId(userId, projectId).map { it.toModel() }
        }
    }

    open suspend fun getCachedTasks(): List<Task> =
        owner()?.let { userId -> database?.taskDao()?.getAllTasks(userId)?.map { it.toModel() } } ?: emptyList()

    open suspend fun createTask(projectId: String, request: CreateTaskRequest,): Task {
        val db = database ?: return apiService?.createTask(projectId, request) ?: throw IllegalStateException("No service")
        val api = apiService ?: throw IllegalStateException("No service")
        val userId = owner() ?: return api.createTask(projectId, request)
        if (sync != null) {
            val task = Task(
                java.util.UUID.randomUUID().toString(), projectId, request.title, request.description,
                request.status == com.example.nexus.api.TaskStatus.DONE, request.dueDate, request.priority, request.status, request.labels, request.checklist
            )
            sync.save("tasks", task.id, "create", task) { db.taskDao().insertTask(task.toEntity(it)) }
            return task
        }
        val task = api.createTask(projectId, request)
        db.taskDao().insertTask(task.toEntity(userId))
        return task
    }

    open suspend fun updateTask(taskId: String, request: UpdateTaskRequest,): Task {
        val db = database ?: return apiService?.updateTask(taskId, request) ?: throw IllegalStateException("No service")
        val api = apiService ?: throw IllegalStateException("No service")
        val userId = owner() ?: return api.updateTask(taskId, request)
        if (sync != null) {
            val previous = db.taskDao().getAllTasks(userId).firstOrNull { it.id == taskId } ?: error("Open this task before editing offline")
            val task = Task(
                taskId, previous.projectId, request.title, request.description,
                request.status == com.example.nexus.api.TaskStatus.DONE, request.dueDate, request.priority, request.status, request.labels, request.checklist,
                previous.createdAt, previous.updatedAt
            )
            sync.save("tasks", taskId, "update", task) { db.taskDao().insertTask(task.toEntity(it)) }
            return task
        }
        val task = api.updateTask(taskId, request)
        db.taskDao().insertTask(task.toEntity(userId))
        return task
    }

    open suspend fun deleteTask(taskId: String) {
        val userId = owner()
        if (sync != null && database != null) {
            sync.save("tasks", taskId, "delete", emptyMap<String, String>()) { database.taskDao().deleteTaskById(it, taskId) }
            return
        }
        apiService?.deleteTask(taskId)
        if (userId != null && database != null) database.taskDao().deleteTaskById(userId, taskId)
    }
}
