package com.example.nexus.api

data class AppConfig(
    val minimumAppVersion: String = "1.0.0",
    val latestAppVersion: String = "1.0.0",
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "",
    val registrationEnabled: Boolean = true,
    val featureFlags: Map<String, Boolean> = emptyMap(),
    val announcements: List<Announcement> = emptyList(),
)

data class Announcement(
    val id: String,
    val title: String,
    val message: String,
    val date: String,
)

data class LocalizationBundle(
    val language: String,
    val strings: Map<String, String> = emptyMap(),
)
