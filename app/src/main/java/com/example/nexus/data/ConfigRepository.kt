package com.example.nexus.data

import com.example.nexus.api.ApiService
import com.example.nexus.api.AppConfig
import com.example.nexus.api.LocalizationBundle

open class ConfigRepository(
    private val apiService: ApiService? = null
) {
    open suspend fun getAppConfig(): AppConfig = apiService?.getAppConfig() ?: AppConfig()

    open suspend fun getLocalization(language: String): LocalizationBundle =
        runCatching { apiService?.getLocalization(language) ?: LocalizationBundle(language) }.getOrDefault(LocalizationBundle(language))
}
