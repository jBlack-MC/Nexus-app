package com.example.nexus.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.api.Project
import com.example.nexus.api.RetrofitClient
import com.example.nexus.api.UpdateProjectRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProjectDetailUiState(
    val isLoading: Boolean = false,
    val project: Project? = null,
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
            _uiState.value = ProjectDetailUiState(isLoading = true)
            runCatching {
                RetrofitClient.instance.getProject(projectId)
            }.onSuccess { project ->
                _uiState.value = ProjectDetailUiState(project = project)
            }.onFailure { error ->
                _uiState.value = ProjectDetailUiState(errorMessage = error.message ?: "Failed to load project")
            }
        }
    }

    fun updateProject(name: String, description: String) {
        val id = projectId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                RetrofitClient.instance.updateProject(
                    id,
                    UpdateProjectRequest(name = name.trim(), description = description.trim().ifBlank { null })
                )
            }.onSuccess { project ->
                _uiState.value = ProjectDetailUiState(project = project)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to update project"
                )
            }
        }
    }

    fun deleteProject() {
        val id = projectId ?: return
        viewModelScope.launch {
            runCatching {
                RetrofitClient.instance.deleteProject(id)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isDeleted = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "Failed to delete project")
            }
        }
    }
}

