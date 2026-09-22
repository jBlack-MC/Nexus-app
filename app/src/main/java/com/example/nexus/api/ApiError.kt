package com.example.nexus.api

import retrofit2.HttpException
import java.io.IOException

sealed class ApiError {
    object SessionExpired : ApiError()         // 401 / 403
    data class ServerError(val code: Int) : ApiError()
    object NoConnection : ApiError()
    data class Unknown(val message: String?) : ApiError()
}

fun Throwable.toApiError(): ApiError = when (this) {
    is HttpException -> when (code()) {
        401, 403 -> ApiError.SessionExpired
        in 500..599 -> ApiError.ServerError(code())
        else -> ApiError.Unknown(message())
    }
    is IOException -> ApiError.NoConnection
    else -> ApiError.Unknown(message)
}

fun ApiError.toUserMessage(): String = when (this) {
    ApiError.SessionExpired -> "Your session has expired. Please log in again."
    is ApiError.ServerError -> "Something went wrong on our end. Please try again shortly."
    ApiError.NoConnection -> "No internet connection. Check your network and try again."
    is ApiError.Unknown -> "Something went wrong. Please try again."
}
