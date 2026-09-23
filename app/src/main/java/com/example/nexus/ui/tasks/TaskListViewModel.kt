package com.example.nexus.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.NexusApp
import com.example.nexus.api.ApiError
import com.example.nexus.api.ChecklistItem
import com.example.nexus.api.CreateTaskRequest
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

/**
 * The editable fields of a task. Kept separate from [Task] so the editor never has to invent
 * server-owned values such as the id, the project id or the timestamps.
 */
data class TaskDraft(
    val title: String,
    val description: String? = null,
    val dueDate: String? = null,
    val priority: TaskPriority = TaskPriority.NONE,
    val status: TaskStatus = TaskStatus.TODO,
    val labels: List<String> = emptyList(),
    val checklist: List<ChecklistItem> = emptyList()
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

    fun createTask(draft: TaskDraft) {
        val id = projectId ?: return
        if (draft.title.isBlank()) return
        viewModelScope.launch {
            runCatching {
                NexusApp.repository.createTask(id, draft.toCreateRequest())
            }.onSuccess {
                loadTasks(id)
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    fun updateTask(taskId: String, draft: TaskDraft) {
        val id = projectId ?: return
        if (draft.title.isBlank()) return
        viewModelScope.launch {
            runCatching {
                NexusApp.repository.updateTask(taskId, draft.toUpdateRequest())
            }.onSuccess {
                loadTasks(id)
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    /**
     * Flips completion using the same convention as the editor: a task is DONE only while it is
     * completed, and returns to TODO when it is reopened. Every other field is preserved.
     */
    fun toggleCompleted(task: Task) {
        updateTask(
            taskId = task.id,
            draft = TaskDraft(
                title = task.title,
                description = task.description,
                dueDate = task.dueDate,
                priority = task.priority,
                status = if (task.isCompleted) TaskStatus.TODO else TaskStatus.DONE,
                labels = task.labels,
                checklist = task.checklist
            )
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

    private fun TaskDraft.toCreateRequest() = CreateTaskRequest(
        title = title.trim(),
        description = description?.trim()?.ifBlank { null },
        dueDate = dueDate,
        priority = priority,
        status = status,
        labels = labels,
        checklist = checklist
    )

    private fun TaskDraft.toUpdateRequest() = UpdateTaskRequest(
        title = title.trim(),
        description = description?.trim()?.ifBlank { null },
        // Completion and status are two views of the same fact, so they are kept in step.
        isCompleted = status == TaskStatus.DONE,
        dueDate = dueDate,
        priority = priority,
        status = status,
        labels = labels,
        checklist = checklist
    )

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