package com.example.nexus.ui.dashboard

import android.os.Looper
import com.example.nexus.api.DashboardData
import com.example.nexus.api.Task
import com.example.nexus.fakes.FakeDashboardRepository
import com.example.nexus.fakes.FakeTaskRepository
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
import java.io.IOException

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

    @Test fun successfulLoad_exposesDashboardAndCachedTasks() =
        runTest(scheduler) {
            val task = Task(id = "task-1", projectId = "project-1", title = "Write tests")
            val fakeDashboardRepo = FakeDashboardRepository(dashboardData = DashboardData(projects = 2, tasks = 3, activity = 4))
            val fakeTaskRepo = FakeTaskRepository(initialCachedTasks = listOf(task))

            val viewModel =
                DashboardViewModel(
                    dashboardRepository = fakeDashboardRepo,
                    taskRepository = fakeTaskRepo,
                )

            advanceUntilIdle()

            val state = viewModel.uiState.value as DashboardState.Success
            assertEquals(2, state.data.projects)
            assertEquals(listOf(task), state.cachedTasks)
        }

    @Test fun emptyLoad_exposesZeroCountsAndNoTasks() =
        runTest(scheduler) {
            val fakeDashboardRepo = FakeDashboardRepository(dashboardData = DashboardData(projects = 0, tasks = 0, activity = 0))
            val fakeTaskRepo = FakeTaskRepository(initialCachedTasks = emptyList())

            val viewModel =
                DashboardViewModel(
                    dashboardRepository = fakeDashboardRepo,
                    taskRepository = fakeTaskRepo,
                )

            advanceUntilIdle()

            val state = viewModel.uiState.value as DashboardState.Success
            assertEquals(0, state.data.projects)
            assertTrue(state.cachedTasks.isEmpty())
        }

    @Test fun failedLoad_exposesFriendlyError() =
        runTest(scheduler) {
            val fakeDashboardRepo = FakeDashboardRepository(dashboardError = IOException("offline"))
            val fakeTaskRepo = FakeTaskRepository()

            val viewModel =
                DashboardViewModel(
                    dashboardRepository = fakeDashboardRepo,
                    taskRepository = fakeTaskRepo,
                )

            advanceUntilIdle()

            assertEquals(
                "No internet connection. Check your network and try again.",
                (viewModel.uiState.value as DashboardState.Error).message,
            )
        }

    @Test fun cachedEmission_marksStateStaleUntilFreshFetchLands() =
        runTest(scheduler) {
            val gate = CompletableDeferred<DashboardData>()
            val fakeDashboardRepo =
                FakeDashboardRepository(
                    cachedDashboardData = DashboardData(projects = 1, tasks = 2, activity = 3),
                    dashboardAction = { gate.await() },
                )
            val fakeTaskRepo = FakeTaskRepository()

            val viewModel =
                DashboardViewModel(
                    dashboardRepository = fakeDashboardRepo,
                    taskRepository = fakeTaskRepo,
                )

            advanceUntilIdle()

            val cachedState = viewModel.uiState.value as DashboardState.Success
            assertTrue(cachedState.isStale)
            assertEquals(1, cachedState.data.projects)

            gate.complete(DashboardData(projects = 9, tasks = 8, activity = 7))
            advanceUntilIdle()

            val freshState = viewModel.uiState.value as DashboardState.Success
            assertFalse(freshState.isStale)
            assertEquals(9, freshState.data.projects)
        }

    @Test fun failedRefresh_keepsCachedDataAndMarksItStale() =
        runTest(scheduler) {
            val fakeDashboardRepo =
                FakeDashboardRepository(
                    dashboardError = IOException("offline"),
                    cachedDashboardData = DashboardData(projects = 4, tasks = 5, activity = 6),
                )
            val fakeTaskRepo = FakeTaskRepository()

            val viewModel =
                DashboardViewModel(
                    dashboardRepository = fakeDashboardRepo,
                    taskRepository = fakeTaskRepo,
                )

            advanceUntilIdle()

            val state = viewModel.uiState.value as DashboardState.Success
            assertTrue(state.isStale)
            assertEquals(4, state.data.projects)
        }

    @Test fun softRefresh_keepsPreviousPayloadWhileNetworkIsInFlight() =
        runTest(scheduler) {
            val gate = CompletableDeferred<DashboardData>()
            var loads = 0
            val fakeDashboardRepo =
                FakeDashboardRepository(
                    dashboardAction = {
                        loads++
                        if (loads == 1) DashboardData(projects = 1, tasks = 2, activity = 3) else gate.await()
                    },
                )
            val fakeTaskRepo = FakeTaskRepository()

            val viewModel =
                DashboardViewModel(
                    dashboardRepository = fakeDashboardRepo,
                    taskRepository = fakeTaskRepo,
                )

            advanceUntilIdle()
            assertEquals(1, (viewModel.uiState.value as DashboardState.Success).data.projects)

            viewModel.fetchDashboard(softRefresh = true)
            advanceUntilIdle()

            val inFlight = viewModel.uiState.value as DashboardState.Success
            assertEquals(1, inFlight.data.projects)
            assertTrue(inFlight.isStale)

            gate.complete(DashboardData(projects = 7, tasks = 8, activity = 9))
            advanceUntilIdle()

            val fresh = viewModel.uiState.value as DashboardState.Success
            assertEquals(7, fresh.data.projects)
            assertFalse(fresh.isStale)
        }

    @Test fun failedSoftRefresh_keepsPreviousPayloadAsStale() =
        runTest(scheduler) {
            var loads = 0
            val fakeDashboardRepo =
                FakeDashboardRepository(
                    dashboardAction = {
                        loads++
                        if (loads == 1) {
                            DashboardData(projects = 3, tasks = 4, activity = 5)
                        } else {
                            throw IOException("offline")
                        }
                    },
                )
            val fakeTaskRepo = FakeTaskRepository()

            val viewModel =
                DashboardViewModel(
                    dashboardRepository = fakeDashboardRepo,
                    taskRepository = fakeTaskRepo,
                )
            advanceUntilIdle()

            viewModel.fetchDashboard(softRefresh = true)
            advanceUntilIdle()

            val state = viewModel.uiState.value as DashboardState.Success
            assertTrue(state.isStale)
            assertEquals(3, state.data.projects)
        }
}
