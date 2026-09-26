package com.example.nexus.fakes

import com.example.nexus.api.CreateProjectRequest
import com.example.nexus.api.Project
import com.example.nexus.api.UpdateProjectRequest
import com.example.nexus.data.ProjectRepository

class FakeProjectRepository(
    initialProjects: List<Project> = emptyList(),
    var projectsError: Throwable? = null,
    var getProjectError: Throwable? = null,
    var createProjectError: Throwable? = null,
    var updateProjectError: Throwable? = null,
    var deleteProjectError: Throwable? = null,
) : ProjectRepository() {
    val projects = initialProjects.toMutableList()

    val createdProjects = mutableListOf<CreateProjectRequest>()
    val updatedProjects = mutableListOf<Pair<String, UpdateProjectRequest>>()
    val deletedProjectIds = mutableListOf<String>()

    override suspend fun getProjects(): List<Project> {
        projectsError?.let { throw it }
        return projects
    }

    override suspend fun getProject(projectId: String): Project {
        getProjectError?.let { throw it }
        return projects.firstOrNull { it.id == projectId }
            ?: throw NoSuchElementException("Project not found: $projectId")
    }

    override suspend fun createProject(request: CreateProjectRequest): Project {
        createProjectError?.let { throw it }
        createdProjects.add(request)
        val newProject =
            Project(
                id = "fake-project-${projects.size + 1}",
                name = request.name,
                description = request.description,
            )
        projects.add(newProject)
        return newProject
    }

    override suspend fun updateProject(
        projectId: String,
        request: UpdateProjectRequest,
    ): Project {
        updateProjectError?.let { throw it }
        updatedProjects.add(projectId to request)
        val index = projects.indexOfFirst { it.id == projectId }
        val updated =
            if (index >= 0) {
                projects[index].copy(name = request.name, description = request.description).also {
                    projects[index] = it
                }
            } else {
                Project(id = projectId, name = request.name, description = request.description)
            }
        return updated
    }

    override suspend fun deleteProject(projectId: String) {
        deleteProjectError?.let { throw it }
        deletedProjectIds.add(projectId)
        projects.removeAll { it.id == projectId }
    }

    val projectsLoader: suspend () -> List<Project> = { getProjects() }
    val projectLoader: suspend (String) -> Project = { getProject(it) }
    val projectCreator: suspend (CreateProjectRequest) -> Project = { createProject(it) }
    val projectUpdater: suspend (String, UpdateProjectRequest) -> Project = { id, req -> updateProject(id, req) }
    val projectDeleter: suspend (String) -> Unit = { deleteProject(it) }
}
