package com.example.nexus.fakes

import com.example.nexus.api.DashboardData
import com.example.nexus.data.DashboardRepository

class FakeDashboardRepository(
    var dashboardData: DashboardData = DashboardData(0, 0, 0),
    var cachedDashboardData: DashboardData? = null,
    var dashboardError: Throwable? = null,
    var cachedDashboardError: Throwable? = null,
    var dashboardAction: (suspend () -> DashboardData)? = null,
) : DashboardRepository() {
    override suspend fun getDashboard(): DashboardData {
        dashboardAction?.let { return it() }
        dashboardError?.let { throw it }
        return dashboardData
    }

    override suspend fun getCachedDashboard(): DashboardData? {
        cachedDashboardError?.let { throw it }
        return cachedDashboardData
    }

    val dashboardLoader: suspend () -> DashboardData = { getDashboard() }
    val cachedDashboardLoader: suspend () -> DashboardData? = { getCachedDashboard() }
}
