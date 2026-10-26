package com.fintrack.shared.feature.planning.domain.model

import kotlinx.serialization.Serializable

enum class AssetType {
    GENERAL_ASSET,
    MMF_YIELD,
    SACCO_SHARES,
    REAL_ESTATE,
    STOCKS_CRYPTO,
    CASH_SAVINGS
}

@Serializable
data class Asset(
    val id: String,
    val userId: String,
    val name: String,
    val assetType: AssetType = AssetType.GENERAL_ASSET,
    val currentBalance: Double = 0.0,
    val annualYieldPercentage: Double? = null,
    val categoryId: String? = null,
    val accountId: String? = null,
    val colorHex: String = "#2979FF",
    val createdAt: String
) {
    /**
     * Kenyan Statutory Tax Rates:
     * - MMF / Cash Savings: 15% Withholding Tax (WHT)
     * - Sacco Dividends / NSE Stocks: 5% Withholding Tax (WHT)
     * - Real Estate: 7.5% Monthly Rental Income (MRI) Tax
     * - Infrastructure Bonds / General Asset: 0% Tax-Free
     */
    val statutoryTaxRatePercentage: Double
        get() = when (assetType) {
            AssetType.MMF_YIELD, AssetType.CASH_SAVINGS -> 15.0
            AssetType.SACCO_SHARES, AssetType.STOCKS_CRYPTO -> 5.0
            AssetType.REAL_ESTATE -> 7.5
            AssetType.GENERAL_ASSET -> 0.0
        }

    /**
     * Gross annual passive yield generated before statutory tax.
     */
    val grossAnnualPassiveYield: Double
        get() = if (annualYieldPercentage != null && annualYieldPercentage > 0) {
            currentBalance * (annualYieldPercentage / 100.0)
        } else 0.0

    val annualTaxDeduction: Double
        get() = grossAnnualPassiveYield * (statutoryTaxRatePercentage / 100.0)

    /**
     * Net annual passive yield in hand after statutory tax.
     */
    val annualPassiveYield: Double
        get() = grossAnnualPassiveYield - annualTaxDeduction

    val monthlyPassiveYield: Double
        get() = annualPassiveYield / 12.0

    val netYieldPercentage: Double
        get() = if (currentBalance > 0) (annualPassiveYield / currentBalance) * 100.0 else (annualYieldPercentage ?: 0.0)

    /**
     * Indicates whether the asset can be liquidated to cash within 24-48 hours.
     */
    val isLiquid: Boolean
        get() = assetType == AssetType.MMF_YIELD ||
                assetType == AssetType.CASH_SAVINGS ||
                assetType == AssetType.STOCKS_CRYPTO ||
                (annualYieldPercentage != null && annualYieldPercentage > 0)

    /**
     * Inflation benchmark comparison against net real return (Kenya CPI average ~6.5% p.a.).
     */
    val netRealReturnPercentage: Double
        get() = netYieldPercentage - 6.5

    val isBeatingInflation: Boolean
        get() = netRealReturnPercentage > 0.0

    /**
     * Compound value forecast after 3 years at net annual yield rate.
     */
    val compoundValue3Years: Double
        get() {
            val rate = netYieldPercentage / 100.0
            if (rate <= 0.0) return currentBalance
            var value = currentBalance
            repeat(3) {
                value *= (1.0 + rate)
            }
            return value
        }
}
