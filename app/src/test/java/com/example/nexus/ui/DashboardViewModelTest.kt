package com.example.nexus.ui

import android.os.Looper
import com.example.nexus.api.DashboardData
import com.example.nexus.api.Task
import java.io.IOException
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private lateinit var scheduler: TestCoroutineScheduler
    private lateinit var dispatcher: TestDispatcher

    @Before fun setUp() {
        scheduler = TestCoroutineScheduler()
        dispatcher = StandardTestDispatcher(scheduler)
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mockk(relaxed = true)
        Dispatchers.setMain(dispatcher)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test fun successfulLoad_exposesDashboardAndCachedTasks() = runTest(scheduler) {
        val task = Task(id = "task-1", projectId = "project-1", title = "Write tests")
        val viewModel = DashboardViewModel(
            dashboardLoader = { DashboardData(projects = 2, tasks = 3, activity = 4) },
            cachedTasksLoader = { listOf(task) }
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value as DashboardState.Success
        assertEquals(2, state.data.projects)
        assertEquals(listOf(task), state.cachedTasks)
    }

    @Test fun emptyLoad_exposesZeroCountsAndNoTasks() = runTest(scheduler) {
        val viewModel = DashboardViewModel(
            dashboardLoader = { DashboardData(projects = 0, tasks = 0, activity = 0) },
            cachedTasksLoader = { emptyList() }
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value as DashboardState.Success
        assertEquals(0, state.data.projects)
        assertTrue(state.cachedTasks.isEmpty())
    }

    @Test fun failedLoad_exposesFriendlyError() = runTest(scheduler) {
        val viewModel = DashboardViewModel(
            dashboardLoader = { throw IOException("offline") },
            cachedTasksLoader = { emptyList() }
        )

        advanceUntilIdle()

        assertEquals(
            "No internet connection. Check your network and try again.",
            (viewModel.uiState.value as DashboardState.Error).message
        )
    }

    @Test fun cachedEmission_marksStateStaleUntilFreshFetchLands() = runTest(scheduler) {
        val gate = CompletableDeferred<DashboardData>()
        val viewModel = DashboardViewModel(
            dashboardLoader = { gate.await() },
            cachedTasksLoader = { emptyList() },
            cachedDashboardLoader = { DashboardData(projects = 1, tasks = 2, activity = 3) }
        )

        advanceUntilIdle() // runs to the cached emission, then parks on the gate

        val cachedState = viewModel.uiState.value as DashboardState.Success
        assertTrue(cachedState.isStale)
        assertEquals(1, cachedState.data.projects)

        gate.complete(DashboardData(projects = 9, tasks = 8, activity = 7))
        advanceUntilIdle()

        val freshState = viewModel.uiState.value as DashboardState.Success
        assertFalse(freshState.isStale)
        assertEquals(9, freshState.data.projects)
    }

    @Test fun failedRefresh_keepsCachedDataAndMarksItStale() = runTest(scheduler) {
        val viewModel = DashboardViewModel(
            dashboardLoader = { throw IOException("offline") },
            cachedTasksLoader = { emptyList() },
            cachedDashboardLoader = { DashboardData(projects = 4, tasks = 5, activity = 6) }
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value as DashboardState.Success
        assertTrue(state.isStale)
        assertEquals(4, state.data.projects)
    }

    @Test fun softRefresh_keepsPreviousPayloadWhileNetworkIsInFlight() = runTest(scheduler) {
        val gate = CompletableDeferred<DashboardData>()
        var loads = 0
        val viewModel = DashboardViewModel(
            dashboardLoader = {
                loads++
                if (loads == 1) DashboardData(projects = 1, tasks = 2, activity = 3) else gate.await()
            },
            cachedTasksLoader = { emptyList() }
        )

        advanceUntilIdle()
        assertEquals(1, (viewModel.uiState.value as DashboardState.Success).data.projects)

        viewModel.fetchDashboard(softRefresh = true)
        advanceUntilIdle() // parks on the gate: the refresh is still in flight

        val inFlight = viewModel.uiState.value as DashboardState.Success
        assertEquals(1, inFlight.data.projects) // previous counts stay, never zeros or Loading
        assertTrue(inFlight.isStale)

        gate.complete(DashboardData(projects = 7, tasks = 8, activity = 9))
        advanceUntilIdle()

        val fresh = viewModel.uiState.value as DashboardState.Success
        assertEquals(7, fresh.data.projects)
        assertFalse(fresh.isStale)
    }

    @Test fun failedSoftRefresh_keepsPreviousPayloadAsStale() = runTest(scheduler) {
        var loads = 0
        val viewModel = DashboardViewModel(
            dashboardLoader = {
                loads++
                if (loads == 1) DashboardData(projects = 3, tasks = 4, activity = 5)
                else throw IOException("offline")
            },
            cachedTasksLoader = { emptyList() }
        )
        advanceUntilIdle()

        viewModel.fetchDashboard(softRefresh = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value as DashboardState.Success
        assertTrue(state.isStale)
        assertEquals(3, state.data.projects)
    }
}
