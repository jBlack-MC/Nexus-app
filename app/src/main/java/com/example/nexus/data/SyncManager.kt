package com.example.nexus.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.withTransaction
import com.example.nexus.api.ApiService
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

@Entity(tableName = "pending_changes")
data class PendingChange(
    @PrimaryKey(autoGenerate = true) val sequence: Long = 0,
    val userId: String,
    val operationId: String,
    val resource: String,
    val entityId: String,
    val action: String,
    val payload: String,
)

@Dao
interface PendingChangeDao {
    @Insert suspend fun insert(change: PendingChange)

    @Query("SELECT * FROM pending_changes WHERE userId = :userId ORDER BY sequence")
    suspend fun getPending(userId: String): List<PendingChange>

    @Query("DELETE FROM pending_changes WHERE sequence = :sequence")
    suspend fun delete(sequence: Long)

    @Query("DELETE FROM pending_changes WHERE userId = :userId AND resource = :resource AND entityId = :entityId")
    suspend fun deleteForEntity(userId: String, resource: String, entityId: String)

    @Query("DELETE FROM pending_changes WHERE userId = :userId")
    suspend fun clearForUser(userId: String)
}

data class SyncRequest(val operationId: String, val resource: String, val entityId: String, val action: String, val payload: JsonObject)
data class SyncStatus(val pending: Int = 0, val syncing: Boolean = false, val message: String = "Changes saved on this device", val conflict: Boolean = false)

/** Local writes and their outbox records commit together. A failed upload never loses a save. */
class SyncManager(private val database: NexusDatabase, private val api: ApiService, private val owner: () -> String?) {
    val mutex = Mutex()
    private val gson = Gson()
    private val mutableStatus = MutableStateFlow(SyncStatus())
    val status = mutableStatus.asStateFlow()
    private var failedChange: PendingChange? = null

    suspend fun discardConflict() = mutex.withLock {
        val change = failedChange ?: return@withLock
        if (owner() != change.userId) return@withLock
        database.withTransaction {
            database.pendingChangeDao().deleteForEntity(change.userId, change.resource, change.entityId)
            when (change.resource) {
                "projects" -> {
                    database.projectDao().deleteProjectById(change.userId, change.entityId)
                    database.taskDao().deleteTasksByProjectId(change.userId, change.entityId)
                }
                "tasks" -> database.taskDao().deleteTaskById(change.userId, change.entityId)
                "habits" -> database.habitDao().deleteHabit(change.userId, change.entityId)
            }
        }
        failedChange = null
        flushLocked()
    }

    suspend fun save(resource: String, id: String, action: String, model: Any, write: suspend (String) -> Unit) {
        mutex.withLock {
            val userId = requireNotNull(owner()) { "Sign in before saving changes" }
            database.withTransaction {
                write(userId)
                database.pendingChangeDao().insert(
                    PendingChange(
                        userId = userId, operationId = java.util.UUID.randomUUID().toString(),
                        resource = resource, entityId = id, action = action, payload = gson.toJson(model),
                    )
                )
            }
            mutableStatus.value = SyncStatus(database.pendingChangeDao().getPending(userId).size, message = "Saved on device · waiting to sync")
        }
    }

    suspend fun sync() = mutex.withLock { flushLocked() }

    /** Caller holds mutex, preventing refresh from overwriting a simultaneous local edit. */
    suspend fun flushLocked(): Boolean {
        val userId = owner() ?: run {
            mutableStatus.value = SyncStatus()
            return false
        }
        val pending = database.pendingChangeDao().getPending(userId)
        failedChange = null
        mutableStatus.value = SyncStatus(pending.size, true, "Syncing changes…")
        try {
            for ((index, change) in pending.withIndex()) {
                if (owner() != userId) return false
                failedChange = change
                api.sync(SyncRequest(change.operationId, change.resource, change.entityId, change.action, gson.fromJson(change.payload, JsonObject::class.java)))
                database.pendingChangeDao().delete(change.sequence)
                mutableStatus.value = SyncStatus(pending.size - index - 1, true, "Syncing changes…")
            }
            mutableStatus.value = SyncStatus(message = "All changes synced")
            failedChange = null
            return true
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val message = when {
                error is IOException -> "Offline · changes saved on device"
                error is HttpException && error.code() == 401 -> "Sign in again to sync saved changes"
                error is HttpException && error.code() == 409 -> "An item changed on another device · review required"
                else -> "Sync needs attention · saved changes kept on device"
            }
            mutableStatus.value = SyncStatus(
                database.pendingChangeDao().getPending(userId).size, message = message,
                conflict = error is HttpException && error.code() in listOf(400, 409)
            )
            return false
        }
    }

    suspend fun <T> read(remote: suspend () -> T, local: suspend () -> T): T = mutex.withLock {
        if (!flushLocked()) return@withLock local()
        try {
            remote()
        } catch (error: IOException) {
            mutableStatus.value = mutableStatus.value.copy(syncing = false, message = "Offline · showing saved data")
            local()
        }
    }
}
