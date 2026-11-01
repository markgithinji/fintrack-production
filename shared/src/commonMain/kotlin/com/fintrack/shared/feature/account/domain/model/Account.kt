package com.fintrack.shared.feature.account.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.Instant

data class Account(
    val id: String,
    val name: String,
    val balance: BigDecimal? = null,
    val income: BigDecimal? = null,
    val expense: BigDecimal? = null,
    val isDefault: Boolean = false,
    val type: AccountType = AccountType.OTHER,
    val linkedSources: List<String> = emptyList(),
    val createdAt: Instant? = null,
    val lastSyncedAt: Instant? = null,
)

enum class PortfolioGroup {
    LIQUID_CASH,
    INVESTMENTS_MMF,
    SAVINGS_SACCO,
    REAL_ESTATE,
    LIABILITIES_DEBT
}

enum class AccountType {
    OTHER,
    MPESA,
    BANK,
    CASH,
    WALLET,
    SAVINGS
}

val AccountType.isLiability: Boolean
    get() = false

val AccountType.isAsset: Boolean
    get() = true

val AccountType.portfolioGroup: PortfolioGroup
    get() = when (this) {
        AccountType.MPESA, AccountType.BANK, AccountType.CASH, AccountType.WALLET, AccountType.OTHER -> PortfolioGroup.LIQUID_CASH
        AccountType.SAVINGS -> PortfolioGroup.SAVINGS_SACCO
    }
