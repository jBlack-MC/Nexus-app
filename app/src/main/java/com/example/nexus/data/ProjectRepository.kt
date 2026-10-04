package com.example.nexus.data

import androidx.room.withTransaction
import com.example.nexus.api.ApiService
import com.example.nexus.api.CreateProjectRequest
import com.example.nexus.api.Project
import com.example.nexus.api.UpdateProjectRequest
import java.io.IOException

open class ProjectRepository(
    private val database: NexusDatabase? = null,
    private val apiService: ApiService? = null,
    private val cacheOwner: () -> String? = { null },
    private val sync: SyncManager? = null,
) {
    private fun owner(): String? = cacheOwner()?.takeIf { it.isNotBlank() }

    private fun ProjectEntity.toModel() = Project(id, name, description, createdAt, updatedAt)

    private fun Project.toEntity(userId: String) = ProjectEntity(id, userId, name, description, createdAt, updatedAt)

    open suspend fun getProjects(): List<Project> {
        val db = database ?: return apiService?.getProjects() ?: emptyList()
        val api = apiService ?: return db.projectDao().getProjects(owner() ?: "").map { it.toModel() }
        val userId = owner() ?: return api.getProjects()
        if (sync != null) {
            return sync.read(
                remote = {
                    api.getProjects().also { rows ->
                        db.withTransaction {
                            db.projectDao().clearForUser(userId)
                            db.projectDao().insertProjects(rows.map { it.toEntity(userId) })
                        }
                    }
                },
                local = { db.projectDao().getProjects(userId).map { it.toModel() } },
            )
        }
        return try {
            val networkProjects = api.getProjects()
            db.projectDao().insertProjects(networkProjects.map { it.toEntity(userId) })
            networkProjects
        } catch (e: IOException) {
            db.projectDao().getProjects(userId).map { it.toModel() }
        }
    }

    open suspend fun getProject(projectId: String): Project {
        val db = database ?: return apiService?.getProject(projectId) ?: throw IllegalStateException("No service")
        val api = apiService ?: return db.projectDao().getProjectById(owner() ?: "", projectId)?.toModel() ?: throw IllegalStateException("No project")
        val userId = owner() ?: return api.getProject(projectId)
        if (sync != null) {
            return sync.read(
                remote = { api.getProject(projectId).also { db.projectDao().insertProject(it.toEntity(userId)) } },
                local = { db.projectDao().getProjectById(userId, projectId)?.toModel() ?: error("Project is not available offline") },
            )
        }
        return try {
            val networkProject = api.getProject(projectId)
            db.projectDao().insertProject(networkProject.toEntity(userId))
            networkProject
        } catch (e: IOException) {
            db.projectDao().getProjectById(userId, projectId)?.toModel() ?: throw e
        }
    }

    open suspend fun createProject(request: CreateProjectRequest): Project {
        val db = database ?: return apiService?.createProject(request) ?: throw IllegalStateException("No service")
        val api = apiService ?: throw IllegalStateException("No service")
        val userId = owner() ?: return api.createProject(request)
        if (sync != null) {
            val project = Project(java.util.UUID.randomUUID().toString(), request.name, request.description)
            sync.save("projects", project.id, "create", project) { db.projectDao().insertProject(project.toEntity(it)) }
            return project
        }
        val project = api.createProject(request)
        db.projectDao().insertProject(project.toEntity(userId))
        return project
    }

    open suspend fun updateProject(projectId: String, request: UpdateProjectRequest,): Project {
        val db = database ?: return apiService?.updateProject(projectId, request) ?: throw IllegalStateException("No service")
        val api = apiService ?: throw IllegalStateException("No service")
        val userId = owner() ?: return api.updateProject(projectId, request)
        if (sync != null) {
            val project = Project(projectId, request.name, request.description)
            sync.save("projects", projectId, "update", project) { db.projectDao().insertProject(project.toEntity(it)) }
            return project
        }
        val project = api.updateProject(projectId, request)
        db.projectDao().insertProject(project.toEntity(userId))
        return project
    }

    open suspend fun deleteProject(projectId: String) {
        val userId = owner()
        if (sync != null && database != null) {
            sync.save("projects", projectId, "delete", emptyMap<String, String>()) {
                database.projectDao().deleteProjectById(it, projectId)
                database.taskDao().deleteTasksByProjectId(it, projectId)
            }
            return
        }
        apiService?.deleteProject(projectId)
        if (userId != null && database != null) {
            database.projectDao().deleteProjectById(userId, projectId)
            database.taskDao().deleteTasksByProjectId(userId, projectId)
        }
    }
}
