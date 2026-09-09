package com.fintrack.shared.feature.category.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CategoryRule(
    val id: String,
    val keyword: String,
    val categoryId: String,
    val isExpense: Boolean
)
