package com.fintrack.shared.feature.summary.data.repository

import com.fintrack.shared.db.FintrackDatabase
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.summary.domain.model.*
import com.fintrack.shared.feature.summary.domain.repository.SummaryRepository
import com.fintrack.shared.feature.core.logger.KMPLogger
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class SummaryRepositoryOfflineImpl(
    private val database: FintrackDatabase,
    private val logger: KMPLogger
) : SummaryRepository {

    private val queries = database.fintrackDatabaseQueries
    private val offlineUserId = "offline_user"
    private val TAG = "SummaryRepo"

    override suspend fun getHighlightsSummary(
        accountId: String?,
        period: String?
    ): Result<StatisticsSummary> = withContext(Dispatchers.IO) {
        try {
            val result = queries.getHighlights(accountId = accountId, userId = offlineUserId).executeAsOne()
            
            Result.Success(
                StatisticsSummary(
                    period = period ?: "All Time",
                    income = BigDecimal.fromDouble(result.incomeTotal ?: 0.0),
                    expense = BigDecimal.fromDouble(result.expenseTotal ?: 0.0),
                    balance = BigDecimal.fromDouble((result.incomeTotal ?: 0.0) - (result.expenseTotal ?: 0.0)),
                    totalTransactionCost = BigDecimal.fromDouble(result.feesTotal ?: 0.0)
                )
            )
        } catch (e: Exception) {
            logger.error(TAG, "Error calculating highlights", e)
            Result.Error(e)
        }
    }

    override suspend fun getDistributionSummary(
        weekOrMonthCode: String,
        type: String?,
        start: String?,
        end: String?,
        accountId: String?
    ): Result<DistributionSummary> = withContext(Dispatchers.IO) {
        try {
            Result.Success(
                DistributionSummary(
                    period = "All Time",
                    totalTransactionCost = BigDecimal.ZERO,
                    incomeCategories = emptyList(),
                    expenseCategories = emptyList(),
                    othersInsightSummary = null
                )
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getAvailableWeeks(accountId: String?): Result<AvailableWeeks> = Result.Success(AvailableWeeks(emptyList()))

    override suspend fun getAvailableMonths(accountId: String?): Result<AvailableMonths> = Result.Success(AvailableMonths(emptyList()))

    override suspend fun getAvailableYears(accountId: String?): Result<AvailableYears> = Result.Success(AvailableYears(emptyList()))

    override suspend fun getOverviewSummary(accountId: String?): Result<OverviewSummary> = withContext(Dispatchers.IO) {
        try {
            Result.Success(
                OverviewSummary(
                    period = "All Time",
                    isCurrent = true,
                    weeklyOverview = emptyList(),
                    monthlyOverview = emptyList()
                )
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getCategoryComparisons(
        accountId: String?,
        period: String?
    ): Result<CategoryComparisonSummary> = Result.Success(
        CategoryComparisonSummary(
            period = period ?: "All Time",
            isCurrent = true,
            data = emptyList()
        )
    )

    override suspend fun getTransactionCounts(
        accountId: String,
        isIncome: Boolean?,
        categoryId: String?,
        start: String?,
        end: String?,
        hasTransactionCost: Boolean?
    ): Result<TransactionCountSummary> = withContext(Dispatchers.IO) {
        try {
            val count = queries.countTransactions(offlineUserId).executeAsOne()
            Result.Success(
                TransactionCountSummary(
                    totalIncomeTransactions = 0,
                    totalExpenseTransactions = 0,
                    totalTransactions = count.toInt(),
                    totalAmount = BigDecimal.ZERO,
                    totalTransactionCost = BigDecimal.ZERO
                )
            )
        } catch (e: Exception) {
            logger.error(TAG, "Error fetching transaction counts", e)
            Result.Error(e)
        }
    }

    override suspend fun getProfileMetrics(): Result<ProfileMetrics> = Result.Success(
        ProfileMetrics(
            name = "Offline User",
            email = "offline@fintrack.local",
            netWorth = BigDecimal.ZERO,
            savingsRate = null,
            essentialSpendRatio = null
        )
    )
}
