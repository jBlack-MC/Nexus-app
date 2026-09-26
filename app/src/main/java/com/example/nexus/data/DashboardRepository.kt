package com.example.nexus.data

import com.example.nexus.api.ApiService
import com.example.nexus.api.DashboardData

open class DashboardRepository(
    private val database: NexusDatabase? = null,
    private val apiService: ApiService? = null,
    private val cacheOwner: () -> String? = { null },
) {
    private fun owner(): String? = cacheOwner()?.takeIf { it.isNotBlank() }

    private fun DashboardEntity.toModel() = DashboardData(projects, tasks, activity)

    open suspend fun getCachedDashboard(): DashboardData? =
        owner()?.let { userId -> database?.dashboardDao()?.getDashboard(userId)?.toModel() }

    open suspend fun getDashboard(): DashboardData {
        val db = database ?: return apiService?.getDashboard() ?: DashboardData(0, 0, 0)
        val api = apiService ?: return db.dashboardDao().getDashboard(owner() ?: "")?.toModel() ?: DashboardData(0, 0, 0)
        val userId = owner() ?: return api.getDashboard()
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
        } catch (e: Exception) {
            db.dashboardDao().getDashboard(userId)?.toModel() ?: throw e
        }
    }
}
