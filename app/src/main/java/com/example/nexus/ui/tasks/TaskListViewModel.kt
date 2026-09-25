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
import com.example.nexus.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TaskListUiState(
    val isLoading: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val errorMessage: String? = null
)

data class TaskDraft(
    val title: String,
    val description: String? = null,
    val dueDate: String? = null,
    val priority: TaskPriority = TaskPriority.NONE,
    val status: TaskStatus = TaskStatus.TODO,
    val labels: List<String> = emptyList(),
    val checklist: List<ChecklistItem> = emptyList()
)

class TaskListViewModel(
    private val taskRepository: TaskRepository? = null,
    private val tasksLoader: suspend (String) -> List<Task> = { (taskRepository ?: NexusApp.taskRepository).getTasks(it) },
    private val taskCreator: suspend (String, CreateTaskRequest) -> Task = { id, req -> (taskRepository ?: NexusApp.taskRepository).createTask(id, req) },
    private val taskUpdater: suspend (String, UpdateTaskRequest) -> Task = { id, req -> (taskRepository ?: NexusApp.taskRepository).updateTask(id, req) },
    private val taskDeleter: suspend (String) -> Unit = { id -> (taskRepository ?: NexusApp.taskRepository).deleteTask(id) },
    private val clearSession: () -> Unit = { AuthSession.clearToken() }
) : ViewModel() {
    private val _uiState = MutableStateFlow(TaskListUiState(isLoading = true))
    val uiState: StateFlow<TaskListUiState> = _uiState

    private var projectId: String? = null

    fun loadTasks(projectId: String) {
        this.projectId = projectId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                tasksLoader(projectId)
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
                taskCreator(id, draft.toCreateRequest())
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
                taskUpdater(taskId, draft.toUpdateRequest())
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
                taskDeleter(taskId)
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
            clearSession()
        }
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = apiError.toUserMessage()
        )
    }
}
