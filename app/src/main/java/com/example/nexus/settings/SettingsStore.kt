package com.example.nexus.settings

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

interface SettingsStore {
    fun getThemeMode(): ThemeMode
    fun setThemeMode(mode: ThemeMode)
    fun getNotificationsEnabled(): Boolean
    fun setNotificationsEnabled(enabled: Boolean)
    fun getLanguage(): String
    fun setLanguage(language: String)
}
