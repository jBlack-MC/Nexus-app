package com.example.nexus

import android.app.Application
import com.example.nexus.api.RetrofitClient
import com.example.nexus.auth.AuthSession
import com.example.nexus.data.NexusDatabase
import com.example.nexus.data.NexusRepository
import com.example.nexus.settings.SettingsPreferences
import com.example.nexus.settings.SettingsSession
import com.example.nexus.util.TokenManager

class NexusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        AuthSession.initialize(TokenManager(this))
        SettingsSession.initialize(SettingsPreferences(this))

        val database = NexusDatabase.getDatabase(this)
        repository = NexusRepository(database, RetrofitClient.instance)
    }

    companion object {
        lateinit var instance: NexusApp
            private set
        lateinit var repository: NexusRepository
            private set
    }
}

