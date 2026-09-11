package com.fintrack.shared.feature.summary.data.repository

import com.fintrack.shared.db.FintrackDatabase
import com.fintrack.shared.db.GetDailyTotals
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.summary.domain.model.*
import com.fintrack.shared.feature.summary.domain.repository.SummaryRepository
import com.fintrack.shared.feature.core.logger.KMPLogger
import com.fintrack.shared.feature.core.util.DateTimeUtils
import com.fintrack.shared.feature.core.util.DateTimeHelper
import com.fintrack.shared.feature.user.domain.repository.UserRepository
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DatePeriod

class SummaryRepositoryOfflineImpl(
    database: FintrackDatabase,
    private val logger: KMPLogger,
    private val userRepository: UserRepository
) : SummaryRepository {

    private val queries = database.fintrackDatabaseQueries
    private val offlineUserId = "offline_user"
    private val tag = "SummaryRepo"

    // Default fallback category IDs (Salary and Food)
    private val defaultIncomeId = "aaaaaaaa-aaaa-4aaa-baaa-000000000001"
    private val defaultExpenseId = "00000000-0000-4000-a000-000000000001"

    override suspend fun getHighlightsSummary(
        accountId: String?,
        period: String?
    ): Result<StatisticsSummary> = withContext(Dispatchers.IO) {
        try {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val targetPeriod = period ?: run {
                val availableMonths = queries.selectDistinctMonths(offlineUserId, accountId).executeAsList()
                availableMonths.firstOrNull() ?: DateTimeHelper.currentMonthCode()
            }
            
            val range = if (targetPeriod.contains("-W")) {
                DateTimeUtils.getIsoWeekRange(targetPeriod)
            } else if (targetPeriod.length == 7) {
                DateTimeUtils.getMonthRange(targetPeriod)
            } else if (targetPeriod.length == 4) {
                val year = targetPeriod.toIntOrNull() ?: now.year
                LocalDate(year, 1, 1) to LocalDate(year, 12, 31)
            } else null

            val (incomeTotal, expenseTotal, feesTotal) = if (range != null) {
                val r = queries.getHighlightsByRange(
                    accountId = accountId,
                    userId = offlineUserId,
                    start = range.first.atStartOfDayIn(TimeZone.currentSystemDefault()),
                    end = range.second.atTime(23, 59, 59).toInstant(TimeZone.currentSystemDefault())
                ).executeAsOne()
                Triple(r.incomeTotal, r.expenseTotal, r.feesTotal)
            } else {
                val r = queries.getHighlights(accountId = accountId, userId = offlineUserId).executeAsOne()
                Triple(r.incomeTotal, r.expenseTotal, r.feesTotal)
            }
            
            val income = BigDecimal.fromDouble(incomeTotal ?: 0.0)
            val totalExpense = BigDecimal.fromDouble(expenseTotal ?: 0.0)
            val fees = BigDecimal.fromDouble(feesTotal ?: 0.0)

            Result.Success(
                StatisticsSummary(
                    period = targetPeriod,
                    income = income,
                    expense = totalExpense,
                    balance = income - totalExpense,
                    totalTransactionCost = fees
                )
            )
        } catch (e: Exception) {
            logger.error(tag, "Error calculating highlights", e)
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
            val isIncomeValue = if (type?.lowercase() == "income") 1L else if (type?.lowercase() == "expense") 0L else null
            
            val startInstant = start?.let { Instant.parse(it) } ?: Instant.fromEpochSeconds(0)
            val endInstant = end?.let { Instant.parse(it) } ?: Clock.System.now()

            val categoryTotals = queries.getCategoryTotals(
                userId = offlineUserId,
                accountId = accountId,
                isIncome = isIncomeValue,
                start = startInstant,
                end = endInstant
            ).executeAsList()

            // Calculate Momentum
            val currentMonthStart = if (weekOrMonthCode.length == 7) { 
                 try { LocalDate.parse("$weekOrMonthCode-01") } catch(_: Exception) { null }
            } else null
            
            val momentumData = if (currentMonthStart != null) {
                val m1Start = currentMonthStart.minus(DatePeriod(months = 1))
                val m1End = currentMonthStart.minus(DatePeriod(days = 1))
                val m2Start = currentMonthStart.minus(DatePeriod(months = 2))
                val m2End = m1Start.minus(DatePeriod(days = 1))

                val m1Totals = queries.getCategoryTotals(
                    userId = offlineUserId,
                    accountId = accountId,
                    isIncome = isIncomeValue,
                    start = m1Start.atStartOfDayIn(TimeZone.currentSystemDefault()),
                    end = m1End.atTime(23, 59, 59).toInstant(TimeZone.currentSystemDefault())
                ).executeAsList()

                val m2Totals = queries.getCategoryTotals(
                    userId = offlineUserId,
                    accountId = accountId,
                    isIncome = isIncomeValue,
                    start = m2Start.atStartOfDayIn(TimeZone.currentSystemDefault()),
                    end = m2End.atTime(23, 59, 59).toInstant(TimeZone.currentSystemDefault())
                ).executeAsList()
                
                m1Totals to m2Totals
            } else null

            val totalAmount = categoryTotals.fold(0.0) { acc, it -> acc + (it.totalSum ?: 0.0) }
            
            val categorySummaries = categoryTotals.map { current ->
                val amount = current.totalSum ?: 0.0
                val percentage = if (totalAmount > 0) (amount / totalAmount) * 100 else 0.0
                
                val momentum = if (momentumData != null) {
                    val prev1 = momentumData.first.find { it.catId == current.catId }?.totalSum ?: 0.0
                    val prev2 = momentumData.second.find { it.catId == current.catId }?.totalSum ?: 0.0
                    when {
                        amount > prev1 && prev1 > prev2 -> "UP"
                        amount < prev1 && prev1 < prev2 -> "DOWN"
                        else -> "STABLE"
                    }
                } else null

                CategorySummary(
                    category = current.catName,
                    categoryId = current.catId,
                    total = BigDecimal.fromDouble(amount),
                    percentage = BigDecimal.fromDouble(percentage),
                    transactionCount = current.txCount.toInt(),
                    momentumTrend = momentum
                )
            }

            Result.Success(
                DistributionSummary(
                    period = weekOrMonthCode,
                    totalTransactionCost = BigDecimal.ZERO,
                    incomeCategories = if (isIncomeValue == 1L) categorySummaries else emptyList(),
                    expenseCategories = if (isIncomeValue == 0L) categorySummaries else emptyList(),
                    othersInsightSummary = null
                )
            )
        } catch (e: Exception) {
            logger.error(tag, "Error calculating distribution", e)
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
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val availableMonths = queries.selectDistinctMonths(offlineUserId, accountId).executeAsList()
            
            @Suppress("DEPRECATION")
            val currentMonthCode = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}"
            val targetMonthCode = availableMonths.firstOrNull() ?: currentMonthCode
            
            val isCurrent = targetMonthCode == currentMonthCode
            
            val monthStart = try { LocalDate.parse("$targetMonthCode-01") } catch(_: Exception) { now }
            val monthEnd = monthStart.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))

            val trendEnd = if (isCurrent) now else monthEnd
            val weeklyStart = trendEnd.minus(DatePeriod(days = 6))
            val monthlyStart = trendEnd.minus(DatePeriod(days = 29))

            val weeklyData = queries.getDailyTotals(
                userId = offlineUserId,
                accountId = accountId,
                start = weeklyStart.atStartOfDayIn(TimeZone.currentSystemDefault()),
                end = trendEnd.atTime(23, 59, 59).toInstant(TimeZone.currentSystemDefault())
            ).executeAsList()

            val monthlyData = queries.getDailyTotals(
                userId = offlineUserId,
                accountId = accountId,
                start = monthlyStart.atStartOfDayIn(TimeZone.currentSystemDefault()),
                end = trendEnd.atTime(23, 59, 59).toInstant(TimeZone.currentSystemDefault())
            ).executeAsList()

            fun fillDates(start: LocalDate, end: LocalDate, data: List<GetDailyTotals>): List<DaySummary> {
                val dataMap = data.associateBy { it.day }
                val results = mutableListOf<DaySummary>()
                var current = start
                while (current <= end) {
                    val dateStr = current.toString()
                    val row = dataMap[dateStr]
                    results.add(DaySummary(
                        date = dateStr,
                        income = BigDecimal.fromDouble(row?.incomeTotal ?: 0.0),
                        expense = BigDecimal.fromDouble(row?.expenseTotal ?: 0.0)
                    ))
                    current = current.plus(DatePeriod(days = 1))
                }
                return results
            }

            Result.Success(
                OverviewSummary(
                    period = targetMonthCode,
                    isCurrent = isCurrent,
                    weeklyOverview = fillDates(weeklyStart, trendEnd, weeklyData),
                    monthlyOverview = fillDates(monthlyStart, trendEnd, monthlyData)
                )
            )
        } catch (e: Exception) {
            logger.error(tag, "Error calculating overview", e)
            Result.Error(e)
        }
    }

    override suspend fun getCategoryComparisons(
        accountId: String?,
        period: String?
    ): Result<CategoryComparisonSummary> = withContext(Dispatchers.IO) {
        try {
            val availableMonths = queries.selectDistinctMonths(offlineUserId, accountId).executeAsList()
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            @Suppress("DEPRECATION")
            val currentMonthCode = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}"

            val targetPeriodString = period ?: availableMonths.firstOrNull() ?: currentMonthCode
            val isCurrent = targetPeriodString == currentMonthCode

            val dateRange = DateTimeUtils.getMonthRange(targetPeriodString) ?: return@withContext Result.Success(CategoryComparisonSummary(targetPeriodString, true, emptyList()))

            val (currentMonthStart, currentMonthEnd) = dateRange

            val user = userRepository.getUserProfile().first()
            val trackedIds = user?.trackedCategoryIds ?: emptyList()

            val currentMonthTotals = queries.getCategoryTotals(
                userId = offlineUserId,
                accountId = accountId,
                isIncome = null,
                start = currentMonthStart.atStartOfDayIn(TimeZone.currentSystemDefault()),
                end = currentMonthEnd.atTime(23, 59, 59).toInstant(TimeZone.currentSystemDefault())
            ).executeAsList()

            val previousMonthStart = currentMonthStart.minus(DatePeriod(months = 1))
            val previousMonthEnd = currentMonthStart.minus(DatePeriod(days = 1))
            
            val previousMonthTotals = queries.getCategoryTotals(
                userId = offlineUserId,
                accountId = accountId,
                isIncome = null,
                start = previousMonthStart.atStartOfDayIn(TimeZone.currentSystemDefault()),
                end = previousMonthEnd.atTime(23, 59, 59).toInstant(TimeZone.currentSystemDefault())
            ).executeAsList()

            val thisWeekEnd = if (isCurrent) now else currentMonthEnd
            val thisWeekStart = thisWeekEnd.minus(DatePeriod(days = 6))
            val lastWeekEnd = thisWeekStart.minus(DatePeriod(days = 1))
            val lastWeekStart = lastWeekEnd.minus(DatePeriod(days = 6))

            val thisWeekTotals = queries.getCategoryTotals(
                userId = offlineUserId,
                accountId = accountId,
                isIncome = null,
                start = thisWeekStart.atStartOfDayIn(TimeZone.currentSystemDefault()),
                end = thisWeekEnd.atTime(23, 59, 59).toInstant(TimeZone.currentSystemDefault())
            ).executeAsList()

            val lastWeekTotals = queries.getCategoryTotals(
                userId = offlineUserId,
                accountId = accountId,
                isIncome = null,
                start = lastWeekStart.atStartOfDayIn(TimeZone.currentSystemDefault()),
                end = lastWeekEnd.atTime(23, 59, 59).toInstant(TimeZone.currentSystemDefault())
            ).executeAsList()

            val finalTargetIds = mutableListOf<String>()
            if (trackedIds.isNotEmpty()) {
                finalTargetIds.addAll(trackedIds.take(2))
                if (finalTargetIds.size < 2) {
                    val firstId = finalTargetIds.first()
                    val firstIsIncome = queries.selectCategoryById(firstId).executeAsOneOrNull()?.isExpense == 0L
                    val padId = if (firstIsIncome) defaultExpenseId else defaultIncomeId
                    finalTargetIds.add(padId)
                }
            } else {
                val topIncome = currentMonthTotals
                    .filter { queries.selectCategoryById(it.catId).executeAsOneOrNull()?.isExpense == 0L }
                    .maxByOrNull { it.totalSum ?: 0.0 }
                
                val topExpense = currentMonthTotals
                    .filter { queries.selectCategoryById(it.catId).executeAsOneOrNull()?.isExpense != 0L }
                    .filter { it.catName != "Transaction Fees" && it.catName != "Transaction Cost" }
                    .maxByOrNull { it.totalSum ?: 0.0 }

                finalTargetIds.add(topIncome?.catId ?: defaultIncomeId)
                finalTargetIds.add(topExpense?.catId ?: defaultExpenseId)
            }

            val comparisons = finalTargetIds.map { id ->
                val current = currentMonthTotals.find { it.catId == id }
                val prev = previousMonthTotals.find { it.catId == id }
                val weekCur = thisWeekTotals.find { it.catId == id }
                val weekPrev = lastWeekTotals.find { it.catId == id }

                val curVal = current?.totalSum ?: 0.0
                val prevVal = prev?.totalSum ?: 0.0
                val wCurVal = weekCur?.totalSum ?: 0.0
                val wPrevVal = weekPrev?.totalSum ?: 0.0

                val categoryName = current?.catName ?: queries.selectCategoryById(id).executeAsOneOrNull()?.name ?: "Unknown"

                CategoryComparison(
                    category = categoryName,
                    currentTotal = BigDecimal.fromDouble(curVal),
                    previousTotal = BigDecimal.fromDouble(prevVal),
                    changePercentage = calculatePercentageChange(curVal, prevVal),
                    isIncome = queries.selectCategoryById(id).executeAsOneOrNull()?.isExpense == 0L,
                    period = targetPeriodString,
                    weeklyCurrentTotal = BigDecimal.fromDouble(wCurVal),
                    weeklyChangePercentage = calculatePercentageChange(wCurVal, wPrevVal)
                )
            }

            Result.Success(
                CategoryComparisonSummary(
                    period = targetPeriodString,
                    isCurrent = isCurrent,
                    data = comparisons
                )
            )
        } catch (e: Exception) {
            logger.error(tag, "Error calculating category comparisons", e)
            Result.Error(e)
        }
    }

    private fun calculatePercentageChange(current: Double, previous: Double): BigDecimal {
        if (previous == 0.0) {
            return if (current > 0) BigDecimal.fromInt(100) else BigDecimal.ZERO
        }
        return BigDecimal.fromDouble(((current - previous) / previous) * 100)
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
            val startInstant = start?.let { try { Instant.parse(it) } catch(_: Exception) { null } }
            val endInstant = end?.let { try { Instant.parse(it) } catch(_: Exception) { null } }

            val allTransactions = queries.selectAllTransactions(offlineUserId).executeAsList()
            
            val filtered = allTransactions.filter { tx ->
                val matchesAccount = tx.accountId == accountId
                val matchesCategory = categoryId == null || tx.categoryId == categoryId
                val matchesIncome = isIncome == null || (tx.isIncome != 0L) == isIncome
                val matchesStart = startInstant == null || tx.dateTime >= startInstant
                val matchesEnd = endInstant == null || tx.dateTime <= endInstant
                val matchesCost = hasTransactionCost == null || (BigDecimal.parseString(tx.transactionCost) > BigDecimal.ZERO) == hasTransactionCost
                
                matchesAccount && matchesCategory && matchesIncome && matchesStart && matchesEnd && matchesCost
            }

            val incomeTxs = filtered.filter { it.isIncome != 0L }
            val expenseTxs = filtered.filter { it.isIncome == 0L }

            val totalAmount = filtered.fold(BigDecimal.ZERO) { acc, tx ->
                val amount = BigDecimal.parseString(tx.amount)
                val cost = BigDecimal.parseString(tx.transactionCost)
                acc + if (tx.isIncome != 0L) (amount - cost) else (amount + cost)
            }

            val totalCost = filtered.fold(BigDecimal.ZERO) { acc, tx ->
                acc + BigDecimal.parseString(tx.transactionCost)
            }

            Result.Success(
                TransactionCountSummary(
                    totalIncomeTransactions = incomeTxs.size,
                    totalExpenseTransactions = expenseTxs.size,
                    totalTransactions = filtered.size,
                    totalAmount = totalAmount,
                    totalTransactionCost = totalCost
                )
            )
        } catch (e: Exception) {
            logger.error(tag, "Error fetching transaction counts", e)
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
            logger.error(tag, "Error fetching profile metrics", e)
            Result.Error(e)
        }
    }
}
