package com.fintrack.shared.feature.account.data.model

import com.fintrack.shared.feature.core.data.serialization.BigDecimalSerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class AccountDto(
    val id: String? = null,
    val name: String,
    @Serializable(with = BigDecimalSerializer::class)
    val balance: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val income: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val expense: BigDecimal? = null,
    val isDefault: Boolean? = false,
    val type: AccountTypeDto? = AccountTypeDto.OTHER,
    val linkedSources: List<String>? = emptyList(),
    val createdAt: Instant? = null,
    val lastSyncedAt: Instant? = null,
)
