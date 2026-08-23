package com.example.nexus.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.api.CreateTaskRequest
import com.example.nexus.api.RetrofitClient
import com.example.nexus.api.Task
import com.example.nexus.api.UpdateTaskRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TaskListUiState(
    val isLoading: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val errorMessage: String? = null
)

class TaskListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TaskListUiState(isLoading = true))
    val uiState: StateFlow<TaskListUiState> = _uiState

    private var projectId: String? = null

    fun loadTasks(projectId: String) {
        this.projectId = projectId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                RetrofitClient.instance.getTasks(projectId)
            }.onSuccess { tasks ->
                _uiState.value = TaskListUiState(tasks = tasks)
            }.onFailure { error ->
                _uiState.value = TaskListUiState(errorMessage = error.message ?: "Failed to load tasks")
            }
        }
    }

    fun createTask(title: String, description: String) {
        val id = projectId ?: return
        if (title.isBlank()) return
        viewModelScope.launch {
            runCatching {
                RetrofitClient.instance.createTask(
                    id,
                    CreateTaskRequest(title = title.trim(), description = description.trim().ifBlank { null })
                )
            }.onSuccess {
                loadTasks(id)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "Failed to create task")
            }
        }
    }

    fun toggleCompleted(task: Task) {
        val id = projectId ?: return
        viewModelScope.launch {
            runCatching {
                RetrofitClient.instance.updateTask(
                    task.id,
                    UpdateTaskRequest(
                        title = task.title,
                        description = task.description,
                        isCompleted = !task.isCompleted
                    )
                )
            }.onSuccess {
                loadTasks(id)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "Failed to update task")
            }
        }
    }

    fun deleteTask(taskId: String) {
        val id = projectId ?: return
        viewModelScope.launch {
            runCatching {
                RetrofitClient.instance.deleteTask(taskId)
            }.onSuccess {
                loadTasks(id)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "Failed to delete task")
            }
        }
    }
}

