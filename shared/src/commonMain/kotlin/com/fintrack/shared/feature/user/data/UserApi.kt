package com.fintrack.shared.feature.user.data

import com.fintrack.shared.feature.core.data.model.ApiResponse
import com.fintrack.shared.feature.user.data.model.TrackedCategoriesRequest
import com.fintrack.shared.feature.user.data.model.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class UserApi(
    private val client: HttpClient
) {
    suspend fun getUserProfile(): UserDto {
        val response: ApiResponse<UserDto> = client.get("users/me").body()
        return response.result
    }

    suspend fun updateProfile(name: String, email: String): UserDto {
        val response: ApiResponse<UserDto> = client.put("users/me") {
            contentType(ContentType.Application.Json)
            setBody(UserDto(name = name, email = email))
        }.body()
        return response.result
    }

    suspend fun updateTrackedCategories(categories: List<String>): UserDto {
        val response: ApiResponse<UserDto> = client.put("users/preferences/tracked-categories") {
            contentType(ContentType.Application.Json)
            setBody(TrackedCategoriesRequest(categories))
        }.body()
        return response.result
    }

    suspend fun deleteUser() {
        client.delete("users/me")
    }
}
