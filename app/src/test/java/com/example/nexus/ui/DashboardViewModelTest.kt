package com.example.nexus.ui

import android.os.Looper
import com.example.nexus.api.DashboardData
import com.example.nexus.api.Task
import java.io.IOException
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
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
}
