package com.fintrack.shared.feature.auth.data.remote

import com.fintrack.shared.feature.auth.data.model.AuthResponseDto
import com.fintrack.shared.feature.auth.data.model.AuthValidationResponse
import com.fintrack.shared.feature.auth.data.model.ChangePasswordRequest
import com.fintrack.shared.feature.auth.data.model.LoginRequest
import com.fintrack.shared.feature.auth.data.model.RegisterRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class AuthApi(
    private val client: HttpClient
) {

    suspend fun login(request: LoginRequest): AuthResponseDto {
        return client.post("auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun register(request: RegisterRequest): AuthResponseDto {
        return client.post("auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getUserById(userId: String, token: String): AuthResponseDto {
        return client.get("users/$userId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }

    suspend fun validateToken(): AuthValidationResponse {
        return client.get("auth/validate") {
        }.body()
    }

    suspend fun refresh(refreshToken: String): AuthResponseDto {
        return client.post("auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("refreshToken" to refreshToken))
        }.body()
    }

    suspend fun logout(refreshToken: String?) {
        client.post("auth/logout") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("refreshToken" to refreshToken))
        }
    }

    suspend fun changePassword(request: ChangePasswordRequest) {
        client.post("auth/change-password") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}