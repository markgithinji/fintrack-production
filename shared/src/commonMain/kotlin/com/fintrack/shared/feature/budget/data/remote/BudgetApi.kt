package com.fintrack.shared.feature.budget.data.remote

import com.fintrack.shared.feature.budget.data.model.BudgetWithStatusDto
import com.fintrack.shared.feature.budget.data.model.CreateBudgetRequest
import com.fintrack.shared.feature.budget.data.model.UpdateBudgetRequest
import com.fintrack.shared.feature.core.data.model.ApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType


class BudgetApi(
    private val client: HttpClient
) {

    suspend fun addBudget(request: CreateBudgetRequest): BudgetWithStatusDto {
        val response: ApiResponse<BudgetWithStatusDto> = client.post("budgets") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
        return response.result
    }

    suspend fun updateBudget(id: String, request: UpdateBudgetRequest): BudgetWithStatusDto {
        val response: ApiResponse<BudgetWithStatusDto> = client.put("budgets/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
        return response.result
    }

    suspend fun deleteBudget(id: String) {
        client.delete("budgets/$id")
    }

    suspend fun getBudgets(
        limit: Int = 20,
        offset: Long = 0,
        accountId: String? = null
    ): List<BudgetWithStatusDto> {
        val response: ApiResponse<List<BudgetWithStatusDto>> = client.get("budgets") {
            parameter("limit", limit)
            parameter("offset", offset)
            accountId?.let { parameter("accountId", it) }
        }.body()
        return response.result
    }

    suspend fun getBudgetById(id: String): BudgetWithStatusDto {
        val response: ApiResponse<BudgetWithStatusDto> = client.get("budgets/$id").body()
        return response.result
    }

    suspend fun deleteAllBudgets(accountIds: List<String>? = null) {
        client.delete("budgets/clear") {
            accountIds?.forEach { parameter("accountId", it) }
        }
    }
}