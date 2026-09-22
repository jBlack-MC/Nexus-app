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

class NexusRepository(
    private val database: NexusDatabase,
    private val apiService: ApiService
) {
    suspend fun getAppConfig(): AppConfig = runCatching { apiService.getAppConfig() }.getOrDefault(AppConfig())

    suspend fun getLocalization(language: String): LocalizationBundle =
        runCatching { apiService.getLocalization(language) }.getOrDefault(LocalizationBundle(language))

    private fun HabitEntity.toModel() = Habit(id, name, description, frequency, targetDays, completedDates, createdAt)
    private fun Habit.toEntity() = HabitEntity(id, name, description, frequency, targetDays, completedDates, createdAt)
    private fun ProjectEntity.toModel() = Project(id, name, description, createdAt, updatedAt)
    private fun Project.toEntity() = ProjectEntity(id, name, description, createdAt, updatedAt)

    private fun TaskEntity.toModel() = Task(id, projectId, title, description, isCompleted, dueDate, priority, status, labels, checklist, createdAt, updatedAt)
    private fun Task.toEntity() = TaskEntity(id, projectId, title, description, isCompleted, dueDate, priority, status, labels, checklist, createdAt, updatedAt)

    private fun DashboardEntity.toModel() = DashboardData(projects, tasks, activity)

    suspend fun getCachedDashboard(): DashboardData? = database.dashboardDao().getDashboard()?.toModel()

    suspend fun getProfile(): UserProfile = apiService.getProfile()

    suspend fun updateProfile(request: UpdateProfileRequest): UserProfile = apiService.updateProfile(request)

    suspend fun changePassword(request: com.example.nexus.api.ChangePasswordRequest) = apiService.changePassword(request)

    suspend fun deleteAccount() = apiService.deleteAccount()

    suspend fun getHabits(): List<Habit> = try {
        apiService.getHabits().also { habits -> database.habitDao().insertHabits(habits.map { it.toEntity() }) }
    } catch (_: Exception) {
        database.habitDao().getHabits().map { it.toModel() }
    }

    suspend fun createHabit(request: CreateHabitRequest): Habit {
        return try {
            apiService.createHabit(request).also { database.habitDao().insertHabit(it.toEntity()) }
        } catch (_: Exception) {
            val habit = Habit(java.util.UUID.randomUUID().toString(), request.name, request.description, request.frequency, request.targetDays)
            database.habitDao().insertHabit(habit.toEntity())
            habit
        }
    }

    suspend fun updateHabit(habit: Habit): Habit {
        return try {
            apiService.updateHabit(habit.id, UpdateHabitRequest(habit.name, habit.description, habit.frequency, habit.targetDays, habit.completedDates))
                .also { database.habitDao().insertHabit(it.toEntity()) }
        } catch (_: Exception) {
            database.habitDao().insertHabit(habit.toEntity())
            habit
        }
    }

    suspend fun deleteHabit(habitId: String) {
        runCatching { apiService.deleteHabit(habitId) }
        database.habitDao().deleteHabit(habitId)
    }

    suspend fun getDashboard(): DashboardData {
        return try {
            val networkData = apiService.getDashboard()
            database.dashboardDao().insertDashboard(
                DashboardEntity(projects = networkData.projects, tasks = networkData.tasks, activity = networkData.activity)
            )
            networkData
        } catch (e: Exception) {
            val cached = database.dashboardDao().getDashboard()
            cached?.toModel() ?: DashboardData(0, 0, 0)
        }
    }

    suspend fun getProjects(): List<Project> {
        return try {
            val networkProjects = apiService.getProjects()
            database.projectDao().insertProjects(networkProjects.map { it.toEntity() })
            networkProjects
        } catch (e: Exception) {
            database.projectDao().getProjects().map { it.toModel() }
        }
    }

    suspend fun getProject(projectId: String): Project {
        return try {
            val networkProject = apiService.getProject(projectId)
            database.projectDao().insertProject(networkProject.toEntity())
            networkProject
        } catch (e: Exception) {
            val cached = database.projectDao().getProjectById(projectId)
            cached?.toModel() ?: throw e
        }
    }

    suspend fun createProject(request: CreateProjectRequest): Project {
        val project = apiService.createProject(request)
        database.projectDao().insertProject(project.toEntity())
        return project
    }

    suspend fun updateProject(projectId: String, request: UpdateProjectRequest): Project {
        val project = apiService.updateProject(projectId, request)
        database.projectDao().insertProject(project.toEntity())
        return project
    }

    suspend fun deleteProject(projectId: String) {
        apiService.deleteProject(projectId)
        database.projectDao().deleteProjectById(projectId)
        database.taskDao().deleteTasksByProjectId(projectId)
    }

    suspend fun getTasks(projectId: String): List<Task> {
        return try {
            val networkTasks = apiService.getTasks(projectId)
            database.taskDao().insertTasks(networkTasks.map { it.toEntity() })
            networkTasks
        } catch (e: Exception) {
            database.taskDao().getTasksByProjectId(projectId).map { it.toModel() }
        }
    }

    suspend fun getCachedTasks(): List<Task> = database.taskDao().getAllTasks().map { it.toModel() }

    suspend fun createTask(projectId: String, request: CreateTaskRequest): Task {
        val task = apiService.createTask(projectId, request)
        database.taskDao().insertTask(task.toEntity())
        return task
    }

    suspend fun updateTask(taskId: String, request: UpdateTaskRequest): Task {
        val task = apiService.updateTask(taskId, request)
        database.taskDao().insertTask(task.toEntity())
        return task
    }

    suspend fun deleteTask(taskId: String) {
        apiService.deleteTask(taskId)
        database.taskDao().deleteTaskById(taskId)
    }
}
