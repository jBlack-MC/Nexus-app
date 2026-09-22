package com.example.nexus

import android.app.Application
import com.example.nexus.api.RetrofitClient
import com.example.nexus.auth.AuthSession
import com.example.nexus.data.NexusDatabase
import com.example.nexus.data.NexusRepository
import com.example.nexus.util.TokenManager

class NexusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AuthSession.initialize(TokenManager(this))

        val database = NexusDatabase.getDatabase(this)
        repository = NexusRepository(database, RetrofitClient.instance)
    }

    companion object {
        lateinit var repository: NexusRepository
            private set
    }
}

