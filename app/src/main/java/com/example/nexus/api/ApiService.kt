package com.example.nexus.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @GET("app/config")
    suspend fun getAppConfig(): AppConfig

    @GET("localization/{language}")
    suspend fun getLocalization(
        @Path("language") language: String,
    ): LocalizationBundle

    @GET("dashboard")
    suspend fun getDashboard(): DashboardData

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest,
    ): AuthResponse

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest,
    ): AuthResponse

    @GET("users/me")
    suspend fun getProfile(): UserProfile

    @PATCH("users/me")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest,
    ): UserProfile

    @POST("users/me/password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest,
    )

    @DELETE("users/me")
    suspend fun deleteAccount()

    @GET("habits")
    suspend fun getHabits(): List<Habit>

    @POST("habits")
    suspend fun createHabit(
        @Body request: CreateHabitRequest,
    ): Habit

    @PATCH("habits/{habitId}")
    suspend fun updateHabit(
        @Path("habitId") habitId: String,
        @Body request: UpdateHabitRequest,
    ): Habit

    @DELETE("habits/{habitId}")
    suspend fun deleteHabit(
        @Path("habitId") habitId: String,
    )

    @GET("projects")
    suspend fun getProjects(): List<Project>

    @GET("projects/{projectId}")
    suspend fun getProject(
        @Path("projectId") projectId: String,
    ): Project

    @POST("projects")
    suspend fun createProject(
        @Body request: CreateProjectRequest,
    ): Project

    @PUT("projects/{projectId}")
    suspend fun updateProject(
        @Path("projectId") projectId: String,
        @Body request: UpdateProjectRequest,
    ): Project

    @DELETE("projects/{projectId}")
    suspend fun deleteProject(
        @Path("projectId") projectId: String,
    )

    @GET("projects/{projectId}/tasks")
    suspend fun getTasks(
        @Path("projectId") projectId: String,
    ): List<Task>

    @POST("projects/{projectId}/tasks")
    suspend fun createTask(
        @Path("projectId") projectId: String,
        @Body request: CreateTaskRequest,
    ): Task

    @PUT("tasks/{taskId}")
    suspend fun updateTask(
        @Path("taskId") taskId: String,
        @Body request: UpdateTaskRequest,
    ): Task

    @DELETE("tasks/{taskId}")
    suspend fun deleteTask(
        @Path("taskId") taskId: String,
    )
}
