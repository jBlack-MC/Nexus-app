package com.example.nexus.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.NexusApp
import com.example.nexus.api.CreateTaskRequest
import com.example.nexus.api.Project
import com.example.nexus.api.Task
import com.example.nexus.api.UpdateProjectRequest
import com.example.nexus.api.UpdateTaskRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProjectDetailUiState(
    val isLoading: Boolean = false,
    val project: Project? = null,
    val tasks: List<Task> = emptyList(),
    val errorMessage: String? = null,
    val isDeleted: Boolean = false
)

class ProjectDetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProjectDetailUiState(isLoading = true))
    val uiState: StateFlow<ProjectDetailUiState> = _uiState

    private var projectId: String? = null

    fun loadProject(projectId: String) {
        if (this.projectId == projectId && _uiState.value.project != null) return
        this.projectId = projectId

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val project = NexusApp.repository.getProject(projectId)
                val tasks = NexusApp.repository.getTasks(projectId)
                project to tasks
            }.onSuccess { (project, tasks) ->
                _uiState.value = ProjectDetailUiState(project = project, tasks = tasks)
            }.onFailure { error ->
                _uiState.value = ProjectDetailUiState(errorMessage = error.message ?: "Failed to load project")
            }
        }
    }

    fun refreshTasks() {
        val id = projectId ?: return
        viewModelScope.launch {
            runCatching {
                NexusApp.repository.getTasks(id)
            }.onSuccess { tasks ->
                _uiState.value = _uiState.value.copy(tasks = tasks, errorMessage = null)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "Failed to load tasks")
            }
        }
    }

    fun updateProject(name: String, description: String) {
        val id = projectId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                NexusApp.repository.updateProject(
                    id,
                    UpdateProjectRequest(name = name.trim(), description = description.trim().ifBlank { null })
                )
            }.onSuccess { project ->
                _uiState.value = _uiState.value.copy(isLoading = false, project = project)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to update project"
                )
            }
        }
    }

    fun createTask(title: String, description: String) {
        val id = projectId ?: return
        if (title.isBlank()) return
        viewModelScope.launch {
            runCatching {
                NexusApp.repository.createTask(
                    id,
                    CreateTaskRequest(title = title.trim(), description = description.trim().ifBlank { null })
                )
            }.onSuccess {
                refreshTasks()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "Failed to create task")
            }
        }
    }

    fun updateTask(task: Task, title: String, description: String, isCompleted: Boolean) {
        viewModelScope.launch {
            runCatching {
                NexusApp.repository.updateTask(
                    task.id,
                    UpdateTaskRequest(
                        title = title.trim(),
                        description = description.trim().ifBlank { null },
                        isCompleted = isCompleted
                    )
                )
            }.onSuccess {
                refreshTasks()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "Failed to update task")
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            runCatching {
                NexusApp.repository.deleteTask(taskId)
            }.onSuccess {
                refreshTasks()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "Failed to delete task")
            }
        }
    }

    fun deleteProject() {
        val id = projectId ?: return
        viewModelScope.launch {
            runCatching {
                NexusApp.repository.deleteProject(id)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isDeleted = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "Failed to delete project")
            }
        }
    }
}

