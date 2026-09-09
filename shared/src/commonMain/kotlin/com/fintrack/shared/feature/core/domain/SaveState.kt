package com.fintrack.shared.feature.core.domain

sealed class SaveState<out T> {
    object Idle : SaveState<Nothing>()
    object Loading : SaveState<Nothing>()
    data class Success<T>(val data: T) : SaveState<T>()
    data class Error(val exception: Throwable) : SaveState<Nothing>()
}