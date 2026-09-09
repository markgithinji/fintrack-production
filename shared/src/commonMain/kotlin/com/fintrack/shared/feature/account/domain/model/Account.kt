package com.fintrack.shared.feature.account.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlin.time.Instant

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

enum class AccountType {
    OTHER,
    MPESA,
    BANK,
    CASH,
    WALLET,
    SAVINGS
}
