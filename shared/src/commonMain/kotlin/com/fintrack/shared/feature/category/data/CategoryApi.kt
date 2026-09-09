package com.fintrack.shared.feature.category.data

import com.fintrack.shared.feature.core.data.model.ApiResponse
import com.fintrack.shared.feature.category.data.model.CategoryDto
import com.fintrack.shared.feature.category.data.model.CreateCategoryRequest
import com.fintrack.shared.feature.category.domain.model.CategoryRule
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class CategoryApi(
    private val client: HttpClient
) {
    suspend fun getCategories(): List<CategoryDto> {
        val response: ApiResponse<List<CategoryDto>> = client.get("categories").body()
        return response.result
    }

    suspend fun getCategoryRules(): List<CategoryRule> {
        val response: ApiResponse<List<CategoryRule>> = client.get("categories/rules").body()
        return response.result
    }

    suspend fun addCategory(name: String, isExpense: Boolean, iconName: String? = null): CategoryDto {
        val response: ApiResponse<CategoryDto> = client.post("categories") {
            contentType(ContentType.Application.Json)
            setBody(CreateCategoryRequest(name, isExpense, iconName))
        }.body()
        return response.result
    }

    suspend fun deleteCategory(id: String) {
        client.delete("categories/$id")
    }
}
