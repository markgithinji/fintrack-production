package com.fintrack.shared.feature.summary.data.repository

import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.safeApiCall
import com.fintrack.shared.feature.summary.data.model.toDomain
import com.fintrack.shared.feature.summary.data.network.SummaryApi
import com.fintrack.shared.feature.summary.domain.model.AvailableMonths
import com.fintrack.shared.feature.summary.domain.model.AvailableWeeks
import com.fintrack.shared.feature.summary.domain.model.AvailableYears
import com.fintrack.shared.feature.summary.domain.model.CategoryComparisonSummary
import com.fintrack.shared.feature.summary.domain.model.DistributionSummary
import com.fintrack.shared.feature.summary.domain.model.OverviewSummary
import com.fintrack.shared.feature.summary.domain.model.ProfileMetrics
import com.fintrack.shared.feature.summary.domain.model.StatisticsSummary
import com.fintrack.shared.feature.summary.domain.model.TransactionCountSummary
import com.fintrack.shared.feature.summary.domain.repository.SummaryRepository

class SummaryRepositoryImpl(
    private val summaryApi: SummaryApi
) : SummaryRepository {

    override suspend fun getHighlightsSummary(
        accountId: String?,
        period: String?
    ): Result<StatisticsSummary> =
        safeApiCall {
            summaryApi.getHighlightsSummary(accountId, period).toDomain()
        }

    override suspend fun getDistributionSummary(
        weekOrMonthCode: String,
        type: String?,
        start: String?,
        end: String?,
        accountId: String?
    ): Result<DistributionSummary> =
        safeApiCall {
            summaryApi.getDistributionSummary(weekOrMonthCode, type, start, end, accountId).toDomain()
        }

    override suspend fun getAvailableWeeks(accountId: String?): Result<AvailableWeeks> =
        safeApiCall {
            summaryApi.getAvailableWeeks(accountId).toDomain()
        }

    override suspend fun getAvailableMonths(accountId: String?): Result<AvailableMonths> =
        safeApiCall {
            summaryApi.getAvailableMonths(accountId).toDomain()
        }

    override suspend fun getAvailableYears(accountId: String?): Result<AvailableYears> =
        safeApiCall {
            summaryApi.getAvailableYears(accountId).toDomain()
        }

    override suspend fun getOverviewSummary(accountId: String?): Result<OverviewSummary> =
        safeApiCall {
            summaryApi.getOverviewSummary(accountId).toDomain()
        }

    override suspend fun getCategoryComparisons(
        accountId: String?,
        period: String?
    ): Result<CategoryComparisonSummary> =
        safeApiCall {
            summaryApi.getCategoryComparisons(accountId, period).toDomain()
        }

    override suspend fun getTransactionCounts(
        accountId: String,
        isIncome: Boolean?,
        categoryId: String?,
        start: String?,
        end: String?,
        hasTransactionCost: Boolean?
    ): Result<TransactionCountSummary> = safeApiCall {
        summaryApi.getTransactionCounts(accountId, isIncome, categoryId, start, end, hasTransactionCost).toDomain()
    }

    override suspend fun getProfileMetrics(): Result<ProfileMetrics> = safeApiCall {
        summaryApi.getProfileMetrics().toDomain()
    }
}