package com.fintrack.shared.feature.auth.domain.datasource

import kotlinx.coroutines.flow.Flow

interface TokenDataSource {
    val accessToken: Flow<String?>
    val refreshToken: Flow<String?>
    suspend fun saveTokens(accessToken: String, refreshToken: String)
    suspend fun clearTokens()
}