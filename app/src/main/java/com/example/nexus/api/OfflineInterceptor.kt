package com.example.nexus.api

import com.example.nexus.auth.LocalAccountManager
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
                    val success =
                        LocalAccountManager.register(
                            email = regRequest.email,
                            displayName = regRequest.displayName,
                            password = regRequest.password,
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
                successResponse(
                    request,
                    UserProfile(
                        email = user?.email ?: email,
                        displayName = user?.displayName ?: "Offline User",
                    ),
                )
            }
            // Dashboard, project, task and habit reads are deliberately NOT faked. A fabricated
            // payload is returned as a successful HTTP response, so the repository would cache it -
            // for example writing zeroed counts over good cached data. Letting the original
            // IOException surface keeps the cache intact and lets NexusRepository fall back to the
            // local database.
            //
            // Writes are not faked either. Returning a synthetic 200 reported a save that never
            // reached the server, and the repository then tried to insert a payload with a null id.
            // Until a real offline write queue exists, writes fail with a clear connection error so
            // the UI can tell the user the change was not saved.
            else -> throw IOException("Offline: no backend connection for $method $url")
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

    private fun successResponse(
        request: Request,
        data: Any,
    ): Response {
        val json = gson.toJson(data)
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK (Offline)")
            .body(json.toResponseBody("application/json".toMediaType()))
            .build()
    }

    private fun errorResponse(
        request: Request,
        code: Int,
        message: String,
    ): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(message)
            .body("{\"message\":\"$message\"}".toResponseBody("application/json".toMediaType()))
            .build()
    }
}
