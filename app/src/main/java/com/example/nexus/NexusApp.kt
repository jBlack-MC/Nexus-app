package com.example.nexus

import android.app.Application
import androidx.room.withTransaction
import com.example.nexus.api.RetrofitClient
import com.example.nexus.auth.AuthSession
import com.example.nexus.data.ConfigRepository
import com.example.nexus.data.DashboardRepository
import com.example.nexus.data.HabitRepository
import com.example.nexus.data.NexusDatabase
import com.example.nexus.data.ProjectRepository
import com.example.nexus.data.TaskRepository
import com.example.nexus.data.UserRepository
import com.example.nexus.settings.SettingsPreferences
import com.example.nexus.settings.SettingsSession
import com.example.nexus.util.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

class NexusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        AuthSession.initialize(TokenManager(this))
        SettingsSession.initialize(SettingsPreferences(this))

        val database = NexusDatabase.getDatabase(this)
        val apiService = RetrofitClient.instance
        val cacheOwner = { AuthSession.cacheOwnerId() }

        syncManager = com.example.nexus.data.SyncManager(database, apiService, cacheOwner)
        projectRepository = ProjectRepository(database, apiService, cacheOwner, syncManager)
        taskRepository = TaskRepository(database, apiService, cacheOwner, syncManager)
        habitRepository = HabitRepository(database, apiService, cacheOwner, syncManager)
        dashboardRepository = DashboardRepository(database, apiService, cacheOwner, syncManager)
        userRepository = UserRepository(apiService, getSharedPreferences("cached_profiles", MODE_PRIVATE), cacheOwner) { userId ->
            syncManager.mutex.withLock {
                database.withTransaction {
                    database.pendingChangeDao().clearForUser(userId)
                    database.projectDao().clearForUser(userId)
                    database.taskDao().clearForUser(userId)
                    database.habitDao().clearForUser(userId)
                    database.dashboardDao().clearForUser(userId)
                }
            }
        }
        configRepository = ConfigRepository(apiService)

        val connectivity = getSystemService(android.net.ConnectivityManager::class.java)
        connectivity.registerNetworkCallback(
            android.net.NetworkRequest.Builder().addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
            object : android.net.ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    applicationScope.launch { syncManager.sync() }
                }
            },
        )
        // Account-scoped rows and unsent changes survive sign-out; only their owner can read them.
        applicationScope.launch {
            while (true) {
                syncManager.sync()
                kotlinx.coroutines.delay(30_000)
            }
        }
    }

    companion object {
        lateinit var syncManager: com.example.nexus.data.SyncManager
            private set
        lateinit var instance: NexusApp
            private set
        lateinit var projectRepository: ProjectRepository
            private set
        lateinit var taskRepository: TaskRepository
            private set
        lateinit var habitRepository: HabitRepository
            private set
        lateinit var dashboardRepository: DashboardRepository
            private set
        lateinit var userRepository: UserRepository
            private set
        lateinit var configRepository: ConfigRepository
            private set

        /** Outlives any screen; used for fire-and-forget cache maintenance. */
        private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
