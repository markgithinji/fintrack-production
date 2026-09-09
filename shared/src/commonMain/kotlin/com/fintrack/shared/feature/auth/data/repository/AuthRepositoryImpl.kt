package com.fintrack.shared.feature.auth.data.repository

import com.fintrack.shared.feature.auth.data.model.ChangePasswordRequest
import com.fintrack.shared.feature.auth.data.model.LoginRequest
import com.fintrack.shared.feature.auth.data.model.RegisterRequest
import com.fintrack.shared.feature.auth.data.model.toDomain
import com.fintrack.shared.feature.auth.data.remote.AuthApi
import com.fintrack.shared.feature.auth.domain.datasource.TokenDataSource
import com.fintrack.shared.feature.auth.domain.model.AuthResponse
import com.fintrack.shared.feature.auth.domain.repository.AuthRepository
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.safeApiCall
import kotlinx.coroutines.flow.firstOrNull

class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val tokenDataSource: TokenDataSource
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<AuthResponse> =
        safeApiCall {
            val authResponse = authApi.login(LoginRequest(email, password))
            val domainResponse = authResponse.toDomain()
            tokenDataSource.saveTokens(domainResponse.accessToken, domainResponse.refreshToken)
            domainResponse
        }

    override suspend fun register(
        name: String,
        email: String,
        password: String
    ): Result<AuthResponse> =
        safeApiCall {
            val authResponse = authApi.register(RegisterRequest(name, email, password))
            val domainResponse = authResponse.toDomain()
            tokenDataSource.saveTokens(domainResponse.accessToken, domainResponse.refreshToken)
            domainResponse
        }

    override suspend fun getUserById(userId: String, token: String): Result<AuthResponse> =
        safeApiCall {
            val authResponse = authApi.getUserById(userId, token)
            authResponse.toDomain()
        }

    override suspend fun validateToken(token: String): Result<Boolean> =
        safeApiCall {
            val response = authApi.validateToken()
            response.isValid
        }

    override suspend fun logout(): Result<Unit> =
        safeApiCall {
            val refreshToken = tokenDataSource.refreshToken.firstOrNull()
            if (refreshToken != null) {
                authApi.logout(refreshToken)
            }
            tokenDataSource.clearTokens()
        }

    override suspend fun refreshToken(refreshToken: String): Result<AuthResponse> =
        safeApiCall {
            val authResponse = authApi.refresh(refreshToken)
            val domainResponse = authResponse.toDomain()
            tokenDataSource.saveTokens(domainResponse.accessToken, domainResponse.refreshToken)
            domainResponse
        }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> =
        safeApiCall {
            authApi.changePassword(ChangePasswordRequest(currentPassword, newPassword))
        }
}