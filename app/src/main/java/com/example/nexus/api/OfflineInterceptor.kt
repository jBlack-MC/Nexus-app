package com.example.nexus.api

import com.example.nexus.auth.LocalAccountManager
import com.example.nexus.auth.LocalUser
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.IOException

class OfflineInterceptor : Interceptor {
    private val gson = Gson()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return try {
            chain.proceed(request)
        } catch (e: IOException) {
            handleOffline(request)
        }
    }

    private fun handleOffline(request: Request): Response {
        val url = request.url.toString()
        val method = request.method

        return when {
            url.contains("auth/login") && method == "POST" -> {
                val loginRequest = parseRequestBody<LoginRequest>(request)
                val user = loginRequest?.let { LocalAccountManager.authenticate(it.email, it.password) }
                if (user != null) {
                    successResponse(request, AuthResponse(token = "offline_token_${user.email}"))
                } else {
                    errorResponse(request, 401, "Invalid credentials (Offline)")
                }
            }
            url.contains("auth/register") && method == "POST" -> {
                val regRequest = parseRequestBody<RegisterRequest>(request)
                if (regRequest != null) {
                    val success = LocalAccountManager.register(
                        LocalUser(regRequest.email, regRequest.password, regRequest.displayName)
                    )
                    if (success) {
                        successResponse(request, AuthResponse(token = "offline_token_${regRequest.email}"))
                    } else {
                        errorResponse(request, 400, "User already exists (Offline)")
                    }
                } else {
                    errorResponse(request, 400, "Invalid registration data")
                }
            }
            url.contains("users/me") && method == "GET" -> {
                // Return a generic profile since we don't store full profiles in TokenStore normally
                // but we can derive it from LocalAccountManager
                val authHeader = request.header("Authorization")
                val email = authHeader?.substringAfter("offline_token_") ?: "unknown@nexus-app.com"
                val user = LocalAccountManager.getUserByEmail(email)
                successResponse(request, UserProfile(
                    email = user?.email ?: email,
                    displayName = user?.displayName ?: "Offline User"
                ))
            }
            url.contains("dashboard") && method == "GET" -> {
                successResponse(request, DashboardData(projects = 0, tasks = 0, activity = 0))
            }
            // For all other POST/PUT/DELETE, just return success to allow the local repository to cache
            method in listOf("POST", "PUT", "DELETE", "PATCH") -> {
                successResponse(request, Any()) 
            }
            // For other GETs, if they are not caught, they will just fail with the original IOException
            // unless we want to return empty lists. 
            // In NexusRepository, GETs for projects/tasks are already wrapped in try-catch to fallback to Room.
            else -> throw IOException("Offline: No backend connection and no fallback for $url")
        }
    }

    private inline fun <reified T> parseRequestBody(request: Request): T? {
        return try {
            val buffer = Buffer()
            request.body?.writeTo(buffer)
            gson.fromJson(buffer.readUtf8(), T::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun successResponse(request: Request, data: Any): Response {
        val json = gson.toJson(data)
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK (Offline)")
            .body(json.toResponseBody("application/json".toMediaType()))
            .build()
    }

    private fun errorResponse(request: Request, code: Int, message: String): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(message)
            .body("{\"message\":\"$message\"}".toResponseBody("application/json".toMediaType()))
            .build()
    }
}
