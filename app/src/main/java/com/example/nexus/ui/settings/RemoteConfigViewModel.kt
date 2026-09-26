package com.example.nexus.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.NexusApp
import com.example.nexus.api.AppConfig
import com.example.nexus.api.toApiError
import com.example.nexus.api.toUserMessage
import com.example.nexus.data.ConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RemoteConfigUiState(
    val config: AppConfig = AppConfig(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class RemoteConfigViewModel(
    private val configRepository: ConfigRepository? = null,
    private val configLoader: suspend () -> AppConfig = { (configRepository ?: NexusApp.configRepository).getAppConfig() },
) : ViewModel() {
    private val _uiState = MutableStateFlow(RemoteConfigUiState())
    val uiState: StateFlow<RemoteConfigUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { configLoader() }
                .onSuccess { config ->
                    _uiState.value = RemoteConfigUiState(config = config, isLoading = false)
                }
                .onFailure { error ->
                    val message = error.toApiError().toUserMessage()
                    Log.w(TAG, "Remote config fetch failed — serving fallback config: $message", error)
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            errorMessage = message,
                        )
                }
        }
    }

    companion object {
        private const val TAG = "RemoteConfig"
    }
}
