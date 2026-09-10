package com.fintrack.shared.feature.summary.data.repository

import com.fintrack.shared.db.FintrackDatabase
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.summary.domain.model.*
import com.fintrack.shared.feature.summary.domain.repository.SummaryRepository
import com.fintrack.shared.feature.core.logger.KMPLogger
import com.fintrack.shared.feature.core.util.DateTimeUtils
import com.fintrack.shared.feature.user.domain.repository.UserRepository
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import kotlin.time.Clock

class SummaryRepositoryOfflineImpl(
    private val database: FintrackDatabase,
    private val logger: KMPLogger,
    private val userRepository: UserRepository
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
            
            val income = BigDecimal.fromDouble(result.incomeTotal ?: 0.0)
            val expense = BigDecimal.fromDouble(result.expenseTotal ?: 0.0)

            Result.Success(
                StatisticsSummary(
                    period = period ?: "All Time",
                    income = income,
                    expense = expense,
                    balance = income - expense,
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
            val isIncome = type?.lowercase() == "income"
            
            val startInstant = start?.let { Instant.parse(it) } ?: Instant.fromEpochSeconds(0)
            val endInstant = end?.let { Instant.parse(it) } ?: (Clock.System.now() as Instant)

            val categoryTotals = queries.getCategoryTotals(
                userId = offlineUserId,
                accountId = accountId,
                isIncome = if (isIncome) 1L else 0L,
                start = startInstant,
                end = endInstant
            ).executeAsList()

            val totalAmount = categoryTotals.fold(0.0) { acc, it -> acc + (it.totalSum ?: 0.0) }
            
            val categorySummaries = categoryTotals.map { 
                val amount = it.totalSum ?: 0.0
                val percentage = if (totalAmount > 0) (amount / totalAmount) * 100 else 0.0
                CategorySummary(
                    category = it.catName,
                    categoryId = it.catId,
                    total = BigDecimal.fromDouble(amount),
                    percentage = BigDecimal.fromDouble(percentage),
                    transactionCount = it.txCount.toInt()
                )
            }

            Result.Success(
                DistributionSummary(
                    period = weekOrMonthCode,
                    totalTransactionCost = BigDecimal.ZERO,
                    incomeCategories = if (isIncome) categorySummaries else emptyList(),
                    expenseCategories = if (!isIncome) categorySummaries else emptyList(),
                    othersInsightSummary = null
                )
            )
        } catch (e: Exception) {
            logger.error(TAG, "Error calculating distribution", e)
            Result.Error(e)
        }
    }

    override suspend fun getAvailableWeeks(accountId: String?): Result<AvailableWeeks> = withContext(Dispatchers.IO) {
        try {
            val weeks = queries.selectDistinctWeeks(offlineUserId, accountId).executeAsList()
            Result.Success(AvailableWeeks(weeks))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getAvailableMonths(accountId: String?): Result<AvailableMonths> = withContext(Dispatchers.IO) {
        try {
            val months = queries.selectDistinctMonths(offlineUserId, accountId).executeAsList()
            Result.Success(AvailableMonths(months))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getAvailableYears(accountId: String?): Result<AvailableYears> = withContext(Dispatchers.IO) {
        try {
            val years = queries.selectDistinctYears(offlineUserId, accountId).executeAsList()
            Result.Success(AvailableYears(years))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getOverviewSummary(accountId: String?): Result<OverviewSummary> = withContext(Dispatchers.IO) {
        try {
            val now = Clock.System.now() as Instant
            val weekAgo = now.minus(7, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            val monthAgo = now.minus(30, DateTimeUnit.DAY, TimeZone.currentSystemDefault())

            val weeklyData = queries.getDailyTotals(
                userId = offlineUserId,
                accountId = accountId,
                start = weekAgo,
                end = now
            ).executeAsList().map {
                DaySummary(
                    date = it.day,
                    income = BigDecimal.fromDouble(it.incomeTotal ?: 0.0),
                    expense = BigDecimal.fromDouble(it.expenseTotal ?: 0.0)
                )
            }

            val monthlyData = queries.getDailyTotals(
                userId = offlineUserId,
                accountId = accountId,
                start = monthAgo,
                end = now
            ).executeAsList().map {
                DaySummary(
                    date = it.day,
                    income = BigDecimal.fromDouble(it.incomeTotal ?: 0.0),
                    expense = BigDecimal.fromDouble(it.expenseTotal ?: 0.0)
                )
            }

            Result.Success(
                OverviewSummary(
                    period = "Last 30 Days",
                    isCurrent = true,
                    weeklyOverview = weeklyData,
                    monthlyOverview = monthlyData
                )
            )
        } catch (e: Exception) {
            logger.error(TAG, "Error calculating overview", e)
            Result.Error(e)
        }
    }

    override suspend fun getCategoryComparisons(
        accountId: String?,
        period: String?
    ): Result<CategoryComparisonSummary> = withContext(Dispatchers.IO) {
        try {
            val dateRange = if (period != null) DateTimeUtils.getMonthRange(period) else null
            
            if (dateRange == null) {
                return@withContext Result.Success(CategoryComparisonSummary(period ?: "All Time", true, emptyList()))
            }

            // Get tracked categories from user settings
            val user = userRepository.getUserProfile().first()
            val trackedIds = user?.trackedCategoryIds ?: emptyList()

            // Current Period Category Totals
            val currentTotals = queries.getCategoryTotals(
                userId = offlineUserId,
                accountId = accountId,
                isIncome = 0L,
                start = dateRange.first.atStartOfDayIn(TimeZone.currentSystemDefault()),
                end = dateRange.second.atTime(23, 59, 59).toInstant(TimeZone.currentSystemDefault())
            ).executeAsList()

            // Previous Period
            val prevMonthStart = dateRange.first.minus(1, DateTimeUnit.MONTH)
            val prevMonthEnd = dateRange.first.minus(1, DateTimeUnit.DAY)
            
            val previousTotals = queries.getCategoryTotals(
                userId = offlineUserId,
                accountId = accountId,
                isIncome = 0L,
                start = prevMonthStart.atStartOfDayIn(TimeZone.currentSystemDefault()),
                end = prevMonthEnd.atTime(23, 59, 59).toInstant(TimeZone.currentSystemDefault())
            ).executeAsList()

            val comparisons = currentTotals
                .filter { row -> 
                    // Filter based on user settings OR just take top ones if none selected
                    trackedIds.isEmpty() || trackedIds.contains(row.catId) 
                }
                .map { current ->
                    val prev = previousTotals.find { it.catId == current.catId }
                    val currentVal = current.totalSum ?: 0.0
                    val prevVal = prev?.totalSum ?: 0.0
                    
                    val change = if (prevVal > 0) ((currentVal - prevVal) / prevVal) * 100 else 0.0
                    
                    CategoryComparison(
                        category = current.catName,
                        currentTotal = BigDecimal.fromDouble(currentVal),
                        previousTotal = BigDecimal.fromDouble(prevVal),
                        changePercentage = BigDecimal.fromDouble(change)
                    )
                }
                .take(2) // Limit to 2 as per backend behavior

            Result.Success(
                CategoryComparisonSummary(
                    period = period ?: "",
                    isCurrent = true,
                    data = comparisons
                )
            )
        } catch (e: Exception) {
            logger.error(TAG, "Error calculating category comparisons", e)
            Result.Error(e)
        }
    }

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

    override suspend fun getProfileMetrics(): Result<ProfileMetrics> = withContext(Dispatchers.IO) {
        try {
            val netWorthResult = queries.getNetWorth(offlineUserId).executeAsOne()
            val netWorthDouble = (netWorthResult.netWorth as? Number)?.toDouble() ?: 0.0
            
            val highlights = queries.getHighlights(accountId = null, userId = offlineUserId).executeAsOne()
            val income = highlights.incomeTotal ?: 0.0
            val expense = highlights.expenseTotal ?: 0.0
            
            val savingsRate = if (income > 0) ((income - expense) / income) * 100 else 0.0

            Result.Success(
                ProfileMetrics(
                    name = "Offline User",
                    email = "offline@fintrack.local",
                    netWorth = BigDecimal.fromDouble(netWorthDouble),
                    savingsRate = BigDecimal.fromDouble(savingsRate),
                    essentialSpendRatio = null 
                )
            )
        } catch (e: Exception) {
            logger.error(TAG, "Error fetching profile metrics", e)
            Result.Error(e)
        }
    }
}
