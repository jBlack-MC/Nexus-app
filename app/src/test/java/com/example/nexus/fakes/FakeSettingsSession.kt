package com.example.nexus.fakes

import com.example.nexus.settings.SettingsSession
import com.example.nexus.settings.ThemeMode
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSettingsSession(
    initialThemeMode: ThemeMode = ThemeMode.SYSTEM,
    initialNotificationsEnabled: Boolean = true,
    initialLanguage: String = "en",
    initialLanguageSyncPending: Boolean = false,
) {
    private val _themeMode = MutableStateFlow(initialThemeMode)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(initialNotificationsEnabled)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _language = MutableStateFlow(initialLanguage)
    val language: StateFlow<String> = _language.asStateFlow()

    private val _languageSyncPending = MutableStateFlow(initialLanguageSyncPending)
    val languageSyncPending: StateFlow<Boolean> = _languageSyncPending.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun setLanguage(lang: String) {
        _language.value = lang
    }

    fun markLanguageSyncPending() {
        _languageSyncPending.value = true
    }

    fun clearLanguageSyncPending() {
        _languageSyncPending.value = false
    }

    fun reset(
        themeMode: ThemeMode = ThemeMode.SYSTEM,
        notificationsEnabled: Boolean = true,
        language: String = "en",
        languageSyncPending: Boolean = false,
    ) {
        _themeMode.value = themeMode
        _notificationsEnabled.value = notificationsEnabled
        _language.value = language
        _languageSyncPending.value = languageSyncPending
    }

    fun applyToMock() {
        mockkObject(SettingsSession)
        every { SettingsSession.themeMode } returns themeMode
        every { SettingsSession.notificationsEnabled } returns notificationsEnabled
        every { SettingsSession.language } returns language
        every { SettingsSession.languageSyncPending } returns languageSyncPending
        every { SettingsSession.setThemeMode(any()) } answers { setThemeMode(firstArg()) }
        every { SettingsSession.setNotificationsEnabled(any()) } answers { setNotificationsEnabled(firstArg()) }
        every { SettingsSession.setLanguage(any()) } answers { setLanguage(firstArg()) }
        every { SettingsSession.markLanguageSyncPending() } answers { markLanguageSyncPending() }
        every { SettingsSession.clearLanguageSyncPending() } answers { clearLanguageSyncPending() }
    }
}
