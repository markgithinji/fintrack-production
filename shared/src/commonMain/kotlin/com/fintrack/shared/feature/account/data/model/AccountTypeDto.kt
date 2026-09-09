package com.fintrack.shared.feature.account.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AccountTypeDto {
    @SerialName("other")
    OTHER,
    @SerialName("mpesa")
    MPESA,
    @SerialName("bank")
    BANK,
    @SerialName("cash")
    CASH,
    @SerialName("wallet")
    WALLET,
    @SerialName("savings")
    SAVINGS
}
