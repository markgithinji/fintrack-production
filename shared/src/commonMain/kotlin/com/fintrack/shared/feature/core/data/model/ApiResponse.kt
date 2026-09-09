package com.fintrack.shared.feature.core.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val result: T
)