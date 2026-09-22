package com.example.nexus.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.NexusApp
import com.example.nexus.api.ApiError
import com.example.nexus.api.CreateProjectRequest
import com.example.nexus.api.Project
import com.example.nexus.api.toApiError
import com.example.nexus.api.toUserMessage
import com.example.nexus.auth.AuthSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProjectListUiState(
    val isLoading: Boolean = false,
    val projects: List<Project> = emptyList(),
    val errorMessage: String? = null
)

class ProjectListViewModel(
    private val projectsLoader: suspend () -> List<Project> = { NexusApp.repository.getProjects() },
    private val projectCreator: suspend (CreateProjectRequest) -> Project = { NexusApp.repository.createProject(it) },
    private val projectDeleter: suspend (String) -> Unit = { NexusApp.repository.deleteProject(it) },
    private val clearSession: () -> Unit = { AuthSession.clearToken() }
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProjectListUiState(isLoading = true))
    val uiState: StateFlow<ProjectListUiState> = _uiState

    init {
        loadProjects()
    }

    fun loadProjects() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                projectsLoader()
            }.onSuccess { projects ->
                _uiState.value = ProjectListUiState(projects = projects)
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    fun createProject(name: String, description: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching {
                projectCreator(
                    CreateProjectRequest(name = name.trim(), description = description.trim().ifBlank { null })
                )
            }.onSuccess {
                loadProjects()
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            runCatching {
                projectDeleter(projectId)
            }.onSuccess {
                loadProjects()
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
