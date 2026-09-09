package com.fintrack.shared.feature.summary.data.model

import com.fintrack.shared.feature.summary.domain.model.AvailableMonths
import com.fintrack.shared.feature.summary.domain.model.AvailableWeeks
import com.fintrack.shared.feature.summary.domain.model.AvailableYears
import com.fintrack.shared.feature.summary.domain.model.CategoryComparison
import com.fintrack.shared.feature.summary.domain.model.CategoryComparisonSummary
import com.fintrack.shared.feature.summary.domain.model.CategorySummary
import com.fintrack.shared.feature.summary.domain.model.Correlation
import com.fintrack.shared.feature.summary.domain.model.DaySummary
import com.fintrack.shared.feature.summary.domain.model.DistributionSummary
import com.fintrack.shared.feature.summary.domain.model.Highlight
import com.fintrack.shared.feature.summary.domain.model.Highlights
import com.fintrack.shared.feature.summary.domain.model.OverviewSummary
import com.fintrack.shared.feature.summary.domain.model.ProfileMetrics
import com.fintrack.shared.feature.summary.domain.model.StatisticsSummary
import com.fintrack.shared.feature.summary.domain.model.TransactionCountSummary

// Highlight Summary
fun HighlightDto.toDomain(): Highlight =
    Highlight(
        label = label.ifEmpty { "-" }, 
        value = value.ifEmpty { "-" }, 
        amount = amount,
        volatilityPercentage = volatilityPercentage
    )

fun CorrelationDto.toDomain(): Correlation =
    Correlation(source, target, insight)

fun HighlightsDto.toDomain(): Highlights =
    Highlights(
        highestMonth = highestMonth?.toDomain(),
        highestCategory = highestCategory?.toDomain(),
        highestDay = highestDay?.toDomain(),
        averagePerDay = averagePerDay,
        ytdChangePercentage = ytdChangePercentage,
        projectedTotal = projectedTotal,
        savingsRate = savingsRate,
        essentialSpendRatio = essentialSpendRatio,
        projectedExceedMonth = projectedExceedMonth,
        correlations = correlations?.map { it.toDomain() }
    )

fun HighlightsSummaryDto.toDomain(): StatisticsSummary =
    StatisticsSummary(
        period = period,
        isCurrent = isCurrent,
        income = income,
        expense = expense,
        balance = balance,
        incomeHighlights = incomeHighlights.toDomain(),
        expenseHighlights = expenseHighlights.toDomain(),
        totalTransactionCost = totalTransactionCost
    )

// Distribution Summary
fun CategorySummaryDto.toDomain(): CategorySummary =
    CategorySummary(
        category = category, 
        categoryId = categoryId,
        total = total, 
        percentage = percentage,
        transactionCount = transactionCount,
        averageTransactionCount = averageTransactionCount,
        momentumTrend = momentumTrend,
        topDescriptionInsights = topDescriptionInsights
    )

fun DistributionSummaryDto.toDomain(): DistributionSummary =
    DistributionSummary(
        period = period,
        totalTransactionCost = totalTransactionCost,
        incomeCategories = incomeCategories.map { it.toDomain() },
        expenseCategories = expenseCategories.map { it.toDomain() },
        othersInsightSummary = othersInsightSummary
    )

// Overview Summary
fun OverviewSummaryDto.toDomain() = OverviewSummary(
    period = period,
    isCurrent = isCurrent,
    weeklyOverview = weeklyOverview.map { it.toDomain() },
    monthlyOverview = monthlyOverview.map { it.toDomain() }
)

fun DaySummaryDto.toDomain() = DaySummary(
    date = date,
    income = income,
    expense = expense
)

// Category comparison
fun CategoryComparisonDto.toDomain() = CategoryComparison(
    category = category,
    currentTotal = currentTotal,
    previousTotal = previousTotal,
    changePercentage = changePercentage,
    isIncome = isIncome,
    period = period,
    weeklyChangePercentage = weeklyChangePercentage,
    weeklyCurrentTotal = weeklyCurrentTotal
)

fun CategoryComparisonSummaryDto.toDomain() = CategoryComparisonSummary(
    period = period,
    isCurrent = isCurrent,
    data = data.map { it.toDomain() }
)

// Available Weeks
fun AvailableWeeksDto.toDomain() = AvailableWeeks(weeks = weeks)

// Available Months
fun AvailableMonthsDto.toDomain() = AvailableMonths(months = this.months)

// Available Years
fun AvailableYearsDto.toDomain() = AvailableYears(years)

// Transaction Count Summary
fun TransactionCountSummaryDto.toDomain() = TransactionCountSummary(
    totalIncomeTransactions = totalIncomeTransactions,
    totalExpenseTransactions = totalExpenseTransactions,
    totalTransactions = totalTransactions,
    totalAmount = totalAmount,
    totalTransactionCost = totalTransactionCost
)

fun ProfileMetricsDto.toDomain() = ProfileMetrics(
    name = name,
    email = email,
    netWorth = netWorth,
    savingsRate = savingsRate,
    essentialSpendRatio = essentialSpendRatio
)
