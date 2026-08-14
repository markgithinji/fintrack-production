package com.fintrack.shared.feature.core.util

import com.fintrack.shared.feature.core.data.model.ApiException
import com.fintrack.shared.feature.core.data.model.AuthErrorType
import com.fintrack.shared.feature.core.data.remote.model.ErrorResponse
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

suspend fun <T> safeApiCall(apiCall: suspend () -> T): Result<T> {
    return try {
        val result = apiCall()
        Result.Success(result)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val domainException = convertToDomainException(e)
        Result.Error(domainException)
    }
}

private suspend fun convertToDomainException(e: Exception): ApiException {
    return when (e) {
        is ApiException -> e

        is RedirectResponseException -> handleRedirectException(e)
        is ClientRequestException -> handleClientException(e)
        is ServerResponseException -> handleServerException(e)

        is SerializationException -> {
            ApiException.SerializationFailure("Failed to parse response: ${e.message}")
        }

        is HttpRequestTimeoutException -> {
            val cleanMessage = e.message.cleanKtorMessage()
            ApiException.Network(cleanMessage.ifEmpty { "Request timeout" })
        }

        is CancellationException -> throw e

        is IllegalStateException -> {
            ApiException.InvalidState("Invalid app state: ${e.message}")
        }

        else -> {
            val className = e::class.simpleName ?: ""
            if (className.contains("IOException") ||
                className.contains("ConnectException") ||
                className.contains("SocketException") ||
                className.contains("UnknownHostException")
            ) {
                val cleanMessage = e.message.cleanKtorMessage()
                ApiException.Network(cleanMessage.ifEmpty { "Network connection failed" })
            } else {
                handleUnknownException(e)
            }
        }
    }
}

private fun handleRedirectException(e: RedirectResponseException): ApiException {
    val statusCode = e.response.status.value
    val cleanMessage = e.message.cleanKtorMessage()
    return when (statusCode) {
        401 -> ApiException.Unauthorized("Authentication required")
        403 -> ApiException.Forbidden("Access denied")
        else -> ApiException.Network("Redirect error: $cleanMessage")
    }
}

private suspend fun handleClientException(e: ClientRequestException): ApiException {
    val statusCode = e.response.status.value
    val url = e.response.call.request.url.toString()
    val errorResponse = try {
        e.response.body<ErrorResponse>()
    } catch (parseException: Exception) {
        null
    }

    val message = errorResponse?.message ?: "Client error occurred"
    val errorCode = errorResponse?.errorCode

    return when (statusCode) {
        400 -> {
            if (errorCode != null) {
                mapToAuthException(message, errorCode) ?: ApiException.Validation(message)
            } else {
                ApiException.Validation(message)
            }
        }
        401 -> {
            if (errorCode != null) {
                mapToAuthException(message, errorCode) ?: ApiException.Auth(message, AuthErrorType.UNAUTHORIZED)
            } else {
                ApiException.Auth(message, AuthErrorType.INVALID_CREDENTIALS)
            }
        }
        403 -> ApiException.Forbidden("Access denied: $message")
        404 -> ApiException.NotFound("Resource not found at $url: $message")
        409 -> {
            if (errorCode != null) {
                mapToAuthException(message, errorCode) ?: ApiException.ClientError(message, statusCode)
            } else {
                ApiException.ClientError(message, statusCode)
            }
        }
        422 -> ApiException.Validation(message)
        in 400..499 -> ApiException.ClientError("$message (at $url)", statusCode)
        else -> ApiException.Unknown("Unexpected client error: $message (at $url)")
    }
}

private fun mapToAuthException(message: String, errorCode: String): ApiException.Auth? {
    val type = when (errorCode) {
        "INVALID_CREDENTIALS" -> AuthErrorType.INVALID_CREDENTIALS
        "USER_NOT_FOUND" -> AuthErrorType.USER_NOT_FOUND
        "INVALID_PASSWORD" -> AuthErrorType.INVALID_PASSWORD
        "USER_ALREADY_EXISTS" -> AuthErrorType.USER_ALREADY_EXISTS
        "WEAK_PASSWORD" -> AuthErrorType.WEAK_PASSWORD
        "SESSION_EXPIRED", "REFRESH_TOKEN_EXPIRED" -> AuthErrorType.SESSION_EXPIRED
        "TOKEN_REVOKED" -> AuthErrorType.TOKEN_REVOKED
        "TOKEN_EXPIRED" -> AuthErrorType.TOKEN_EXPIRED
        "INVALID_TOKEN", "INVALID_REFRESH_TOKEN" -> AuthErrorType.INVALID_TOKEN
        "UNAUTHORIZED" -> AuthErrorType.UNAUTHORIZED
        else -> return null
    }
    return ApiException.Auth(message, type)
}

private fun handleServerException(e: ServerResponseException): ApiException {
    val statusCode = e.response.status.value
    val cleanMessage = e.message.cleanKtorMessage()

    return when (statusCode) {
        500 -> ApiException.ServerError("Internal server error", statusCode)
        503 -> ApiException.ServerError("Service unavailable", statusCode)
        else -> ApiException.ServerError("Server error $statusCode: $cleanMessage", statusCode)
    }
}

private fun handleUnknownException(e: Exception): ApiException {
    val message = e.message.cleanKtorMessage()
    return ApiException.Unknown(message.ifEmpty { "Unknown error occurred" })
}

private fun String?.cleanKtorMessage(): String {
    if (this == null) return ""
    return this.substringBefore("[").trim()
}
