package com.example.nexus.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.NexusApp
import com.example.nexus.api.ApiError
import com.example.nexus.api.CreateTaskRequest
import com.example.nexus.api.ChecklistItem
import com.example.nexus.api.Task
import com.example.nexus.api.TaskPriority
import com.example.nexus.api.TaskStatus
import com.example.nexus.api.UpdateTaskRequest
import com.example.nexus.api.toApiError
import com.example.nexus.api.toUserMessage
import com.example.nexus.auth.AuthSession
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
                NexusApp.repository.getTasks(projectId)
            }.onSuccess { tasks ->
                _uiState.value = TaskListUiState(tasks = tasks)
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    fun createTask(title: String, description: String, dueDate: String?, priority: TaskPriority, status: TaskStatus, labels: List<String>, checklist: List<ChecklistItem>) {
        val id = projectId ?: return
        if (title.isBlank()) return
        viewModelScope.launch {
            runCatching {
                NexusApp.repository.createTask(
                    id,
                    CreateTaskRequest(title.trim(), description.trim().ifBlank { null }, dueDate, priority, status, labels, checklist)
                )
            }.onSuccess {
                loadTasks(id)
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    fun updateTask(taskId: String, title: String, description: String, isCompleted: Boolean, dueDate: String? = null, priority: TaskPriority = TaskPriority.NONE, status: TaskStatus = if (isCompleted) TaskStatus.DONE else TaskStatus.TODO, labels: List<String> = emptyList(), checklist: List<ChecklistItem> = emptyList()) {
        val id = projectId ?: return
        if (title.isBlank()) return
        viewModelScope.launch {
            runCatching {
                NexusApp.repository.updateTask(
                    taskId,
                    UpdateTaskRequest(title.trim(), description.trim().ifBlank { null }, isCompleted, dueDate, priority, status, labels, checklist)
                )
            }.onSuccess {
                loadTasks(id)
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    fun toggleCompleted(task: Task) {
        updateTask(
            taskId = task.id,
            title = task.title,
            description = task.description ?: "",
            isCompleted = !task.isCompleted,
            dueDate = task.dueDate,
            priority = task.priority,
            status = if (!task.isCompleted) TaskStatus.DONE else TaskStatus.TODO,
            labels = task.labels,
            checklist = task.checklist
        )
    }

    fun deleteTask(taskId: String) {
        val id = projectId ?: return
        viewModelScope.launch {
            runCatching {
                NexusApp.repository.deleteTask(taskId)
            }.onSuccess {
                loadTasks(id)
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    private fun handleError(error: Throwable) {
        val apiError = error.toApiError()
        if (apiError is ApiError.SessionExpired) {
            AuthSession.clearToken()
        }
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = apiError.toUserMessage()
        )
    }
}
