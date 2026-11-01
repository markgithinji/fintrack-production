package com.fintrack.shared.feature.planning.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Debt(
    val id: String,
    val userId: String = "local_user",
    val name: String,
    val balance: Double,
    val initialBalance: Double,
    val interestRate: Double,
    val minimumPayment: Double,
    val categoryId: String? = null,
    val accountId: String? = null,
    val colorHex: String = "#E53935",
    val createdAt: String
) {
    val progressPercentage: Float
        get() = if (initialBalance > 0) {
            ((initialBalance - balance) / initialBalance).toFloat().coerceIn(0f, 1f)
        } else 1f

    val isPaidOff: Boolean
        get() = balance <= 0.0
}
