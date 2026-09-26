package com.example.nexus.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.NexusApp
import com.example.nexus.api.ApiError
import com.example.nexus.api.DashboardData
import com.example.nexus.api.Task
import com.example.nexus.api.toApiError
import com.example.nexus.api.toUserMessage
import com.example.nexus.auth.AuthSession
import com.example.nexus.data.DashboardRepository
import com.example.nexus.data.TaskRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class DashboardState {
    data object Loading : DashboardState()

    data class Success(
        val data: DashboardData,
        val cachedTasks: List<Task>,
        val isStale: Boolean = false,
    ) : DashboardState()

    data class Error(val message: String) : DashboardState()
}

class DashboardViewModel(
    private val dashboardRepository: DashboardRepository? = null,
    private val taskRepository: TaskRepository? = null,
    private val dashboardLoader: suspend () -> DashboardData = { (dashboardRepository ?: NexusApp.dashboardRepository).getDashboard() },
    private val cachedTasksLoader: suspend () -> List<Task> = { (taskRepository ?: NexusApp.taskRepository).getCachedTasks() },
    private val cachedDashboardLoader: suspend () -> DashboardData? = {
        (dashboardRepository ?: NexusApp.dashboardRepository).getCachedDashboard()
    },
    private val clearSession: () -> Unit = { AuthSession.clearToken() },
) : ViewModel() {
    private val _uiState = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val uiState: StateFlow<DashboardState> = _uiState

    init {
        fetchDashboard()
    }

    fun fetchDashboard(softRefresh: Boolean = false) {
        viewModelScope.launch {
            val previousState = _uiState.value
            if (!softRefresh) {
                _uiState.value = DashboardState.Loading
            }
            val cachedData =
                try {
                    cachedDashboardLoader()
                } catch (_: Exception) {
                    null
                }
            val cachedTasks =
                try {
                    cachedTasksLoader()
                } catch (_: Exception) {
                    emptyList()
                }
            val showingCached = cachedData != null || cachedTasks.isNotEmpty()
            if (softRefresh && previousState is DashboardState.Success) {
                _uiState.value = previousState.copy(isStale = true)
            } else {
                _uiState.value =
                    DashboardState.Success(
                        cachedData ?: DashboardData(0, 0, 0),
                        cachedTasks,
                        isStale = showingCached,
                    )
            }

            runCatching {
                coroutineScope {
                    val dashboard = async { dashboardLoader() }
                    val tasks = async { cachedTasksLoader() }
                    dashboard.await() to tasks.await()
                }
            }.onSuccess { (data, tasks) ->
                _uiState.value = DashboardState.Success(data, tasks)
            }.onFailure { error ->
                val apiError = error.toApiError()
                if (apiError is ApiError.SessionExpired) clearSession()
                val keepingVisibleData =
                    cachedData != null || (softRefresh && previousState is DashboardState.Success)
                if (!keepingVisibleData) {
                    _uiState.value = DashboardState.Error(apiError.toUserMessage())
                }
            }
        }
    }
}
