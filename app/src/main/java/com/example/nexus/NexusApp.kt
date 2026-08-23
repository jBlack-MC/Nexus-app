package com.example.nexus

import android.app.Application
import com.example.nexus.auth.AuthSession
import com.example.nexus.auth.SecureTokenStore

class NexusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AuthSession.initialize(SecureTokenStore(this))
    }
}

