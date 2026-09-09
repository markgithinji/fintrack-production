package com.fintrack.shared.feature.summary.data.network

import com.fintrack.shared.feature.core.data.model.ApiResponse
import com.fintrack.shared.feature.summary.data.model.AvailableMonthsDto
import com.fintrack.shared.feature.summary.data.model.AvailableWeeksDto
import com.fintrack.shared.feature.summary.data.model.AvailableYearsDto
import com.fintrack.shared.feature.summary.data.model.CategoryComparisonSummaryDto
import com.fintrack.shared.feature.summary.data.model.DistributionSummaryDto
import com.fintrack.shared.feature.summary.data.model.HighlightsSummaryDto
import com.fintrack.shared.feature.summary.data.model.OverviewSummaryDto
import com.fintrack.shared.feature.summary.data.model.ProfileMetricsDto
import com.fintrack.shared.feature.summary.data.model.TransactionCountSummaryDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class SummaryApi(
    private val client: HttpClient
) {
    suspend fun getHighlightsSummary(
        accountId: String? = null,
        period: String? = null
    ): HighlightsSummaryDto {
        val response: ApiResponse<HighlightsSummaryDto> =
            client.get("transactions/summary/highlights") {
                accountId?.let { parameter("accountId", it) }
                period?.let { parameter("period", it) }
            }.body()
        return response.result
    }

    suspend fun getDistributionSummary(
        period: String,
        type: String? = null,
        start: String? = null,
        end: String? = null,
        accountId: String? = null
    ): DistributionSummaryDto {
        val response: ApiResponse<DistributionSummaryDto> =
            client.get("transactions/summary/distribution") {
                parameter("period", period)
                type?.let { parameter("type", it) }
                start?.let { parameter("start", it) }
                end?.let { parameter("end", it) }
                accountId?.let { parameter("accountId", it) }
            }.body()
        return response.result
    }

    suspend fun getAvailableWeeks(accountId: String? = null): AvailableWeeksDto {
        val response: ApiResponse<AvailableWeeksDto> =
            client.get("transactions/summary/available-weeks") {
                accountId?.let { parameter("accountId", it) }
            }.body()
        return response.result
    }

    suspend fun getAvailableMonths(accountId: String? = null): AvailableMonthsDto {
        val response: ApiResponse<AvailableMonthsDto> =
            client.get("transactions/summary/available-months") {
                accountId?.let { parameter("accountId", it) }
            }.body()
        return response.result
    }

    suspend fun getAvailableYears(accountId: String? = null): AvailableYearsDto {
        val response: ApiResponse<AvailableYearsDto> =
            client.get("transactions/summary/available-years") {
                accountId?.let { parameter("accountId", it) }
            }.body()
        return response.result
    }

    suspend fun getOverviewSummary(accountId: String? = null): OverviewSummaryDto {
        val response: ApiResponse<OverviewSummaryDto> =
            client.get("transactions/summary/overview") {
                accountId?.let { parameter("accountId", it) }
            }.body()
        return response.result
    }

    suspend fun getCategoryComparisons(
        accountId: String? = null,
        period: String? = null
    ): CategoryComparisonSummaryDto {
        val response: ApiResponse<CategoryComparisonSummaryDto> =
            client.get("transactions/summary/category-comparison") {
                accountId?.let { parameter("accountId", it) }
                period?.let { parameter("period", it) }
            }.body()
        return response.result
    }

    suspend fun getTransactionCounts(
        accountId: String,
        isIncome: Boolean? = null,
        categoryId: String? = null,
        start: String? = null,
        end: String? = null,
        hasTransactionCost: Boolean? = null
    ): TransactionCountSummaryDto {
        val response: ApiResponse<TransactionCountSummaryDto> =
            client.get("transactions/summary/counts") {
                parameter("accountId", accountId)
                isIncome?.let { parameter("isIncome", it) }
                categoryId?.let { parameter("categoryId", it) }
                start?.let { parameter("start", it) }
                end?.let { parameter("end", it) }
                hasTransactionCost?.let { parameter("hasCost", it) }
            }.body()
        return response.result
    }

    suspend fun getProfileMetrics(): ProfileMetricsDto {
        val response: ApiResponse<ProfileMetricsDto> =
            client.get("transactions/summary/profile-metrics").body()
        return response.result
    }
}