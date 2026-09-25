package com.example.nexus.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.NexusApp
import com.example.nexus.api.ApiError
import com.example.nexus.api.CreateTaskRequest
import com.example.nexus.api.Project
import com.example.nexus.api.Task
import com.example.nexus.api.TaskStatus
import com.example.nexus.api.UpdateProjectRequest
import com.example.nexus.api.UpdateTaskRequest
import com.example.nexus.api.toApiError
import com.example.nexus.api.toUserMessage
import com.example.nexus.auth.AuthSession
import com.example.nexus.data.ProjectRepository
import com.example.nexus.data.TaskRepository
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

class ProjectDetailViewModel(
    private val projectRepository: ProjectRepository? = null,
    private val taskRepository: TaskRepository? = null,
    private val projectLoader: suspend (String) -> Project = { (projectRepository ?: NexusApp.projectRepository).getProject(it) },
    private val tasksLoader: suspend (String) -> List<Task> = { (taskRepository ?: NexusApp.taskRepository).getTasks(it) },
    private val projectUpdater: suspend (String, UpdateProjectRequest) -> Project = { id, req -> (projectRepository ?: NexusApp.projectRepository).updateProject(id, req) },
    private val projectDeleter: suspend (String) -> Unit = { (projectRepository ?: NexusApp.projectRepository).deleteProject(it) },
    private val taskCreator: suspend (String, CreateTaskRequest) -> Task = { id, req -> (taskRepository ?: NexusApp.taskRepository).createTask(id, req) },
    private val taskUpdater: suspend (String, UpdateTaskRequest) -> Task = { id, req -> (taskRepository ?: NexusApp.taskRepository).updateTask(id, req) },
    private val taskDeleter: suspend (String) -> Unit = { (taskRepository ?: NexusApp.taskRepository).deleteTask(it) },
    private val clearSession: () -> Unit = { AuthSession.clearToken() }
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProjectDetailUiState(isLoading = true))
    val uiState: StateFlow<ProjectDetailUiState> = _uiState

    private var projectId: String? = null

    fun loadProject(projectId: String) {
        if (this.projectId == projectId && _uiState.value.project != null) return
        this.projectId = projectId

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val project = projectLoader(projectId)
                val tasks = tasksLoader(projectId)
                project to tasks
            }.onSuccess { (project, tasks) ->
                _uiState.value = ProjectDetailUiState(project = project, tasks = tasks)
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    fun refreshTasks() {
        val id = projectId ?: return
        viewModelScope.launch {
            runCatching {
                tasksLoader(id)
            }.onSuccess { tasks ->
                _uiState.value = _uiState.value.copy(tasks = tasks, errorMessage = null)
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    fun updateProject(name: String, description: String) {
        val id = projectId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                projectUpdater(
                    id,
                    UpdateProjectRequest(name = name.trim(), description = description.trim().ifBlank { null })
                )
            }.onSuccess { project ->
                _uiState.value = _uiState.value.copy(isLoading = false, project = project)
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    fun createTask(title: String, description: String) {
        val id = projectId ?: return
        if (title.isBlank()) return
        viewModelScope.launch {
            runCatching {
                taskCreator(
                    id,
                    CreateTaskRequest(title = title.trim(), description = description.trim().ifBlank { null })
                )
            }.onSuccess {
                refreshTasks()
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    fun updateTask(task: Task, title: String, description: String, isCompleted: Boolean) {
        viewModelScope.launch {
            runCatching {
                taskUpdater(
                    task.id,
                    UpdateTaskRequest(
                        title = title.trim(),
                        description = description.trim().ifBlank { null },
                        isCompleted = isCompleted,
                        dueDate = task.dueDate,
                        priority = task.priority,
                        status = if (isCompleted) TaskStatus.DONE else TaskStatus.TODO,
                        labels = task.labels,
                        checklist = task.checklist
                    )
                )
            }.onSuccess {
                refreshTasks()
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            runCatching {
                taskDeleter(taskId)
            }.onSuccess {
                refreshTasks()
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    fun deleteProject() {
        val id = projectId ?: return
        viewModelScope.launch {
            runCatching {
                projectDeleter(id)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isDeleted = true)
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    private fun handleError(error: Throwable) {
        val apiError = error.toApiError()
        if (apiError is ApiError.SessionExpired) {
            clearSession()
        }
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = apiError.toUserMessage()
        )
    }
}
