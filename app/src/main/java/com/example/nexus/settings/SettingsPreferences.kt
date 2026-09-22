package com.example.nexus.settings

import android.content.Context

class SettingsPreferences(context: Context) : SettingsStore {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getThemeMode(): ThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return runCatching { ThemeMode.valueOf(name ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    override fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    override fun getNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATIONS, true)

    override fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    override fun getLanguage(): String = prefs.getString(KEY_LANGUAGE, "en") ?: "en"

    override fun setLanguage(language: String) {
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
    }

    companion object {
        private const val PREFS_NAME = "nexus_settings_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_LANGUAGE = "language"
    }
}
