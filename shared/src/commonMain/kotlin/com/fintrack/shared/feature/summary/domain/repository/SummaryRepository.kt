package com.fintrack.shared.feature.summary.domain.repository

import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.summary.domain.model.AvailableMonths
import com.fintrack.shared.feature.summary.domain.model.AvailableWeeks
import com.fintrack.shared.feature.summary.domain.model.AvailableYears
import com.fintrack.shared.feature.summary.domain.model.CategoryComparison
import com.fintrack.shared.feature.summary.domain.model.CategoryComparisonSummary
import com.fintrack.shared.feature.summary.domain.model.DistributionSummary
import com.fintrack.shared.feature.summary.domain.model.OverviewSummary
import com.fintrack.shared.feature.summary.domain.model.ProfileMetrics
import com.fintrack.shared.feature.summary.domain.model.StatisticsSummary
import com.fintrack.shared.feature.summary.domain.model.TransactionCountSummary

interface SummaryRepository {
    suspend fun getHighlightsSummary(
        accountId: String?,
        period: String? = null
    ): Result<StatisticsSummary>
    suspend fun getDistributionSummary(
        weekOrMonthCode: String,
        type: String?,
        start: String?,
        end: String?,
        accountId: String?
    ): Result<DistributionSummary>
    suspend fun getAvailableWeeks(accountId: String?): Result<AvailableWeeks>
    suspend fun getAvailableMonths(accountId: String?): Result<AvailableMonths>
    suspend fun getAvailableYears(accountId: String?): Result<AvailableYears>
    suspend fun getOverviewSummary(accountId: String?): Result<OverviewSummary>
    suspend fun getCategoryComparisons(
        accountId: String?,
        period: String? = null
    ): Result<CategoryComparisonSummary>
    suspend fun getTransactionCounts(
        accountId: String,
        isIncome: Boolean? = null,
        categoryId: String? = null,
        start: String? = null,
        end: String? = null,
        hasTransactionCost: Boolean? = null
    ): Result<TransactionCountSummary>

    suspend fun getProfileMetrics(): Result<ProfileMetrics>
}