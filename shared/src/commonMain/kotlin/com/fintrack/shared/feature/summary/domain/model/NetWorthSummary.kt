package com.fintrack.shared.feature.summary.domain.model

import com.fintrack.shared.feature.account.domain.model.PortfolioGroup
import com.ionspin.kotlin.bignum.decimal.BigDecimal

data class AssetLiabilityItem(
    val id: String,
    val name: String,
    val amount: BigDecimal,
    val isAsset: Boolean
)

data class PortfolioAllocationGroup(
    val group: PortfolioGroup,
    val displayName: String,
    val totalAmount: BigDecimal,
    val percentageOfAssets: BigDecimal,
    val hexColor: String
)

data class NetWorthMilestone(
    val targetAmount: BigDecimal,
    val currentProgressPercentage: BigDecimal,
    val remainingAmount: BigDecimal,
    val estimatedMonthsToTarget: Int? = null
)

data class NetWorthSummary(
    val totalAssets: BigDecimal,
    val totalLiabilities: BigDecimal,
    val netWorth: BigDecimal,
    val previousNetWorth: BigDecimal? = null,
    val growthRatePercentage: BigDecimal? = null,
    val ytdGrowthRatePercentage: BigDecimal? = null,
    val debtToAssetRatioPercentage: BigDecimal = BigDecimal.ZERO,
    val financialHealthStatus: String = "Healthy",
    val assetBreakdown: List<AssetLiabilityItem> = emptyList(),
    val liabilityBreakdown: List<AssetLiabilityItem> = emptyList(),
    val portfolioAllocations: List<PortfolioAllocationGroup> = emptyList(),
    val milestone: NetWorthMilestone? = null,
    val fireScore: FireScore? = null,
    val historicalTrendPoints: List<Double> = emptyList()
)
