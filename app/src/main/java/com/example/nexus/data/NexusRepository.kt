package com.example.nexus.data

import com.example.nexus.api.ApiService
import com.example.nexus.api.CreateProjectRequest
import com.example.nexus.api.CreateTaskRequest
import com.example.nexus.api.DashboardData
import com.example.nexus.api.Project
import com.example.nexus.api.Task
import com.example.nexus.api.UpdateProfileRequest
import com.example.nexus.api.UpdateProjectRequest
import com.example.nexus.api.UpdateTaskRequest
import com.example.nexus.api.UserProfile
import com.example.nexus.api.CreateHabitRequest
import com.example.nexus.api.Habit
import com.example.nexus.api.UpdateHabitRequest
import com.example.nexus.api.AppConfig
import com.example.nexus.api.LocalizationBundle

/**
 * Single point of truth for network and cache access.
 *
 * Cached rows are scoped to [cacheOwner], so two accounts on the same device can never read each
 * other's data. Read operations fall back to the local cache when the network is unavailable.
 * Write operations deliberately do not invent local state: they surface the failure instead, since
 * there is not yet an offline write queue that could sync those changes later.
 */
class NexusRepository(
    private val database: NexusDatabase,
    private val apiService: ApiService,
    private val cacheOwner: () -> String?
) {
    /** Errors propagate so RemoteConfigViewModel can surface them instead of a silent default. */
    suspend fun getAppConfig(): AppConfig = apiService.getAppConfig()

    suspend fun getLocalization(language: String): LocalizationBundle =
        runCatching { apiService.getLocalization(language) }.getOrDefault(LocalizationBundle(language))

    /** The current cache owner, or null when nobody is signed in (nothing may be cached). */
    private fun owner(): String? = cacheOwner()?.takeIf { it.isNotBlank() }

    private fun HabitEntity.toModel() = Habit(id, name, description, frequency, targetDays, completedDates, createdAt)
    private fun Habit.toEntity(userId: String) = HabitEntity(id, userId, name, description, frequency, targetDays, completedDates, createdAt)
    private fun ProjectEntity.toModel() = Project(id, name, description, createdAt, updatedAt)
    private fun Project.toEntity(userId: String) = ProjectEntity(id, userId, name, description, createdAt, updatedAt)

    private fun TaskEntity.toModel() = Task(id, projectId, title, description, isCompleted, dueDate, priority, status, labels, checklist, createdAt, updatedAt)
    private fun Task.toEntity(userId: String) = TaskEntity(id, userId, projectId, title, description, isCompleted, dueDate, priority, status, labels, checklist, createdAt, updatedAt)

    private fun DashboardEntity.toModel() = DashboardData(projects, tasks, activity)

    suspend fun getCachedDashboard(): DashboardData? =
        owner()?.let { userId -> database.dashboardDao().getDashboard(userId)?.toModel() }

    suspend fun getProfile(): UserProfile = apiService.getProfile()

    suspend fun updateProfile(request: UpdateProfileRequest): UserProfile = apiService.updateProfile(request)

    suspend fun changePassword(request: com.example.nexus.api.ChangePasswordRequest) = apiService.changePassword(request)

    suspend fun deleteAccount() = apiService.deleteAccount()

    suspend fun getHabits(): List<Habit> {
        val userId = owner() ?: return apiService.getHabits()
        return try {
            apiService.getHabits().also { habits ->
                database.habitDao().insertHabits(habits.map { it.toEntity(userId) })
            }
        } catch (_: Exception) {
            database.habitDao().getHabits(userId).map { it.toModel() }
        }
    }

    suspend fun createHabit(request: CreateHabitRequest): Habit {
        val userId = owner() ?: return apiService.createHabit(request)
        val habit = apiService.createHabit(request)
        database.habitDao().insertHabit(habit.toEntity(userId))
        return habit
    }

    suspend fun updateHabit(habit: Habit): Habit {
        val request = UpdateHabitRequest(habit.name, habit.description, habit.frequency, habit.targetDays, habit.completedDates)
        val userId = owner() ?: return apiService.updateHabit(habit.id, request)
        val updated = apiService.updateHabit(habit.id, request)
        database.habitDao().insertHabit(updated.toEntity(userId))
        return updated
    }

    suspend fun deleteHabit(habitId: String) {
        val userId = owner()
        apiService.deleteHabit(habitId)
        if (userId != null) database.habitDao().deleteHabit(userId, habitId)
    }

    suspend fun getDashboard(): DashboardData {
        val userId = owner() ?: return apiService.getDashboard()
        return try {
            val networkData = apiService.getDashboard()
            database.dashboardDao().insertDashboard(
                DashboardEntity(
                    userId = userId,
                    projects = networkData.projects,
                    tasks = networkData.tasks,
                    activity = networkData.activity
                )
            )
            networkData
        } catch (e: Exception) {
            // Never substitute zeroed counts for real data: either serve the real cache or fail.
            database.dashboardDao().getDashboard(userId)?.toModel() ?: throw e
        }
    }

    suspend fun getProjects(): List<Project> {
        val userId = owner() ?: return apiService.getProjects()
        return try {
            val networkProjects = apiService.getProjects()
            database.projectDao().insertProjects(networkProjects.map { it.toEntity(userId) })
            networkProjects
        } catch (e: Exception) {
            database.projectDao().getProjects(userId).map { it.toModel() }
        }
    }

    suspend fun getProject(projectId: String): Project {
        val userId = owner() ?: return apiService.getProject(projectId)
        return try {
            val networkProject = apiService.getProject(projectId)
            database.projectDao().insertProject(networkProject.toEntity(userId))
            networkProject
        } catch (e: Exception) {
            database.projectDao().getProjectById(userId, projectId)?.toModel() ?: throw e
        }
    }

    suspend fun createProject(request: CreateProjectRequest): Project {
        val userId = owner() ?: return apiService.createProject(request)
        val project = apiService.createProject(request)
        database.projectDao().insertProject(project.toEntity(userId))
        return project
    }

    suspend fun updateProject(projectId: String, request: UpdateProjectRequest): Project {
        val userId = owner() ?: return apiService.updateProject(projectId, request)
        val project = apiService.updateProject(projectId, request)
        database.projectDao().insertProject(project.toEntity(userId))
        return project
    }

    suspend fun deleteProject(projectId: String) {
        val userId = owner()
        apiService.deleteProject(projectId)
        if (userId != null) {
            database.projectDao().deleteProjectById(userId, projectId)
            database.taskDao().deleteTasksByProjectId(userId, projectId)
        }
    }

    suspend fun getTasks(projectId: String): List<Task> {
        val userId = owner() ?: return apiService.getTasks(projectId)
        return try {
            val networkTasks = apiService.getTasks(projectId)
            database.taskDao().insertTasks(networkTasks.map { it.toEntity(userId) })
            networkTasks
        } catch (e: Exception) {
            database.taskDao().getTasksByProjectId(userId, projectId).map { it.toModel() }
        }
    }

    suspend fun getCachedTasks(): List<Task> =
        owner()?.let { userId -> database.taskDao().getAllTasks(userId).map { it.toModel() } } ?: emptyList()

    suspend fun createTask(projectId: String, request: CreateTaskRequest): Task {
        val userId = owner() ?: return apiService.createTask(projectId, request)
        val task = apiService.createTask(projectId, request)
        database.taskDao().insertTask(task.toEntity(userId))
        return task
    }

    suspend fun updateTask(taskId: String, request: UpdateTaskRequest): Task {
        val userId = owner() ?: return apiService.updateTask(taskId, request)
        val task = apiService.updateTask(taskId, request)
        database.taskDao().insertTask(task.toEntity(userId))
        return task
    }

    suspend fun deleteTask(taskId: String) {
        val userId = owner()
        apiService.deleteTask(taskId)
        if (userId != null) database.taskDao().deleteTaskById(userId, taskId)
    }
}
