package com.example.nexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.NexusApp
import com.example.nexus.api.ApiError
import com.example.nexus.api.DashboardData
import com.example.nexus.api.Task
import com.example.nexus.api.toApiError
import com.example.nexus.api.toUserMessage
import com.example.nexus.auth.AuthSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

sealed class DashboardState {
    data object Loading : DashboardState()
    data class Success(
        val data: DashboardData,
        val cachedTasks: List<Task>,
        /**
         * True while this payload comes from the local cache instead of a fresh fetch:
         * either the stale-while-revalidate window before the network lands, or a failed
         * refresh keeping last-known data. Dashboard shows a "saved data" banner while set;
         * it clears automatically on the next successful fetch.
         */
        val isStale: Boolean = false
    ) : DashboardState()
    data class Error(val message: String) : DashboardState()
}

class DashboardViewModel(
    private val dashboardLoader: suspend () -> DashboardData = { NexusApp.repository.getDashboard() },
    private val cachedTasksLoader: suspend () -> List<Task> = { NexusApp.repository.getCachedTasks() },
    private val cachedDashboardLoader: suspend () -> DashboardData? = { NexusApp.repository.getCachedDashboard() },
    private val clearSession: () -> Unit = { AuthSession.clearToken() }
) : ViewModel() {
    private val _uiState = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val uiState: StateFlow<DashboardState> = _uiState

    init {
        fetchDashboard()
    }

    /**
     * Loads the dashboard counts (plus the cached tasks behind the schedule cards).
     *
     * @param softRefresh keeps the current [DashboardState.Success] on screen while the network
     *   call runs — used when the screen is re-entered, so coming back from Projects/Tasks
     *   refreshes silently instead of flashing the loading skeleton. A failed soft refresh keeps
     *   the last-known data with [DashboardState.Success.isStale] set.
     */
    fun fetchDashboard(softRefresh: Boolean = false) {
        viewModelScope.launch {
            // Captured before the in-flight placeholder so a soft refresh can keep showing the
            // payload the user is already looking at.
            val previousState = _uiState.value
            if (!softRefresh) {
                _uiState.value = DashboardState.Loading
            }
            // The cache is best-effort: a failing cache read must never wedge the screen on
            // Loading. Show what we have (or zeros) while the network load is in flight.
            val cachedData = try {
                cachedDashboardLoader()
            } catch (_: Exception) {
                null
            }
            val cachedTasks = try {
                cachedTasksLoader()
            } catch (_: Exception) {
                emptyList()
            }
            val showingCached = cachedData != null || cachedTasks.isNotEmpty()
            if (softRefresh && previousState is DashboardState.Success) {
                // A re-entry refresh must not blank the counts to zero while the network call is in
                // flight: keep the visible payload and let the stale banner mark it as refreshing.
                _uiState.value = previousState.copy(isStale = true)
            } else {
                _uiState.value = DashboardState.Success(
                    cachedData ?: DashboardData(0, 0, 0),
                    cachedTasks,
                    isStale = showingCached
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
                // With cache present — or with a payload already on screen during a soft refresh —
                // the last-known Success (isStale = true) is intentionally kept, so the banner
                // signals saved data instead of the screen vanishing on a failed refresh.
                val keepingVisibleData =
                    cachedData != null || (softRefresh && previousState is DashboardState.Success)
                if (!keepingVisibleData) {
                    _uiState.value = DashboardState.Error(apiError.toUserMessage())
                }
            }
        }
    }
}
