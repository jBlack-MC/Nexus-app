package com.example.nexus.ui.projects

import android.os.Looper
import com.example.nexus.api.Project
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
class ProjectListViewModelTest {
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

    @Test fun successfulLoad_exposesProjects() = runTest(scheduler) {
        val project = Project(id = "project-1", name = "PoE")
        val viewModel = ProjectListViewModel(projectsLoader = { listOf(project) })

        advanceUntilIdle()

        assertEquals(listOf(project), viewModel.uiState.value.projects)
        assertTrue(!viewModel.uiState.value.isLoading)
    }

    @Test fun emptyLoad_exposesEmptyProjectList() = runTest(scheduler) {
        val viewModel = ProjectListViewModel(projectsLoader = { emptyList() })

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.projects.isEmpty())
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test fun failedLoad_exposesFriendlyError() = runTest(scheduler) {
        val viewModel = ProjectListViewModel(projectsLoader = { throw IOException("offline") })

        advanceUntilIdle()

        assertEquals(
            "No internet connection. Check your network and try again.",
            viewModel.uiState.value.errorMessage
        )
    }
}
