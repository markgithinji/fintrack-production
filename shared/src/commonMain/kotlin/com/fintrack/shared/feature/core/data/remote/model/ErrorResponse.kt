package com.fintrack.shared.feature.core.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val message: String,
    val errorCode: String? = null
)
