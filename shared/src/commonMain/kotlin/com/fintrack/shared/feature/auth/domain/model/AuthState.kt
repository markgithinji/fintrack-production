package com.fintrack.shared.feature.auth.domain.model

sealed class AuthState<out T> {
    object Idle : AuthState<Nothing>()
    data class Loading(val message: String = "") : AuthState<Nothing>()
    data class Success<T>(val data: T) : AuthState<T>()
    data class Error(val exception: Throwable) : AuthState<Nothing>()
}