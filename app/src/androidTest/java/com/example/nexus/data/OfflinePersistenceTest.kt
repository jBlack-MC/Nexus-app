package com.example.nexus.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nexus.api.ApiService
import com.example.nexus.api.CreateProjectRequest
import java.io.IOException
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflinePersistenceTest {
    @Test fun offlineSaveSurvivesDatabaseReopenAndSyncsOnlyItsOwner() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "offline-test-${java.util.UUID.randomUUID()}.db"
        var online = false
        var uploads = 0
        val api = Proxy.newProxyInstance(ApiService::class.java.classLoader, arrayOf(ApiService::class.java)) { _, method, _ ->
            if (method.name == "sync") {
                if (!online) throw IOException("offline")
                uploads++
                Unit
            } else {
                throw IOException("offline")
            }
        } as ApiService
        var database = Room.databaseBuilder(context, NexusDatabase::class.java, name).build()
        try {
            var owner = "alice"
            var sync = SyncManager(database, api) { owner }
            val repository = ProjectRepository(database, api, { owner }, sync)
            val project = repository.createProject(CreateProjectRequest("Saved offline"))
            assertEquals(project.id, repository.getProjects().single().id)
            assertEquals(1, database.pendingChangeDao().getPending("alice").size)
            database.close()
            database = Room.databaseBuilder(context, NexusDatabase::class.java, name).build()
            sync = SyncManager(database, api) { owner }
            assertEquals("Saved offline", database.projectDao().getProjects("alice").single().name)
            owner = "bob"
            online = true
            sync.sync()
            assertEquals(0, uploads)
            assertTrue(database.projectDao().getProjects("bob").isEmpty())
            owner = "alice"
            sync.sync()
            assertEquals(1, uploads)
            assertTrue(database.pendingChangeDao().getPending("alice").isEmpty())
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
