package com.example.nexus.fakes

import com.example.nexus.api.AppConfig
import com.example.nexus.api.LocalizationBundle
import com.example.nexus.data.ConfigRepository

class FakeConfigRepository(
    var appConfig: AppConfig = AppConfig(),
    var appConfigError: Throwable? = null,
    var localizationBundle: LocalizationBundle = LocalizationBundle("en"),
    var localizationError: Throwable? = null,
) : ConfigRepository() {
    override suspend fun getAppConfig(): AppConfig {
        appConfigError?.let { throw it }
        return appConfig
    }

    override suspend fun getLocalization(language: String): LocalizationBundle {
        localizationError?.let { throw it }
        return localizationBundle
    }

    val configLoader: suspend () -> AppConfig = { getAppConfig() }
}
