package com.fintrack.shared.feature.summary.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal

data class AssetLiabilityItem(
    val id: String,
    val name: String,
    val amount: BigDecimal,
    val isAsset: Boolean
)

data class NetWorthSummary(
    val totalAssets: BigDecimal,
    val totalLiabilities: BigDecimal,
    val netWorth: BigDecimal,
    val previousNetWorth: BigDecimal? = null,
    val growthRatePercentage: BigDecimal? = null,
    val debtToAssetRatioPercentage: BigDecimal = BigDecimal.ZERO,
    val financialHealthStatus: String = "Healthy",
    val assetBreakdown: List<AssetLiabilityItem> = emptyList(),
    val liabilityBreakdown: List<AssetLiabilityItem> = emptyList()
)
