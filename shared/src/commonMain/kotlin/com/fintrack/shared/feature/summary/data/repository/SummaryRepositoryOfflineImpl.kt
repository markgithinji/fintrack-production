package com.fintrack.shared.feature.summary.data.repository

import com.fintrack.shared.db.FintrackDatabase
import com.fintrack.shared.db.GetCategoryTotals
import com.fintrack.shared.db.GetDailyTotals
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.summary.domain.model.*
import com.fintrack.shared.feature.summary.domain.repository.SummaryRepository
import com.fintrack.shared.feature.core.logger.KMPLogger
import com.fintrack.shared.feature.core.util.DateTimeUtils
import com.fintrack.shared.feature.core.util.DateTimeHelper
import com.fintrack.shared.feature.summary.util.MerchantInsightUtils
import com.fintrack.shared.feature.user.domain.repository.UserRepository
import com.fintrack.shared.feature.budget.domain.repository.BudgetRepository
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.DecimalMode
import com.ionspin.kotlin.bignum.decimal.RoundingMode
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
    private val userRepository: UserRepository,
    private val budgetRepository: BudgetRepository
) : SummaryRepository {

    private val queries = database.fintrackDatabaseQueries
    private val offlineUserId = "offline_user"
    private val tag = "SummaryRepo"

    /**
     * Set of category names considered "Essential" for financial health benchmarks (Needs vs Wants).
     */
    private val essentialCategories = setOf(
        "Rent", "Groceries", "Transport", "Bills", "Health", "Education", "Utilities", "Insurance"
    )

    // Default fallback category IDs (Salary and Food)
    private val defaultIncomeId = "aaaaaaaa-aaaa-4aaa-baaa-000000000001"
    private val defaultExpenseId = "00000000-0000-4000-a000-000000000001"

    private val ratioMode = DecimalMode(decimalPrecision = 20, roundingMode = RoundingMode.ROUND_HALF_AWAY_FROM_ZERO)

    override suspend fun getHighlightsSummary(
        accountId: String?,
        period: String?
    ): Result<StatisticsSummary> = withContext(Dispatchers.IO) {
        try {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val targetPeriod = period ?: DateTimeHelper.currentMonthCode()
            
            val range = if (targetPeriod.contains("-W")) {
                DateTimeUtils.getIsoWeekRange(targetPeriod)
            } else if (targetPeriod.length == 7) {
                DateTimeUtils.getMonthRange(targetPeriod)
            } else if (targetPeriod.length == 4) {
                val year = targetPeriod.toIntOrNull() ?: now.year
                LocalDate(year, 1, 1) to LocalDate(year, 12, 31)
            } else null

            val startInstant = range?.first?.atStartOfDayIn(TimeZone.currentSystemDefault()) ?: Instant.fromEpochSeconds(0)
            val endInstant = range?.second?.atTime(23, 59, 59)?.toInstant(TimeZone.currentSystemDefault()) ?: Clock.System.now()

            val highlightsResult = queries.getHighlightsByRange(
                accountId = accountId,
                userId = offlineUserId,
                start = startInstant,
                end = endInstant
            ).executeAsOne()
            
            // Unified fee handling logic from backend
            val income = BigDecimal.fromDouble(highlightsResult.incomeTotal ?: 0.0)
            val fees = BigDecimal.fromDouble(highlightsResult.feesTotal ?: 0.0)
            
            val expenseCategoryTotals = queries.getCategoryTotals(
                userId = offlineUserId,
                accountId = accountId,
                isIncome = 0L,
                start = startInstant,
                end = endInstant
            ).executeAsList()

            val feeAmountFromCategory = expenseCategoryTotals
                .find { it.catName.trim().equals("Transaction Fees", ignoreCase = true) || it.catName.trim().equals("Transaction Cost", ignoreCase = true) }
                ?.totalSum ?: 0.0

            // totalExpense = Sum of all expense categories - Fee category + sum of all fee fields
            val totalExpenseRaw = expenseCategoryTotals.sumOf { it.totalSum ?: 0.0 }
            val totalExpense = BigDecimal.fromDouble(totalExpenseRaw - feeAmountFromCategory) + fees

            // Calculate ratios (Multiply by 100 for percentage)
            val savingsRate = if (income > BigDecimal.ZERO) {
                (income - totalExpense).divide(income, ratioMode).multiply(BigDecimal.fromInt(100))
            } else null

            val essentialSpend = expenseCategoryTotals
                .filter { cat -> essentialCategories.any { it.equals(cat.catName.trim(), ignoreCase = true) } }
                .sumOf { it.totalSum ?: 0.0 }
            
            val essentialSpendRatio = if (totalExpense > BigDecimal.ZERO) {
                BigDecimal.fromDouble(essentialSpend).divide(totalExpense, ratioMode).multiply(BigDecimal.fromInt(100))
            } else null

            // Annual Forecast logic from backend
            val currentYear = now.year
            val isCurrentYearView = targetPeriod.startsWith(currentYear.toString())
            
            val ytdStart = LocalDate(currentYear, 1, 1).atStartOfDayIn(TimeZone.currentSystemDefault())
            val ytdEnd = Clock.System.now()
            
            val ytdHighlights = queries.getHighlightsByRange(
                accountId = accountId,
                userId = offlineUserId,
                start = ytdStart,
                end = ytdEnd
            ).executeAsOne()
            
            val ytdIncome = BigDecimal.fromDouble(ytdHighlights.incomeTotal ?: 0.0)
            val ytdFees = BigDecimal.fromDouble(ytdHighlights.feesTotal ?: 0.0)
            
            val ytdExpenseCategoryTotals = queries.getCategoryTotals(
                userId = offlineUserId,
                accountId = accountId,
                isIncome = 0L,
                start = ytdStart,
                end = ytdEnd
            ).executeAsList()
            
            val ytdFeeAmountFromCategory = ytdExpenseCategoryTotals
                .find { it.catName.trim().equals("Transaction Fees", ignoreCase = true) || it.catName.trim().equals("Transaction Cost", ignoreCase = true) }
                ?.totalSum ?: 0.0
                
            val ytdTotalExpense = BigDecimal.fromDouble(ytdExpenseCategoryTotals.sumOf { it.totalSum ?: 0.0 } - ytdFeeAmountFromCategory) + ytdFees

            @Suppress("DEPRECATION")
            val monthCount = now.monthNumber.coerceAtLeast(1)
            
            val projectedIncome = if (isCurrentYearView) {
                ytdIncome.multiply(BigDecimal.fromInt(12)).divide(BigDecimal.fromInt(monthCount), ratioMode)
            } else null
            
            val projectedExpense = if (isCurrentYearView) {
                ytdTotalExpense.multiply(BigDecimal.fromInt(12)).divide(BigDecimal.fromInt(monthCount), ratioMode)
            } else null

            // Volatility Tracking: Get data from previous year
            val prevYearStart = LocalDate(currentYear - 1, 1, 1).atStartOfDayIn(TimeZone.currentSystemDefault())
            val prevYearEnd = LocalDate(currentYear - 1, 12, 31).atTime(23, 59, 59).toInstant(TimeZone.currentSystemDefault())
            
            val prevYearCategoryTotals = queries.getCategoryTotals(
                userId = offlineUserId,
                accountId = accountId,
                isIncome = null,
                start = prevYearStart,
                end = prevYearEnd
            ).executeAsList()

            // Get daily data for peaks
            val dailyData = queries.getDailyTotals(
                userId = offlineUserId,
                accountId = accountId,
                start = startInstant,
                end = endInstant
            ).executeAsList()

            val incomeCategoryTotals = queries.getCategoryTotals(
                userId = offlineUserId,
                accountId = accountId,
                isIncome = 1L,
                start = startInstant,
                end = endInstant
            ).executeAsList()

            fun calculateAveragePerDay(data: List<GetDailyTotals>, isIncome: Boolean): BigDecimal {
                val days = data.size.coerceAtLeast(1)
                val total = data.fold(0.0) { acc, d -> acc + (if (isIncome) (d.incomeTotal ?: 0.0) else (d.expenseTotal ?: 0.0)) }
                return BigDecimal.fromDouble(total / days)
            }

            fun calculateHighestDay(data: List<GetDailyTotals>, isIncome: Boolean): Highlight? {
                return data.maxByOrNull { if (isIncome) (it.incomeTotal ?: 0.0) else (it.expenseTotal ?: 0.0) }
                    ?.let {
                        Highlight(
                            label = it.day,
                            value = it.day,
                            amount = BigDecimal.fromDouble(if (isIncome) (it.incomeTotal ?: 0.0) else (it.expenseTotal ?: 0.0))
                        )
                    }
            }

            fun calculateHighestCategory(data: List<GetCategoryTotals>, prevTotals: List<GetCategoryTotals>): Highlight? {
                return data.maxByOrNull { it.totalSum ?: 0.0 }
                    ?.let { current ->
                        val curAmount = BigDecimal.fromDouble(current.totalSum ?: 0.0)
                        val prevMatch = prevTotals.find { it.catId == current.catId }
                        val volatility = if (prevMatch != null && (prevMatch.totalSum ?: 0.0) > 0) {
                            val prevAmount = BigDecimal.fromDouble(prevMatch.totalSum ?: 0.0)
                            (curAmount - prevAmount).divide(prevAmount, ratioMode).multiply(BigDecimal.fromInt(100))
                        } else null

                        Highlight(
                            label = current.catName,
                            value = current.catName,
                            amount = curAmount,
                            volatilityPercentage = volatility
                        )
                    }
            }

            fun calculateHighestMonth(data: List<GetDailyTotals>, isIncome: Boolean): Highlight? {
                return data.groupBy { it.day.substring(0, 7) }
                    .mapValues { (_, entries) ->
                        entries.fold(0.0) { acc, entry ->
                            acc + (if (isIncome) (entry.incomeTotal ?: 0.0) else (entry.expenseTotal ?: 0.0))
                        }
                    }
                    .maxByOrNull { it.value }
                    ?.let { (month, amount) ->
                        Highlight(
                            label = month,
                            value = month,
                            amount = BigDecimal.fromDouble(amount)
                        )
                    }
            }

            val isYearMode = targetPeriod.length == 4
            
            // Risk Forecast: Budget "Runway"
            val projectedExceedMonth = if (isYearMode && projectedExpense != null) {
                calculateProjectedExceedMonth(accountId, totalExpense, projectedExpense)
            } else null

            // Correlations: Smart Insights
            val correlations = calculateCorrelations(accountId, targetPeriod)
            val isIncomeType = if (targetPeriod.contains("-W")) null else true // Placeholder or derive from context if possible

            Result.Success(
                StatisticsSummary(
                    period = targetPeriod,
                    income = income,
                    expense = totalExpense,
                    balance = income - totalExpense,
                    totalTransactionCost = fees,
                    incomeHighlights = Highlights(
                        highestMonth = if (isYearMode) calculateHighestMonth(dailyData, true) else null,
                        highestCategory = calculateHighestCategory(incomeCategoryTotals, prevYearCategoryTotals),
                        highestDay = calculateHighestDay(dailyData, true),
                        averagePerDay = calculateAveragePerDay(dailyData, true),
                        savingsRate = savingsRate,
                        projectedTotal = projectedIncome,
                        correlations = correlations // Show in both for now or filter if we have type
                    ),
                    expenseHighlights = Highlights(
                        highestMonth = if (isYearMode) calculateHighestMonth(dailyData, false) else null,
                        highestCategory = calculateHighestCategory(expenseCategoryTotals, prevYearCategoryTotals),
                        highestDay = calculateHighestDay(dailyData, false),
                        averagePerDay = calculateAveragePerDay(dailyData, false),
                        essentialSpendRatio = essentialSpendRatio,
                        projectedTotal = projectedExpense,
                        projectedExceedMonth = projectedExceedMonth,
                        correlations = correlations
                    )
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
            
            // 1. Determine Date Range
            val range = if (weekOrMonthCode.contains("-W")) {
                DateTimeUtils.getIsoWeekRange(weekOrMonthCode)
            } else if (weekOrMonthCode.length == 7) {
                DateTimeUtils.getMonthRange(weekOrMonthCode)
            } else if (weekOrMonthCode.length == 4) {
                val year = weekOrMonthCode.toIntOrNull() ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
                LocalDate(year, 1, 1) to LocalDate(year, 12, 31)
            } else null

            val startInstant = start?.let { Instant.parse(it) } 
                ?: range?.first?.atStartOfDayIn(TimeZone.currentSystemDefault()) 
                ?: Instant.fromEpochSeconds(0)
                
            val endInstant = end?.let { Instant.parse(it) } 
                ?: range?.second?.atTime(23, 59, 59)?.toInstant(TimeZone.currentSystemDefault()) 
                ?: Clock.System.now()

            // 2. Get Top-Level Fees for the period (Backend Parity)
            val highlights = queries.getHighlightsByRange(
                userId = offlineUserId,
                accountId = accountId,
                start = startInstant,
                end = endInstant
            ).executeAsOne()
            val totalFees = BigDecimal.fromDouble(highlights.feesTotal ?: 0.0)

            // 3. Get Category Totals
            val categoryTotalsRaw = queries.getCategoryTotals(
                userId = offlineUserId,
                accountId = accountId,
                isIncome = isIncomeValue,
                start = startInstant,
                end = endInstant
            ).executeAsList()

            // 4. Calculate Refined Total for Percentages
            val feeCategoryNames = setOf("Transaction Fees", "Transaction Cost")
            val feeCategoryTotal = categoryTotalsRaw
                .find { feeCategoryNames.any { name -> it.catName.trim().equals(name, ignoreCase = true) } }
                ?.totalSum ?: 0.0

            val rawSum = categoryTotalsRaw.sumOf { it.totalSum ?: 0.0 }
            val refinedTotal = if (isIncomeValue == 0L) {
                // Expense logic: Total = Sum - Fee Category + Actual Fees
                BigDecimal.fromDouble(rawSum - feeCategoryTotal) + totalFees
            } else {
                BigDecimal.fromDouble(rawSum)
            }

            // 5. Historical Averages (Usually X times)
            val historicalAverageCounts = if (range != null) {
                val histStart = range.first.minus(DatePeriod(months = 6)).atStartOfDayIn(TimeZone.currentSystemDefault())
                val histEnd = range.first.minus(DatePeriod(days = 1)).atTime(23, 59, 59).toInstant(TimeZone.currentSystemDefault())
                
                queries.getCategoryTotals(offlineUserId, accountId, isIncomeValue, histStart, histEnd).executeAsList()
                    .associate { it.catId to BigDecimal.fromDouble(it.txCount.toDouble() / 6.0) }
            } else emptyMap()

            // 6. Merchant Insights (Mainly X, Y)
            val allTransactionsForPeriod = queries.selectAllTransactions(offlineUserId).executeAsList()
                .filter { it.dateTime >= startInstant && it.dateTime <= endInstant && (accountId == null || it.accountId == accountId) }
            
            val merchantInsights = allTransactionsForPeriod
                .filter { tx -> (isIncomeValue == null || tx.isIncome == isIncomeValue) }
                .groupBy { it.categoryId }
                .mapValues { (_, txs) ->
                    val catName = txs.firstOrNull()?.let { queries.selectCategoryById(it.categoryId).executeAsOneOrNull()?.name } ?: "Unknown"
                    txs.mapNotNull { it.description }
                        .filter { MerchantInsightUtils.isDescriptionMeaningful(it, catName) }
                        .map { MerchantInsightUtils.cleanMerchantName(it) }
                        .groupBy { it }
                        .mapValues { it.value.size }
                        .entries.sortedByDescending { it.value }
                        .take(3)
                        .map { it.key }
                }

            // 7. Calculate Momentum (Backend Logic)
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

            // 8. Map to Domain Models (Filtering out Fee category from the list)
            val categorySummaries = categoryTotalsRaw
                .filterNot { feeCategoryNames.any { name -> it.catName.trim().equals(name, ignoreCase = true) } }
                .map { current ->
                    val amount = BigDecimal.fromDouble(current.totalSum ?: 0.0)
                    val percentage = if (refinedTotal > BigDecimal.ZERO) {
                        amount.divide(refinedTotal, ratioMode).multiply(BigDecimal.fromInt(100))
                    } else BigDecimal.ZERO
                    
                    val momentum = if (momentumData != null) {
                        val prev1 = momentumData.first.find { it.catId == current.catId }?.totalSum ?: 0.0
                        val prev2 = momentumData.second.find { it.catId == current.catId }?.totalSum ?: 0.0
                        val curVal = current.totalSum ?: 0.0
                        when {
                            curVal > prev1 && prev1 > prev2 -> "UP"
                            curVal < prev1 && prev1 < prev2 -> "DOWN"
                            else -> "STABLE"
                        }
                    } else null

                    CategorySummary(
                        category = current.catName,
                        categoryId = current.catId,
                        total = amount,
                        percentage = percentage,
                        transactionCount = current.txCount.toInt(),
                        averageTransactionCount = historicalAverageCounts[current.catId],
                        momentumTrend = momentum,
                        topDescriptionInsights = merchantInsights[current.catId]
                    )
                }

            Result.Success(
                DistributionSummary(
                    period = weekOrMonthCode,
                    totalTransactionCost = totalFees,
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

    private val monthNames = listOf(
        "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
        "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"
    )

    private suspend fun calculateProjectedExceedMonth(
        accountId: String?,
        totalExpense: BigDecimal,
        expenseProjectedTotal: BigDecimal
    ): String? {
        val result = budgetRepository.getBudgets(accountId)
        if (result !is Result.Success) return null
        
        val budgets = result.data.map { it.budget }.filter { it.isExpense }
        val totalMonthlyLimit = budgets.fold(BigDecimal.ZERO) { acc, b -> acc + b.limit }
        val yearlyLimit = totalMonthlyLimit.multiply(BigDecimal.fromInt(12))

        if (expenseProjectedTotal > yearlyLimit && yearlyLimit > BigDecimal.ZERO) {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val currentMonth = now.monthNumber
            val averagePerMonth = totalExpense.divide(BigDecimal.fromInt(currentMonth), ratioMode)

            for (m in (currentMonth + 1)..12) {
                val accumulatedSpend = totalExpense + averagePerMonth.multiply(BigDecimal.fromInt(m - currentMonth))
                if (accumulatedSpend > yearlyLimit) {
                    return monthNames.getOrNull(m - 1)
                }
            }
        }
        return null
    }

    private suspend fun calculateCorrelations(
        accountId: String?,
        targetPeriod: String
    ): List<Correlation> {
        if (targetPeriod.length != 7) return emptyList() // Only for month view
        
        try {
            val year = targetPeriod.split("-")[0].toInt()
            val month = targetPeriod.split("-")[1].toInt()
            val hCurrentStart = LocalDate(year, month, 1)
            val historicalStart = hCurrentStart.minus(DatePeriod(months = 6))
            val historicalEnd = hCurrentStart.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))

            // Get monthly category stats for the last 6 months
            val monthlyData = mutableMapOf<String, Pair<BigDecimal, Map<String, BigDecimal>>>()
            var current = historicalStart
            while (current <= historicalEnd) {
                val monthCode = "${current.year}-${current.monthNumber.toString().padStart(2, '0')}"
                val range = DateTimeUtils.getMonthRange(monthCode)
                if (range != null) {
                    val start = range.first.atStartOfDayIn(TimeZone.currentSystemDefault())
                    val end = range.second.atTime(23, 59, 59).toInstant(TimeZone.currentSystemDefault())
                    
                    val incomeTotals = queries.getCategoryTotals(offlineUserId, accountId, 1L, start, end).executeAsList()
                    val expenseTotals = queries.getCategoryTotals(offlineUserId, accountId, 0L, start, end).executeAsList()
                    
                    val totalIncome = BigDecimal.fromDouble(incomeTotals.sumOf { it.totalSum ?: 0.0 })
                    val expenseMap = expenseTotals.associate { (it.catName.lowercase()) to BigDecimal.fromDouble(it.totalSum ?: 0.0) }
                    
                    monthlyData[monthCode] = totalIncome to expenseMap
                }
                current = current.plus(DatePeriod(months = 1))
            }

            val targetCats = setOf("shopping", "entertainment", "dining out")
            val keys = monthlyData.keys.sorted().toList()
            val correlations = mutableListOf<Correlation>()
            
            for (i in 1 until keys.size) {
                val prevMonth = monthlyData[keys[i - 1]]!!
                val currMonth = monthlyData[keys[i]]!!
                val nextMonth = if (i + 1 < keys.size) monthlyData[keys[i + 1]] else null

                if (prevMonth.first > BigDecimal.ZERO) {
                    val incomeIncrease = (currMonth.first - prevMonth.first).divide(prevMonth.first, ratioMode).doubleValue(false)
                    if (incomeIncrease > 0.10) {
                        targetCats.forEach { cat ->
                            val prevExp = prevMonth.second[cat] ?: BigDecimal.ZERO
                            val currExp = currMonth.second[cat] ?: BigDecimal.ZERO
                            val nextExp = nextMonth?.second?.get(cat) ?: BigDecimal.ZERO

                            if (prevExp > BigDecimal.ZERO) {
                                val expIncrease = (currExp - prevExp).divide(prevExp, ratioMode).doubleValue(false)
                                if (expIncrease > 0.15) {
                                    val incPct = (incomeIncrease * 100).toInt()
                                    val expPct = (expIncrease * 100).toInt()
                                    correlations.add(
                                        Correlation(
                                            source = "Income",
                                            target = cat.replaceFirstChar { it.uppercase() },
                                            insight = "When your income increases by $incPct%, your '${cat.replaceFirstChar { it.uppercase() }}' spend tends to increase by $expPct% in the same month."
                                        )
                                    )
                                } else if (nextExp > BigDecimal.ZERO) {
                                    val nextExpIncrease = (nextExp - currExp).divide(currExp, ratioMode).doubleValue(false)
                                    if (nextExpIncrease > 0.15) {
                                        val incPct = (incomeIncrease * 100).toInt()
                                        val expPct = (nextExpIncrease * 100).toInt()
                                        correlations.add(
                                            Correlation(
                                                source = "Income",
                                                target = cat.replaceFirstChar { it.uppercase() },
                                                insight = "Following a $incPct% income increase, your '${cat.replaceFirstChar { it.uppercase() }}' spend tended to increase by $expPct% the next month."
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return correlations
        } catch (e: Exception) {
            return emptyList()
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

            val dateRange = DateTimeUtils.getMonthRange(targetPeriodString) ?: return@withContext Result.Success(CategoryComparisonSummary(period = targetPeriodString, isCurrent = true, data = emptyList()))

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
            val timeZone = TimeZone.currentSystemDefault()
            val startInstant = start?.let { 
                try { LocalDate.parse(it).atStartOfDayIn(timeZone) } catch (e: Exception) { null } 
            }
            val endInstant = end?.let { 
                try { LocalDate.parse(it).atTime(23, 59, 59).toInstant(timeZone) } catch (e: Exception) { null } 
            }
            val isIncomeLong = isIncome?.let { if (it) 1L else 0L }
            val categoryIds = categoryId?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
            val useCategoryFilter = if (categoryIds.isNotEmpty()) 1L else 0L

            val row = queries.getTransactionCountSummary(
                userId = offlineUserId,
                accountId = accountId,
                isIncome = isIncomeLong,
                useCategoryFilter = useCategoryFilter,
                categoryIds = categoryIds,
                start = startInstant,
                end = endInstant,
                hasTransactionCost = hasTransactionCost
            ).executeAsOne()

            Result.Success(
                TransactionCountSummary(
                    totalIncomeTransactions = row.incomeCount.toInt(),
                    totalExpenseTransactions = row.expenseCount.toInt(),
                    totalTransactions = row.totalCount.toInt(),
                    totalAmount = BigDecimal.fromDouble(row.totalAmount ?: 0.0),
                    totalTransactionCost = BigDecimal.fromDouble(row.totalTransactionCost ?: 0.0)
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
