package com.fintrack.shared.feature.summary.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal

enum class BudgetBucketType {
    NEEDS,    // Target: ~50%
    WANTS,    // Target: ~30%
    SAVINGS   // Target: ~20%
}

enum class TrimPriority {
    HIGH,
    MEDIUM,
    LOW
}

data class BudgetBucketSummary(
    val bucketType: BudgetBucketType,
    val actualAmount: BigDecimal,
    val actualPercentage: BigDecimal,
    val targetPercentage: BigDecimal,
    val targetAmount: BigDecimal,
    val varianceAmount: BigDecimal
)

data class TrimRecommendation(
    val id: String,
    val categoryName: String,
    val bucketType: BudgetBucketType,
    val currentSpend: BigDecimal,
    val suggestedTrimAmount: BigDecimal,
    val projectedAnnualSavings: BigDecimal,
    val impactMessage: String,
    val iconName: String? = null,
    val priority: TrimPriority = TrimPriority.MEDIUM
)

data class BudgetOptimizerSummary(
    val totalIncome: BigDecimal,
    val totalExpense: BigDecimal,
    val needs: BudgetBucketSummary,
    val wants: BudgetBucketSummary,
    val savings: BudgetBucketSummary,
    val recommendations: List<TrimRecommendation>,
    val healthScore: Int,
    val primaryActionableInsight: String
)
