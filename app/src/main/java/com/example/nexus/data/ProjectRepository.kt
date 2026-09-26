package com.example.nexus.data

import com.example.nexus.api.ApiService
import com.example.nexus.api.CreateProjectRequest
import com.example.nexus.api.Project
import com.example.nexus.api.UpdateProjectRequest

open class ProjectRepository(
    private val database: NexusDatabase? = null,
    private val apiService: ApiService? = null,
    private val cacheOwner: () -> String? = { null },
) {
    private fun owner(): String? = cacheOwner()?.takeIf { it.isNotBlank() }

    private fun ProjectEntity.toModel() = Project(id, name, description, createdAt, updatedAt)

    private fun Project.toEntity(userId: String) = ProjectEntity(id, userId, name, description, createdAt, updatedAt)

    open suspend fun getProjects(): List<Project> {
        val db = database ?: return apiService?.getProjects() ?: emptyList()
        val api = apiService ?: return db.projectDao().getProjects(owner() ?: "").map { it.toModel() }
        val userId = owner() ?: return api.getProjects()
        return try {
            val networkProjects = api.getProjects()
            db.projectDao().insertProjects(networkProjects.map { it.toEntity(userId) })
            networkProjects
        } catch (e: Exception) {
            db.projectDao().getProjects(userId).map { it.toModel() }
        }
    }

    open suspend fun getProject(projectId: String): Project {
        val db = database ?: return apiService?.getProject(projectId) ?: throw IllegalStateException("No service")
        val api = apiService ?: return db.projectDao().getProjectById(owner() ?: "", projectId)?.toModel() ?: throw IllegalStateException("No project")
        val userId = owner() ?: return api.getProject(projectId)
        return try {
            val networkProject = api.getProject(projectId)
            db.projectDao().insertProject(networkProject.toEntity(userId))
            networkProject
        } catch (e: Exception) {
            db.projectDao().getProjectById(userId, projectId)?.toModel() ?: throw e
        }
    }

    open suspend fun createProject(request: CreateProjectRequest): Project {
        val db = database ?: return apiService?.createProject(request) ?: throw IllegalStateException("No service")
        val api = apiService ?: throw IllegalStateException("No service")
        val userId = owner() ?: return api.createProject(request)
        val project = api.createProject(request)
        db.projectDao().insertProject(project.toEntity(userId))
        return project
    }

    open suspend fun updateProject(
        projectId: String,
        request: UpdateProjectRequest,
    ): Project {
        val db = database ?: return apiService?.updateProject(projectId, request) ?: throw IllegalStateException("No service")
        val api = apiService ?: throw IllegalStateException("No service")
        val userId = owner() ?: return api.updateProject(projectId, request)
        val project = api.updateProject(projectId, request)
        db.projectDao().insertProject(project.toEntity(userId))
        return project
    }

    open suspend fun deleteProject(projectId: String) {
        val userId = owner()
        apiService?.deleteProject(projectId)
        if (userId != null && database != null) {
            database.projectDao().deleteProjectById(userId, projectId)
            database.taskDao().deleteTasksByProjectId(userId, projectId)
        }
    }
}
