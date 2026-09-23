package com.example.nexus

import android.app.Application
import com.example.nexus.api.RetrofitClient
import com.example.nexus.auth.AuthSession
import com.example.nexus.data.NexusDatabase
import com.example.nexus.data.NexusRepository
import com.example.nexus.settings.SettingsPreferences
import com.example.nexus.settings.SettingsSession
import com.example.nexus.util.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NexusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        AuthSession.initialize(TokenManager(this))
        SettingsSession.initialize(SettingsPreferences(this))

        val database = NexusDatabase.getDatabase(this)
        repository = NexusRepository(
            database = database,
            apiService = RetrofitClient.instance,
            cacheOwner = { AuthSession.cacheOwnerId() }
        )

        // Cached rows are stamped with the signed-in account. Whenever the session ends or a
        // different account signs in, drop the cache so the next account can never read it.
        AuthSession.setOnSessionCleared {
            applicationScope.launch { database.clearAllTables() }
        }
    }

    companion object {
        lateinit var instance: NexusApp
            private set
        lateinit var repository: NexusRepository
            private set

        /** Outlives any screen; used for fire-and-forget cache maintenance. */
        private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

