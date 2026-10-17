package com.fintrack.shared.feature.budget.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SavingsGoal(
    val id: String,
    val userId: String,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDate: String,
    val categoryId: String? = null,
    val accountId: String? = null,
    val iconName: String = "Savings",
    val colorHex: String = "#2E7D32",
    val createdAt: String
) {
    val remainingAmount: Double
        get() = (targetAmount - currentAmount).coerceAtLeast(0.0)

    val progressPercentage: Float
        get() = if (targetAmount > 0) ((currentAmount / targetAmount).toFloat()).coerceIn(0f, 1f) else 0f

    val isCompleted: Boolean
        get() = currentAmount >= targetAmount
}
