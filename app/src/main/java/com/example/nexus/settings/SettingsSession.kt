package com.example.nexus.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SettingsSession {
    private var store: SettingsStore? = null

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _language = MutableStateFlow("en")
    val language: StateFlow<String> = _language.asStateFlow()

    fun initialize(settingsStore: SettingsStore) {
        store = settingsStore
        _themeMode.value = settingsStore.getThemeMode()
        _notificationsEnabled.value = settingsStore.getNotificationsEnabled()
        _language.value = settingsStore.getLanguage()
    }

    private fun getStore(): SettingsStore {
        return store ?: throw IllegalStateException("SettingsSession.initialize() was not called")
    }

    fun setThemeMode(mode: ThemeMode) {
        getStore().setThemeMode(mode)
        _themeMode.value = mode
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        getStore().setNotificationsEnabled(enabled)
        _notificationsEnabled.value = enabled
    }

    fun setLanguage(language: String) {
        getStore().setLanguage(language)
        _language.value = language
    }
}
