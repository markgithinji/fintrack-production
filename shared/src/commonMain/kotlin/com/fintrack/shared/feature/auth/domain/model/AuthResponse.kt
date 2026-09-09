package com.fintrack.shared.feature.auth.domain.model

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String
)