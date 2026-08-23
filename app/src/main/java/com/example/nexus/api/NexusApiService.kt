package com.example.nexus.api

import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST

interface NexusApiService {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @GET("dashboard")
    suspend fun getDashboardData(): DashboardData
}