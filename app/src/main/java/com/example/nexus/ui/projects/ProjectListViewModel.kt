package com.example.nexus.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.api.CreateProjectRequest
import com.example.nexus.api.Project
import com.example.nexus.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProjectListUiState(
    val isLoading: Boolean = false,
    val projects: List<Project> = emptyList(),
    val errorMessage: String? = null
)

class ProjectListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProjectListUiState(isLoading = true))
    val uiState: StateFlow<ProjectListUiState> = _uiState

    init {
        loadProjects()
    }

    fun loadProjects() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                RetrofitClient.instance.getProjects()
            }.onSuccess { projects ->
                _uiState.value = ProjectListUiState(projects = projects)
            }.onFailure { error ->
                _uiState.value = ProjectListUiState(errorMessage = error.message ?: "Failed to load projects")
            }
        }
    }

    fun createProject(name: String, description: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching {
                RetrofitClient.instance.createProject(
                    CreateProjectRequest(name = name.trim(), description = description.trim().ifBlank { null })
                )
            }.onSuccess {
                loadProjects()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "Failed to create project")
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            runCatching {
                RetrofitClient.instance.deleteProject(projectId)
            }.onSuccess {
                loadProjects()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "Failed to delete project")
            }
        }
    }
}

