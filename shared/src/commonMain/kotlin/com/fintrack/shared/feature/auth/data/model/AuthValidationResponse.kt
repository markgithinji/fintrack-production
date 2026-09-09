package com.fintrack.shared.feature.auth.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthValidationResponse(
    val isValid: Boolean,
    val userId: String?,
    val message: String
)