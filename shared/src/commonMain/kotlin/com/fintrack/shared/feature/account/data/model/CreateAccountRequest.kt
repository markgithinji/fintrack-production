package com.fintrack.shared.feature.account.data.model

import com.fintrack.shared.feature.core.data.serialization.BigDecimalSerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class CreateAccountRequest(
    val name: String,
    val type: AccountTypeDto,
    @Serializable(with = BigDecimalSerializer::class)
    val balance: BigDecimal? = null,
    val linkedSources: List<String> = emptyList(),
    val isDefault: Boolean = false
)
