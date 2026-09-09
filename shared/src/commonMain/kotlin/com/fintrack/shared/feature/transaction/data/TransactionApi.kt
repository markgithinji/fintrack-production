package com.fintrack.shared.feature.transaction.data

import com.fintrack.shared.feature.core.data.model.ApiResponse
import com.fintrack.shared.feature.transaction.data.model.PaginatedTransactionDto
import com.fintrack.shared.feature.transaction.data.model.CreateTransactionRequest
import com.fintrack.shared.feature.transaction.data.model.TransactionDto
import com.fintrack.shared.feature.transaction.domain.model.RecurringBill
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

class TransactionApi(
    private val client: HttpClient
) {
    suspend fun getTransactions(
        limit: Int = 20,
        sortBy: String = "date",
        order: String = "DESC",
        afterDateTime: String? = null,
        afterId: String? = null,
        accountId: String? = null,
        isIncome: Boolean? = null,
        categoryId: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        hasTransactionCost: Boolean? = null
    ): PaginatedTransactionDto {
        val response: ApiResponse<PaginatedTransactionDto> = client.get("transactions") {
            parameter("limit", limit)
            parameter("sortBy", sortBy)
            parameter("order", order)
            afterDateTime?.let { parameter("afterDateTime", it) }
            afterId?.let { parameter("afterId", it) }
            accountId?.let { parameter("accountId", it) }
            isIncome?.let { parameter("isIncome", it) }
            categoryId?.let { parameter("categoryId", it) }
            startDate?.let { parameter("start", it) }
            endDate?.let { parameter("end", it) }
            hasTransactionCost?.let { parameter("hasCost", it) }
        }.body()
        return response.result
    }

    suspend fun addTransaction(request: CreateTransactionRequest): TransactionDto {
        val response: ApiResponse<TransactionDto> = client.post("transactions") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
        return response.result
    }

    suspend fun addTransactions(requests: List<CreateTransactionRequest>) {
        client.post("transactions/batch") {
            contentType(ContentType.Application.Json)
            setBody(requests)
        }.bodyAsText()
    }

    suspend fun importMpesaTransactions(requests: List<CreateTransactionRequest>) {
        client.post("transactions/mpesa") {
            contentType(ContentType.Application.Json)
            setBody(requests)
        }.bodyAsText()
    }

    suspend fun importEquityTransactions(requests: List<CreateTransactionRequest>) {
        client.post("transactions/equity") {
            contentType(ContentType.Application.Json)
            setBody(requests)
        }.bodyAsText()
    }

    suspend fun getTransaction(id: String): TransactionDto {
        val response: ApiResponse<TransactionDto> = client.get("transactions/$id").body()
        return response.result
    }

    suspend fun updateTransaction(id: String, request: CreateTransactionRequest): TransactionDto {
        val response: ApiResponse<TransactionDto> = client.put("transactions/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
        return response.result
    }

    suspend fun deleteTransaction(id: String) {
        client.delete("transactions/$id")
    }

    suspend fun deleteAllTransactions(accountIds: List<String>? = null) {
        client.delete("transactions/clear") {
            accountIds?.forEach { parameter("accountId", it) }
        }
    }

    suspend fun getRecurringBills(): List<RecurringBill> {
        val response: ApiResponse<List<RecurringBill>> = client.get("transactions/recurring/detect").body()
        return response.result
    }
}
