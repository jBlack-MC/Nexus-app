package com.example.nexus.data

import androidx.room.withTransaction
import com.example.nexus.api.ApiService
import com.example.nexus.api.DashboardData

open class DashboardRepository(
    private val database: NexusDatabase? = null,
    private val apiService: ApiService? = null,
    private val cacheOwner: () -> String? = { null },
    private val sync: SyncManager? = null,
) {
    private fun owner(): String? = cacheOwner()?.takeIf { it.isNotBlank() }

    private fun DashboardEntity.toModel() = DashboardData(projects, tasks, activity)

    open suspend fun getCachedDashboard(): DashboardData? =
        owner()?.let { userId -> database?.dashboardDao()?.getDashboard(userId)?.toModel() }

    open suspend fun getDashboard(): DashboardData {
        val db = database ?: return apiService?.getDashboard() ?: DashboardData(0, 0, 0)
        val api = apiService ?: return db.dashboardDao().getDashboard(owner() ?: "")?.toModel() ?: DashboardData(0, 0, 0)
        val userId = owner() ?: return api.getDashboard()
        if (sync != null) {
            return sync.read(
                remote = {
                    // Hydrate every project so Today and offline task lists do not depend on which
                    // detail screens were previously opened.
                    val projects = api.getProjects()
                    val tasks = projects.flatMap { api.getTasks(it.id) }
                    val data = DashboardData(projects.size, tasks.size, tasks.count { it.isCompleted })
                    db.withTransaction {
                        db.projectDao().clearForUser(userId)
                        db.taskDao().clearForUser(userId)
                        db.projectDao().insertProjects(projects.map { ProjectEntity(it.id, userId, it.name, it.description, it.createdAt, it.updatedAt) })
                        db.taskDao().insertTasks(
                            tasks.map {
                                TaskEntity(it.id, userId, it.projectId, it.title, it.description, it.isCompleted, it.dueDate, it.priority, it.status, it.labels, it.checklist, it.createdAt, it.updatedAt)
                            }
                        )
                        db.dashboardDao().insertDashboard(DashboardEntity(userId, data.projects, data.tasks, data.activity))
                    }
                    data
                },
                local = {
                    val tasks = db.taskDao().getAllTasks(userId)
                    DashboardData(db.projectDao().getProjects(userId).size, tasks.size, tasks.count { it.isCompleted })
                },
            )
        }
        return try {
            val networkData = api.getDashboard()
            db.dashboardDao().insertDashboard(
                DashboardEntity(
                    userId = userId,
                    projects = networkData.projects,
                    tasks = networkData.tasks,
                    activity = networkData.activity,
                ),
            )
            networkData
        } catch (e: java.io.IOException) {
            db.dashboardDao().getDashboard(userId)?.toModel() ?: throw e
        }
    }
}
