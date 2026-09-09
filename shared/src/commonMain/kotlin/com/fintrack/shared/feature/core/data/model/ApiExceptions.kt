package com.fintrack.shared.feature.core.data.model

sealed class ApiException(open val details: String) : Exception(details) {
    data class Network(override val details: String) : ApiException(details)
    data class SerializationFailure(override val details: String) : ApiException(details)
    data class InvalidState(override val details: String) : ApiException(details)

    // HTTP status code exceptions
    data class Unauthorized(override val details: String) : ApiException(details)
    data class Forbidden(override val details: String) : ApiException(details)
    data class NotFound(override val details: String) : ApiException(details)
    data class ClientError(override val details: String, val statusCode: Int) : ApiException(details)
    data class ServerError(override val details: String, val statusCode: Int) : ApiException(details)

    // Domain specific exceptions
    data class Validation(override val details: String) : ApiException(details)
    data class Auth(override val details: String, val type: AuthErrorType) : ApiException(details)

    data class Unknown(override val details: String) : ApiException(details)
}

enum class AuthErrorType {
    INVALID_CREDENTIALS,
    USER_NOT_FOUND,
    INVALID_PASSWORD,
    USER_ALREADY_EXISTS,
    WEAK_PASSWORD,
    SESSION_EXPIRED,
    TOKEN_REVOKED,
    TOKEN_EXPIRED,
    INVALID_TOKEN,
    UNAUTHORIZED,
    UNKNOWN
}

fun Throwable.getUserFriendlyMessage(): String {
    if (this is ApiException) {
        return when (this) {
            is ApiException.Auth -> when (type) {
                AuthErrorType.INVALID_CREDENTIALS -> "The email or password you entered is incorrect."
                AuthErrorType.USER_NOT_FOUND -> "No account found with this email."
                AuthErrorType.INVALID_PASSWORD -> "The password you entered is incorrect."
                AuthErrorType.USER_ALREADY_EXISTS -> "An account with this email already exists."
                AuthErrorType.WEAK_PASSWORD -> "Your password is too weak. Try a stronger one."
                AuthErrorType.SESSION_EXPIRED,
                AuthErrorType.TOKEN_EXPIRED -> "Your session has expired. Please log in again."
                AuthErrorType.TOKEN_REVOKED -> "This session has been revoked. Please log in again."
                AuthErrorType.INVALID_TOKEN -> "Invalid authentication. Please log in again."
                AuthErrorType.UNAUTHORIZED -> "You are not authorized to perform this action."
                AuthErrorType.UNKNOWN -> "An authentication error occurred. Please try again."
            }
            is ApiException.Network -> details.ifEmpty { "Connection failed. Please check your internet and try again." }
            is ApiException.Validation -> details
            is ApiException.NotFound -> if (details.contains("http")) details else "The requested information could not be found."
            is ApiException.ServerError -> "Something went wrong on our end. We're working on it!"
            is ApiException.Unauthorized -> "Authentication required. Please log in again."
            is ApiException.Forbidden -> "You don't have permission to do this."
            is ApiException.ClientError -> if (details.contains("{") || details.contains("[")) "We couldn't process your request. Please try again." else details
            is ApiException.SerializationFailure -> "Data format error. Please try again later."
            is ApiException.Unknown -> "An unexpected error occurred. Please try again."
            else -> "An unexpected error occurred. Please try again."
        }
    }

    val message = this.message ?: ""
    return when {
        message.contains("timeout", ignoreCase = true) -> "Connection timed out. Please check your internet and try again."
        message.contains("unresolved", ignoreCase = true) || message.contains("address", ignoreCase = true) -> "No internet connection. Please check your network."
        message.contains("Connection refused", ignoreCase = true) -> "Unable to reach the server. It might be down."
        else -> "An unexpected error occurred. Please try again."
    }
}
