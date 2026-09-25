package com.example.nexus

import android.app.Application
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

class NexusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        AuthSession.initialize(TokenManager(this))
        SettingsSession.initialize(SettingsPreferences(this))

        val database = NexusDatabase.getDatabase(this)
        val apiService = RetrofitClient.instance
        val cacheOwner = { AuthSession.cacheOwnerId() }

        projectRepository = ProjectRepository(database, apiService, cacheOwner)
        taskRepository = TaskRepository(database, apiService, cacheOwner)
        habitRepository = HabitRepository(database, apiService, cacheOwner)
        dashboardRepository = DashboardRepository(database, apiService, cacheOwner)
        userRepository = UserRepository(apiService)
        configRepository = ConfigRepository(apiService)

        // Cached rows are stamped with the signed-in account. Whenever the session ends or a
        // different account signs in, drop the cache so the next account can never read it.
        AuthSession.setOnSessionCleared {
            applicationScope.launch { database.clearAllTables() }
        }
    }

    companion object {
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
