package com.example.nexus.data

import com.example.nexus.api.ApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SyncManagerTest {
    private val database = mockk<NexusDatabase>()
    private val dao = mockk<PendingChangeDao>(relaxed = true)
    private val api = mockk<ApiService>(relaxed = true)
    private val first = PendingChange(1, "account", "operation-1", "projects", "project", "create", "{\"name\":\"Plan\"}")
    private val second = first.copy(sequence = 2, operationId = "operation-2", action = "delete", payload = "{}")
    private val manager = SyncManager(database, api) { "account" }

    init {
        every { database.pendingChangeDao() } returns dao
    }

    @Test fun `acknowledged operations are removed in sequence`() = runTest {
        coEvery { dao.getPending("account") } returns listOf(first, second)
        manager.sync()
        coVerify(exactly = 1) { api.sync(match { it.operationId == "operation-1" }) }
        coVerify(exactly = 1) { api.sync(match { it.operationId == "operation-2" }) }
        coVerify(exactly = 1) { dao.delete(1) }
        coVerify(exactly = 1) { dao.delete(2) }
        assertEquals(0, manager.status.value.pending)
    }

    @Test fun `network failure keeps pending changes and stops dependent uploads`() = runTest {
        coEvery { dao.getPending("account") } returns listOf(first, second)
        coEvery { api.sync(any()) } throws IOException("offline")
        manager.sync()
        coVerify(exactly = 0) { dao.delete(any()) }
        coVerify(exactly = 1) { api.sync(any()) }
        assertEquals(2, manager.status.value.pending)
        assertFalse(manager.status.value.syncing)
    }

    @Test fun `pending writes prevent remote refresh overwriting local edits`() = runTest {
        coEvery { dao.getPending("account") } returns listOf(first)
        coEvery { api.sync(any()) } throws IOException()
        val result = manager.read(remote = { error("Remote must not run") }, local = { "local edit" })
        assertEquals("local edit", result)
    }

    @Test fun `offline reads use cache but authorization errors surface`() = runTest {
        coEvery { dao.getPending("account") } returns emptyList()
        assertEquals("cache", manager.read(remote = { throw IOException() }, local = { "cache" }))
        try {
            manager.read(remote = { throw IllegalStateException("invalid session") }, local = { "cache" })
            fail("Non-network errors must not be hidden")
        } catch (_: IllegalStateException) { }
    }
}
